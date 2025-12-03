웹 크롤러
=======
Kotlin & Spring Boot 기반의 웹 크롤러
---------------------------------
**목록 탐색**과 **본문 추출**로 구성하고, Redis 기반의 요청제한과 우선순위 기반 스케줄링을 통해 데이터를 효율적으로 수집

###### 추후, LLM을 연동하고 HTML 구조 변경 시 스스로 선택자를 복구하는 **자가 치유** 기능을 추가할 예정

데이터베이스 설계
------------
#### tbl_crawl_site
    도메인, 요청 제한 시간, 타임아웃 등 설정정보
#### tbl_crawl_source
    시드(Seed), 크롤링의 시작점 URL
#### tbl_crawl_page_rule 
    페이지 규칙, URL 정규식 패턴을 통해 페이지 타입 판별 및 탐색 범위 설정
#### tbl_crawl_extraction_rule 
    추출 규칙, CONTENT 페이지에서 데이터를 추출할 CSS 선택자와 JSON Key 매핑
#### tbl_crawl_priority_rule
    우선 순위 규칙, 특정 CSS나 키워드('속보')발견 시 가산점 부여 규칙
#### tbl_crawl_target
    작업 큐, 실행 대기중인 URL 목록 (우선순위, 상태 포함)

동작 흐름
-------
1. 시드생성: CrawlSeedTaskGenerator가 tbl_crawl_source를 확인하여 주기적으로 시드 URL을 tbl_crawl_target에 주입
2. 스케쥴링: CrawlScheduler가 Redis Rate Limiter를 통해 요청 가능한 사이트 ID를 확보
3. 작업할당: CrawlDispatcher가 해당 사이트(도메인)의 가장 우선순위가 높은 작업을 가져와 LinkDiscoverer 또는 ContentExtractor로 배정
4. 탐색(LIST): LinkDiscoverer가 페이지를 파싱, PageRule 에 따라 링크를 찾고 우선순위를 분석(PriorityAnalyzer)하여 새 작업을 큐에 등록
5. 추출(CONTENT): ContentExtractor가 ExtractionRule에 따라 본문 데이터를 추출하여 DB에 저장

