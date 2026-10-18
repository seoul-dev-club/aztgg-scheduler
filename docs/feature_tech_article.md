# TechArticle 기능 설계 문서

## 개요

테크 아티클(기술 블로그 글)을 수집·저장하고 사용자에게 제공하는 기능이다.
인프런, 우아한테크코스 기술 블로그 등 다양한 출처의 아티클을 스케줄러가 주기적으로 수집하고, 신규 아티클 발견 시 디스코드로 알림을 발송한다.

---

## 1. 도메인 모델

### TechArticle

| 필드 | 타입 | 필수 여부 | 설명 |
|------|------|-----------|------|
| `techArticleId` | `Long` | 필수 (PK) | 고유 식별자 |
| `title` | `String` | 필수 | 테크 아티클 제목 |
| `description` | `String` | 필수 | 아티클 콘텐츠 앞부분 truncate 값 |
| `link` | `String` | 필수 | 아티클 원문 링크 |
| `source` | `String` | 필수 | 아티클 출처 (`TechArticleSourceType` Enum name 저장) |
| `writer` | `String` | 선택 | 아티클 작성자 |
| `createdAt` | `LocalDateTime` | 필수 | 아티클 작성일자 |
| `scrapedAt` | `LocalDateTime` | 필수 | 스크래핑 수집 일자 (자동 설정) |
| `hash` | `String` | 필수 | 중복 수집 방지용 해시 (link 기반) |

### 제약 조건

- `hash` 는 `link` 를 기반으로 생성하며 유니크 제약을 두어 동일 아티클 중복 저장을 방지한다.
- `writer` 는 출처에 따라 제공되지 않을 수 있으므로 선택값으로 처리한다.
- `createdAt` 은 스크래핑 시점이 아닌 아티클 원문의 작성일자다.
- `source` 는 DB에 Enum name(`String`)으로 저장한다 (`ScrapGroupCodeType` 동일 방식).

### ERD (tech_article 테이블)

```sql
CREATE TABLE aztggdb.tech_article
(
  techArticleId BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(500)  NOT NULL,
  description   TEXT          NOT NULL,
  link          VARCHAR(1000) NOT NULL,
  source        VARCHAR(100)  NOT NULL,
  writer        VARCHAR(100) NULL,
  createdAt     DATETIME      NOT NULL,
  scrapedAt     DATETIME      NOT NULL,
  hash          VARCHAR(64)   NOT NULL,
  UNIQUE KEY uq_hash (hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

---

## 2. TechArticleSourceType Enum

`global/asset` 패키지에 위치한다. 출처별 RSS URL과 한글명을 함께 관리한다.

```java
// global/asset/TechArticleSourceType.java
@Getter
@AllArgsConstructor
public enum TechArticleSourceType {

    GEEK_NEWS("Geek News", "https://news.hada.io/rss/news"),
    KOREAN_FE("KOREAN FE Article", "https://kofearticle.substack.com/feed"),
    KAKAO_TECH("카카오 기술 블로그", "https://tech.kakao.com/feed"),
    WOOWAHAN("우아한 형제들 기술 블로그", "https://techblog.woowahan.com/feed"),
    DAANGN("당근 기술 블로그", "https://medium.com/feed/daangn"),
    TOSS("토스 기술 블로그", "https://toss.tech/rss.xml"),
    ;

    private final String korean;
    private final String rssUrl;
}
```

- DB에는 `Enum.name()` 값(`GEEK_NEWS`, `KAKAO_TECH` 등)을 저장한다.
- 출처 추가 시 Enum에 항목만 추가하면 된다.

---

## 3. 디스코드 알림

신규 아티클 수집 시 기존 `DiscordWebhookSender`를 그대로 활용한다.

- **WebhookType**: 기존 `NOTICE` 재사용 → `WebhookType`, `DiscordWebhookSender`, `application.yml` 변경 없음
- **메시지 구성**
  - `title`: 아티클 제목
  - `description`: 아티클 콘텐츠 앞부분 200자 truncate + 링크 + 출처 + 작성자(있을 경우)
  - `colorHex`: 출처별 고유 색상 (`TechArticleSourceType`에 colorHex 필드 추가 고려)

---

## 4. 패키지 구조

```
global/asset/
└── TechArticleSourceType.java              # 6개 출처 Enum (korean, rssUrl 필드)

techarticle/
├── domain/
│   ├── TechArticle.java                    # Spring Data JDBC 엔티티
│   ├── TechArticleRepository.java          # hash 기반 중복 조회
│   └── TechArticleDomainService.java       # 저장/조회 서비스
├── application/
│   ├── TechArticleRssCollectorService.java # RSS 수집 추상 클래스 (템플릿 메서드 패턴)
│   └── collectorservice/
│       ├── GeekNewsCollectorService.java
│       ├── KoreanFeCollectorService.java
│       ├── KakaoTechCollectorService.java
│       ├── WoowahanTechCollectorService.java
│       ├── DaangnTechCollectorService.java
│       └── TossTechCollectorService.java
└── presentation/
    └── TechArticleScheduler.java           # @Scheduled(fixedDelay = 6 * 60 * 60 * 1000)
```

---

## 5. 미결 사항 (설계 진행 중)

- [x] 수집 대상 출처 목록 확정 — 6개 출처, 모두 RSS 방식
- [x] `WebhookType` — 기존 `NOTICE` 재사용
- [x] `TechArticleSourceType` 위치 — `global/asset`
- [x] `description` truncate 글자 수 — 200자
- [x] 스케줄러 실행 주기 — 6시간 (`fixedDelay = 6h`)
