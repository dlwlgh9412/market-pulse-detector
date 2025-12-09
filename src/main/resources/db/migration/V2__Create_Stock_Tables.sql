create table tbl_stock_info
(
    code        varchar(20)  not null primary key comment '종목코드 (예: 000000)',
    stock_name  varchar(100) not null comment '종목명',
    market_type varchar(20)  not null comment 'KOSPI, KOSDQ',
    country     varchar(2)   not null comment 'KR, US, JP',
    is_active   boolean   default true,
    created_at  timestamp default current_timestamp
);

alter table tbl_stock_info
    add column listed_at   date comment '상장일',
    add column delisted_at date comment '상장폐지일',
    add column updated_at  timestamp default current_timestamp on update current_timestamp;

create table tbl_stock_history
(
    id              bigint auto_increment primary key,
    stock_code      varchar(20) not null,
    old_stock_name  varchar(100),
    old_market_type varchar(20),
    -- 변경 사유 (RENAMED: 사명변경, MARKET_MOVE: 코스닥 -> 코스피, DELISTED: 상폐, NEW:신규상장
    change_type     varchar(20) not null,
    change_date     date        not null comment '변경 발생일',
    created_at      timestamp default current_timestamp,
    index idx_stock_history (stock_code, change_date)
);

create table tbl_stock_alias
(
    id         bigint auto_increment primary key,
    stock_code varchar(20)  not null,
    alias      varchar(100) not null comment '매핑될 키워드(예: 삼전, 삼성전자, NVDA, 엔비디아)',
    index idx_stock_alias (alias)
#     foreign key (stock_code) references tbl_stock_info (code)
);

-- [초기 데이터 예시]
-- 1. 한국 (삼성전자)
insert into tbl_stock_info (code, stock_name, market_type, country)
values ('005930', '삼성전자', 'KOSPI', 'KR');
insert into tbl_stock_alias (stock_code, alias)
values ('005930', '삼성전자'),
       ('005930', '삼전'),
       ('005930', '005930');

-- 2. 한국 (SK하이닉스)
insert into tbl_stock_info (code, stock_name, market_type, country)
values ('000660', 'SK하이닉스', 'KOSPI', 'KR');
insert into tbl_stock_alias (stock_code, alias)
values ('000660', 'SK하이닉스'),
       ('000660', '하이닉스'),
       ('000660', '000660');

-- 3. 미국 (테슬라)
insert into tbl_stock_info (code, stock_name, market_type, country)
values ('TSLA', 'Tesla, Inc.', 'NASDAQ', 'US');
insert into tbl_stock_alias (stock_code, alias)
values ('TSLA', '테슬라'),
       ('TSLA', 'Tesla'),
       ('TSLA', 'TSLA');

-- 4. 미국 (엔비디아)
insert into tbl_stock_info (code, stock_name, market_type, country)
values ('NVDA', 'NVIDIA Corp', 'NASDAQ', 'US');
insert into tbl_stock_alias (stock_code, alias)
values ('NVDA', '엔비디아'),
       ('NVDA', 'NVIDIA'),
       ('NVDA', 'NVDA');

-- 신한지주 (055550)
INSERT INTO tbl_stock_info (code, stock_name, market_type, country)
VALUES ('055550', '신한지주', 'KOSPI', 'KR');

INSERT INTO tbl_stock_alias (stock_code, alias)
VALUES ('055550', '신한지주'),
       ('055550', 'Shinhan');

-- KB금융 (105560) - V4 예시에 포함되어 있었다면 이것도 에러 날 예정
INSERT INTO tbl_stock_info (code, stock_name, market_type, country)
VALUES ('105560', 'KB금융', 'KOSPI', 'KR');

INSERT INTO tbl_stock_alias (stock_code, alias)
VALUES ('105560', 'KB금융'),
       ('105560', 'KB Financial');

create table tbl_crawl_analysis_result
(
    id         bigint auto_increment primary key,
    target_id  bigint      not null,
    stock_code varchar(20) not null,
    match_type varchar(20) default 'DIRECT' comment '매칭 유형(DIRECT: 직접 언급, THEME: 테마 언급)',
    confidence float       default 1.0 comment '신뢰도 (단순 매칭 1.0, LLM 추론 시 변동)',
    sentiment  varchar(10) default 'NEUTRAL' comment 'POSITIVE, NEGATIVE, NEUTRAL',
    created_at timestamp   default current_timestamp,
    unique key uk_target_stock (target_id, stock_code)
);

-- 마켓 테마/ 정책 정의
create table tbl_market_theme
(
    id          bigint auto_increment primary key,
    theme_name  varchar(100) not null comment '테마명',
    description varchar(255) comment '테마설명',
    is_active   boolean   default true,
    created_at  timestamp default current_timestamp
);

-- 테마 감지 키워드
-- 하나의 테마를 찾기 위한 여러 키워드
create table tbl_market_theme_keyword
(
    id       bigint auto_increment primary key,
    theme_id bigint       not null,
    keyword  varchar(100) not null comment '감지할 단어',
    index idx_theme_keyword (keyword)
);

-- 테마-종목 매핑
-- 어떤 테마에 어떤 종목이 수혜/손해를 입는지 정의
-- 예: 반도체 지원법 (1) -> 삼성전자(0000000) , 하이닉스 (00000000)
create table tbl_market_theme_stock_map
(
    id         bigint auto_increment primary key,
    theme_id   bigint      not null,
    stock_code varchar(20) not null,
    reason     varchar(255) comment '연관 사유 (예: 파운드리 세제 혜택 수혜)',
    unique key uk_theme_stock (theme_id, stock_code)
);

-- 뉴스-테마 분석결과
-- 이 뉴스는 '반도체 지원법' 테마와 관련이 있다 라는 사실 저장
create table tbl_crawl_theme_result
(
    id         bigint auto_increment primary key,
    target_id  bigint not null,
    theme_id   bigint not null,
    confidence float default 1.0,
    unique key uk_target_theme (target_id, theme_id)
);

-- [초기 데이터 예시: 밸류업 프로그램]
insert into tbl_market_theme (theme_name, description)
values ('기업 밸류업 프로그램', '저PBR 주주환원 정책');
set @theme_id = last_insert_id();
insert into tbl_market_theme_keyword (theme_id, keyword)
values (@theme_id, '밸류업'),
       (@theme_id, '기업가치 제고'),
       (@theme_id, '저PBR');
-- 금융지주사 매핑
insert into tbl_market_theme_stock_map (theme_id, stock_code, reason)
values (@theme_id, '105560', 'KB금융: 주주환원율 확대 기대'),
       (@theme_id, '055550', '신한지주: 저PBR 대표주');