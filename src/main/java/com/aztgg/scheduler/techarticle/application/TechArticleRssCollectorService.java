package com.aztgg.scheduler.techarticle.application;

import com.aztgg.scheduler.global.asset.TechArticleSourceType;
import com.aztgg.scheduler.global.asset.WebhookType;
import com.aztgg.scheduler.global.logging.AppLogger;
import com.aztgg.scheduler.global.service.DiscordWebhookSender;
import com.aztgg.scheduler.global.util.HashUtils;
import com.aztgg.scheduler.techarticle.domain.TechArticle;
import com.aztgg.scheduler.techarticle.domain.TechArticleDomainService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class TechArticleRssCollectorService {

    private static final int DESCRIPTION_MAX_LENGTH = 200;
    private static final DateTimeFormatter RSS_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final TechArticleDomainService techArticleDomainService;
    private final TechArticleSourceType sourceType;
    @Autowired
    private DiscordWebhookSender discordWebhookSender;

    protected TechArticleRssCollectorService(TechArticleDomainService techArticleDomainService,
                                             TechArticleSourceType sourceType) {
        this.techArticleDomainService = techArticleDomainService;
        this.sourceType = sourceType;
    }

    public void collect() {
        AppLogger.infoLog("{} 테크 아티클 수집 시작", sourceType.name());
        long startTime = System.currentTimeMillis();

        Document doc;
        try {
            doc = fetchDocument(sourceType.getRssUrl());
        } catch (Exception e) {
            throw new RuntimeException(sourceType.name() + " RSS 요청 실패", e);
        }

        Elements items = doc.select("item");
        boolean isAtom = items.isEmpty();
        if (isAtom) {
            items = doc.select("entry");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException(sourceType.name() + " RSS item이 비어있음");
        }

        Set<String> existingHashes = techArticleDomainService.findHashSetBySource(sourceType);

        List<TechArticle> newArticles = new ArrayList<>();
        for (Element item : items) {
            String link = isAtom ? extractAtomLink(item) : extractLink(item);
            String hash = HashUtils.encrypt(link);

            if (existingHashes.contains(hash)) {
                continue;
            }

            String rawDescription = isAtom
                    ? Jsoup.parse(text(item, "content")).text()
                    : Jsoup.parse(text(item, "description")).text();
            String description = rawDescription.length() > DESCRIPTION_MAX_LENGTH
                    ? rawDescription.substring(0, DESCRIPTION_MAX_LENGTH)
                    : rawDescription;

            LocalDateTime createdAt = isAtom
                    ? parseDateIso(text(item, "published"))
                    : parseDate(text(item, "pubDate"));

            String writer = isAtom ? extractAtomWriter(item) : extractWriter(item);

            newArticles.add(TechArticle.builder()
                    .title(text(item, "title"))
                    .description(description)
                    .link(link)
                    .source(sourceType.name())
                    .writer(writer)
                    .createdAt(createdAt)
                    .hash(hash)
                    .build());
        }

        if (!newArticles.isEmpty()) {
            techArticleDomainService.saveAll(newArticles);
            sendDiscordNotification(newArticles);
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        AppLogger.infoLog("{} 테크 아티클 수집 완료: 신규 {}건, {}초 소요", sourceType.name(), newArticles.size(), duration);
    }

    protected Map<String, String> additionalHeaders() {
        return Map.of();
    }

    private Document fetchDocument(String url) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .GET();
        additionalHeaders().forEach(requestBuilder::header);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }
        return Jsoup.parse(response.body(), "", Parser.xmlParser());
    }

    protected String extractLink(Element item) {
        String link = text(item, "link");
        if (link.isBlank()) {
            link = item.select("guid").text();
        }
        return link;
    }

    private String extractAtomLink(Element item) {
        Element linkEl = item.selectFirst("link[rel=alternate]");
        if (linkEl == null) {
            linkEl = item.selectFirst("link");
        }
        return linkEl != null ? linkEl.attr("href") : text(item, "id");
    }

    protected String extractWriter(Element item) {
        String author = item.select("dc|creator").text();
        if (author.isBlank()) {
            author = text(item, "author");
        }
        return author.isBlank() ? null : author;
    }

    private String extractAtomWriter(Element item) {
        String name = item.select("author > name").text();
        return name.isBlank() ? null : name;
    }

    private void sendDiscordNotification(List<TechArticle> articles) {
        StringBuilder sb = new StringBuilder();
        for (TechArticle article : articles) {
            sb.append(String.format("**[%s](%s)**", article.getTitle(), article.getLink()));
            if (article.getWriter() != null) {
                sb.append(String.format(" — %s", article.getWriter()));
            }
            sb.append("\n");
        }
        discordWebhookSender.sendDiscordMessage(
                WebhookType.NOTICE,
                String.format("[%s] 새 테크 아티클 %d건", sourceType.getKorean(), articles.size()),
                sb.toString(),
                "#5865F2"
        );
    }

    private String text(Element item, String tag) {
        Element el = item.selectFirst(tag);
        return el != null ? el.text() : "";
    }

    private LocalDateTime parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return ZonedDateTime.parse(pubDate, RSS_DATE_FORMATTER).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseDateIso(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return ZonedDateTime.parse(dateStr).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    @Retryable(value = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public void collectWithRetry() {
        collect();
    }

    @Recover
    public void recover(Exception e) {
        discordWebhookSender.sendDiscordMessage(
                WebhookType.NOTICE,
                String.format("[%s] 테크 아티클 수집 최종 재시도 실패", sourceType.getKorean()),
                e.getMessage(),
                "#FF0000"
        );
        AppLogger.errorLog(sourceType.name() + " 테크 아티클 수집 최종 재시도 실패", e);
    }
}
