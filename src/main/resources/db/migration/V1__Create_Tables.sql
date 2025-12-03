create table tbl_crawl_site
(
    id             int auto_increment primary key,
    site_name      varchar(100)            not null comment '사이트 이름',
    seed_url       varchar(2048)           not null comment '크롤링 시작 URL',
    domain         varchar(255)            not null comment '사이트 도메인',
    crawl_policy   text comment 'robots.txt',
    crawl_delay_ms int       default 1000 comment '크롤링 지연 시간',
    user_agent     TEXT                    not null,
    timeout        bigint    default 60000 not null,
    created_at     timestamp default current_timestamp,
    unique key uk_domain (domain)
);

insert into tbl_crawl_site (site_name, seed_url, domain, crawl_delay_ms, user_agent)
values ('네이버 뉴스', 'https://news.naver.com', 'news.naver.com', 10000,
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36');

create table tbl_crawl_source
(
    id                 bigint auto_increment primary key,
    site_id            int           not null,
    page_rule_id       bigint        not null,
    source_url         varchar(2048) not null,
    description        varchar(255) comment '설명 (예: 네이버 경제 메인)',
    crawl_interval_sec int     default 3600,
    is_active          boolean default true,
    last_generated_at  timestamp     null
);

insert into tbl_crawl_source (site_id, page_rule_id, source_url, description, crawl_interval_sec)
values (1, 1, 'https://news.naver.com/section/100', '네이버 정치 메인', 60),
       (1, 1, 'https://news.naver.com/section/101', '네이버 경제 메인', 60),
       (1, 1, 'https://news.naver.com/section/105', '네이버 IT/과학 메인', 60);


create table tbl_crawl_target
(
    id                bigint auto_increment primary key,
    parent_target_id  bigint                        null comment '부모 작업 아이디 (계층 추적용)',
    source_id         bigint                        null comment '이 작업을 생성한 부모 작업 ID',
    page_rule_id      bigint                        null comment '사용할 규칙 템플릿 ID',
    site_id           int                           not null comment 'tbl_crawl_site 아이디',
    status            varchar(20) default 'PENDING' not null comment 'PENDING, IN_PROGRESS, DONE, FAILED',
    page_type         varchar(10) default 'UNKNOWN' not null comment '작업 타입(LIST, ARTICLE, UNKNOWN)',
    target_name       varchar(100) comment '크롤링 대상 이름(예: 네이버 경제)',
    target_url        varchar(2048) comment '크롤링 시작 URL (섹션 URL)',
    target_url_hash   varchar(64) unique comment '유니크 전용 해시',
    is_active         boolean     default true,
    depth             int         default 1         not null comment '탐색 깊이 (1 = 시드 URL)',
    retry_count       int         default 0,
    next_try_at       timestamp                     null comment '다음 재시도 예정 시간',
    last_processed_at timestamp                     null comment '마지막 크롤링 완료 시간',
    priority          int         default 0,
    created_at        timestamp   default current_timestamp,
    updated_at        timestamp   default current_timestamp on update current_timestamp,
    index idx_crawl_target (site_id, status, priority, next_try_at)
);

create table tbl_crawl_page_rule
(
    id                bigint auto_increment primary key,
    site_id           int          not null,
    rule_name         varchar(100) not null comment '규칙 이름(예: 네이버 경제 본문)',
    page_type         varchar(10)  not null comment '페이지 타입 (LIST, CONTENT)',
    url_pattern_regex varchar(255) null comment '페이지 식별용 정규식',
    link_search_scope varchar(255) null comment '링크 탐색 범위 CSS 선택자 (LIST용)',
    match_priority    int       default 10,
    created_at        timestamp default current_timestamp,
    unique key uk_template_name (rule_name)
);

insert into tbl_crawl_page_rule (site_id, rule_name, page_type, url_pattern_regex, link_search_scope,
                                 match_priority)
values (1, '네이버 뉴스 목록형', 'LIST', '.*news\\.naver\\.com\\/section\\/\\d+', 'div#newsct', 100),
       (1, '네이버 뉴스 컨텐츠형', 'CONTENT', '.*news\\.naver\\.com\\/mnews\\/article\\/\\d{3}\\/\\d+', null, 100);

create table tbl_crawl_extraction_rule
(
    id                bigint auto_increment primary key,
    page_rule_id      bigint        not null,
    is_required       boolean     default false comment '필수 데이터 여부',
    json_key          varchar(100)  not null comment '추출 대상 키 (예: list_title)',
    css_selector      varchar(1024) not null comment 'CSS 또는 XPath 선택자',
    extract_attribute text,
    selector_type     VARCHAR(5)  default 'CSS' comment '선택자 유형 (CSS, XPATH)',
    status            varchar(10) default 'ACTIVE' comment 'ACTIVE, FAILED, IN_REVIEW, ARCHIVED',
    failure_count     int         default 0 comment '연속 실패 횟수',
    last_used_at      timestamp     null comment '마지막 크롤링 시도 시간',
    last_success_at   timestamp     null comment '마지막 크롤링 성공 시간',
    updated_by        varchar(10) default 'USER' comment '선택자 수정 주체(USER, LLM)',
    updated_at        timestamp   default current_timestamp on update current_timestamp,
    unique key uk_target_key_active (page_rule_id, json_key, status)
);

insert into tbl_crawl_extraction_rule (page_rule_id, json_key, is_required, css_selector, extract_attribute,
                                       selector_type, status)
values (2, 'title', true, 'h2#title_area', null, 'CSS', 'ACTIVE'),
       (2, 'summary', false, 'strong.media_end_summary', null, 'CSS', 'ACTIVE'),
       (2, 'image', false, 'img#img1', 'data-src', 'CSS', 'ACTIVE'),
       (2, 'image_desc', false, 'em.img_desc', null, 'CSS', 'ACTIVE'),
       (2, 'content', true, 'article#dic_area', null, 'CSS', 'ACTIVE')
;

create table tbl_crawl_priority_rule
(
    id                   bigint auto_increment primary key,
    page_rule_id         bigint       not null comment 'crawl_structure_template.id',
    condition_type       varchar(255) not null comment '조건 타입 (SELECTOR, TEXT)',
    condition_expression varchar(20)  not null comment '조건 식 (CSS 선택자 또는 검색어)',
    priority_bonus       int          not null default 0,
    is_active            boolean               default true
);

insert into tbl_crawl_priority_rule (page_rule_id, condition_type, condition_expression, priority_bonus)
values (1, 'TEXT_CONTAINS', '긴급', 100),
       (1, 'TEXT_CONTAINS', '속보', 100),
       (1, 'SELECTOR_MATCH', '.sh_text_headline', 30);


create table tbl_crawled_data
(
    id         bigint auto_increment primary key,
    target_id  bigint        not null comment 'tbl_crawl_target 아이디',
    data       JSON          not null comment '수집 데이터(JSON)',
    source_url varchar(2048) null comment '원본 URL',
    crawled_at timestamp default current_timestamp
);

create table tb_crawl_extraction_rule_change_history
(
    id            bigint auto_increment primary key,
    rule_id       bigint not null,
    target_id     bigint not null,
    old_selector  varchar(1024),
    new_selector  varchar(1024),
    change_reason varchar(255),
    change_at     timestamp default current_timestamp,
    is_verified   boolean   default false
);

create table shedlock
(
    name       varchar(64)  not null primary key,
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null,
    locked_by  varchar(255) not null
);