create table tbl_crawl_proxy
(
    id            bigint primary key auto_increment,
    host          varchar(50) not null,
    port          int         not null,
    type          varchar(10) not null comment 'HTTP, ..',
    username      varchar(50) comment '인증이 필요한 경우 사용',
    password      varchar(50) comment '인증이 필요한 경우 사용',
    is_active     boolean     not null default true,
    fail_count    int         not null default 0,
    success_count bigint      not null default 0,
    last_used_at  timestamp,
    created_at    datetime,
    index idx_crawl_proxy (is_active, last_used_at)
);

create table tbl_user_agent (
    id bigint primary key auto_increment,
    agent_string varchar(500) not null ,
    device_type varchar(20) not null ,
    browser_type varchar(20),
    is_active boolean default true,
    created_at datetime default current_timestamp
);

INSERT INTO tbl_user_agent (agent_string, device_type, browser_type) VALUES
                                                                         ('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36', 'DESKTOP', 'CHROME'),
                                                                         ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36', 'DESKTOP', 'CHROME'),
                                                                         ('Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1', 'MOBILE', 'SAFARI');


