package com.aztgg.scheduler.techarticle.domain;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface TechArticleRepository extends CrudRepository<TechArticle, Long> {

    @Query("SELECT hash FROM tech_article WHERE source = :source")
    Set<String> findHashSetBySource(@Param("source") String source);
}
