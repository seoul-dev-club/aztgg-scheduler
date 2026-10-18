package com.aztgg.scheduler.techarticle.application.collectorservice;

import com.aztgg.scheduler.global.asset.TechArticleSourceType;
import com.aztgg.scheduler.techarticle.application.TechArticleRssCollectorService;
import com.aztgg.scheduler.techarticle.domain.TechArticleDomainService;
import org.springframework.stereotype.Service;

@Service
public class DaangnTechCollectorService extends TechArticleRssCollectorService {

    public DaangnTechCollectorService(TechArticleDomainService techArticleDomainService) {
        super(techArticleDomainService, TechArticleSourceType.DAANGN);
    }
}
