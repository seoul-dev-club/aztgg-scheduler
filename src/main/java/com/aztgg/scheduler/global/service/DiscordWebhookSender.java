package com.aztgg.scheduler.global.service;

import com.aztgg.scheduler.global.asset.WebhookType;
import com.aztgg.scheduler.global.logging.AppLogger;
import com.aztgg.scheduler.global.util.ProfileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DiscordWebhookSender {

    private final RestClient restClient =RestClient.create();
    private final ProfileUtils profileUtils;

    @Value("${discord.webhook.notice-url}")
    private String noticeWebhookUrl;
    private String hotIssueWebhookUrl;
    @Value("${discord.webhook.tech-article-url}")
    private String techArticleWebhookUrl;

    public DiscordWebhookSender(ProfileUtils profileUtils) {
        this.profileUtils = profileUtils;
    }

    public void sendDiscordMessage(WebhookType type, String title, String description, String colorHex) {
        String webhookUrl = switch(type){
            case NOTICE -> noticeWebhookUrl;
            case HOTISSUE -> hotIssueWebhookUrl;
            case TECH_ARTICLE -> techArticleWebhookUrl;
        };

        Map<String, Object> embed = Map.of(
                "title", title,
                "description", description,
                "color", Integer.decode(colorHex)
        );

        Map<String, Object> payload = Map.of("embeds", List.of(embed));

        if (!profileUtils.isLocal()) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            } catch(Exception e){
                AppLogger.errorLog("디스코드 Embed 전송 실패 ", e);
            }
        }
    }

}

