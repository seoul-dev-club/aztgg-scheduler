package com.aztgg.scheduler.techarticle.presentation;

import com.aztgg.scheduler.global.logging.AppLogger;
import com.aztgg.scheduler.techarticle.application.collectorservice.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TechArticleScheduler {

    private final GeekNewsCollectorService geekNewsCollectorService;
    private final KoreanFeCollectorService koreanFeCollectorService;
    private final KakaoTechCollectorService kakaoTechCollectorService;
    private final DaangnTechCollectorService daangnTechCollectorService;
    private final TossTechCollectorService tossTechCollectorService;

    @Value("${aztgg.donotnotice:false}")
    private boolean doNotCollect;

    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000)
    public void collectTechArticles() {
        if (doNotCollect) {
            AppLogger.debugLog("aztgg.donotnotice = true");
            return;
        }
        geekNewsCollectorService.collectWithRetry();
        koreanFeCollectorService.collectWithRetry();
        kakaoTechCollectorService.collectWithRetry();
        daangnTechCollectorService.collectWithRetry();
        tossTechCollectorService.collectWithRetry();
    }
}
