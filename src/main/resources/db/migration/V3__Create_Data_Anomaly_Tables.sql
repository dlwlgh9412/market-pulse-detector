create table tbl_data_anomaly_log
(
    id            bigint auto_increment primary key,
    target_key    varchar(255) not null comment '문제가 발생한 데이터 식별자 (예: ThemeMap:123)',
    anomaly_type  varchar(50)  not null comment '이상 유형 (예: MISSING_DATA, PARSE_FAIL)',
    payload       text comment '상세 내용 또는 스냅샷',
    status        varchar(20) default 'NEW' comment 'NEW, RESOLVED, IGNORED',
    occurrence    int         default 1,
    first_seen_at timestamp   default current_timestamp,
    last_seen_at  timestamp   default current_timestamp,
    resolved_at   timestamp    null,

    unique key uk_anomaly (target_key, anomaly_type)
);