package com.aztgg.scheduler.techarticle.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("tech_article")
public class TechArticle {

    @Id
    @Column("techArticleId")
    private Long techArticleId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("link")
    private String link;

    @Column("source")
    private String source;

    @Column("writer")
    private String writer;

    @Column("createdAt")
    private LocalDateTime createdAt;

    @Column("scrapedAt")
    private LocalDateTime scrapedAt;

    @Column("hash")
    private String hash;

    @Builder
    public TechArticle(Long techArticleId,
                       String title,
                       String description,
                       String link,
                       String source,
                       String writer,
                       LocalDateTime createdAt,
                       LocalDateTime scrapedAt,
                       String hash) {
        this.techArticleId = techArticleId;
        this.title = title;
        this.description = description;
        this.link = link;
        this.source = source;
        this.writer = writer;
        this.createdAt = createdAt;
        this.scrapedAt = scrapedAt != null ? scrapedAt : LocalDateTime.now();
        this.hash = hash;
    }
}
