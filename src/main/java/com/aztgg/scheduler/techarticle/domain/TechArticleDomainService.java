package com.aztgg.scheduler.techarticle.domain;

import com.aztgg.scheduler.global.asset.TechArticleSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TechArticleDomainService {

    private final TechArticleRepository techArticleRepository;

    public Set<String> findHashSetBySource(TechArticleSourceType source) {
        return techArticleRepository.findHashSetBySource(source.name());
    }

    public List<TechArticle> saveAll(List<TechArticle> articles) {
        return (List<TechArticle>) techArticleRepository.saveAll(articles);
    }
}
