package com.aztgg.scheduler.techarticle.application.collectorservice;

import com.aztgg.scheduler.global.asset.TechArticleSourceType;
import com.aztgg.scheduler.techarticle.application.TechArticleRssCollectorService;
import com.aztgg.scheduler.techarticle.domain.TechArticleDomainService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WoowahanTechCollectorService extends TechArticleRssCollectorService {

    public WoowahanTechCollectorService(TechArticleDomainService techArticleDomainService) {
        super(techArticleDomainService, TechArticleSourceType.WOOWAHAN);
    }

    @Override
    protected Map<String, String> additionalHeaders() {
        return Map.of("Referer", "https://techblog.woowahan.com/");
    }
}
