-- ============================================
-- CoopWork Schema (PTKT → CoopWork 확장)
-- ============================================

-- 조직
CREATE TABLE IF NOT EXISTS organization (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    slug              VARCHAR(100) NOT NULL UNIQUE,
    invite_code       VARCHAR(8),
    invite_expires_at TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(200) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 멤버십 (조직 ↔ 사용자)
-- role: OWNER, ADMIN, MEMBER
CREATE TABLE IF NOT EXISTS membership (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    role            VARCHAR(20) DEFAULT 'MEMBER',
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id)  REFERENCES organization(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_membership (org_id, user_id)
);

-- ── 기존 테이블에 새 컬럼 추가 (이미 존재하는 DB 업그레이드용) ──
ALTER TABLE organization ADD COLUMN IF NOT EXISTS invite_code       VARCHAR(8);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS invite_expires_at TIMESTAMP;
ALTER TABLE category  ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE kpi       ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE kpi       ADD COLUMN IF NOT EXISTS org_id   BIGINT;
ALTER TABLE kpi       ADD COLUMN IF NOT EXISTS scope    VARCHAR(20) DEFAULT 'PERSONAL';
ALTER TABLE kpi_record ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 알림
-- type: TASK_ASSIGNED, TASK_DONE, KPI_ACHIEVED, BLOCKER_HIGH
CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(500),
    link       VARCHAR(200),
    read_at    TIMESTAMP,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON notification(user_id, read_at);

-- 데일리 스크럼 (서버 저장)
CREATE TABLE IF NOT EXISTS scrum (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    org_id           BIGINT,
    scrum_date       DATE         NOT NULL,
    tasks_json       CLOB,
    blocker          VARCHAR(1000),
    blocker_severity VARCHAR(10)  DEFAULT 'LOW',
    energy           INT          DEFAULT 0,
    focus            VARCHAR(500),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_slack_sync_at TIMESTAMP,
    UNIQUE KEY uq_scrum_user_date (user_id, scrum_date)
);
ALTER TABLE scrum ADD COLUMN IF NOT EXISTS last_slack_sync_at TIMESTAMP;

-- 프로젝트
CREATE TABLE IF NOT EXISTS project (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id      BIGINT       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    owner_id    BIGINT       NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 태스크
-- status: BACKLOG, TODO, IN_PROGRESS, REVIEW, DONE
-- priority: P1, P2, P3
CREATE TABLE IF NOT EXISTS task (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT       NOT NULL,
    title            VARCHAR(300) NOT NULL,
    description      VARCHAR(2000),
    assignee_id      BIGINT,
    status           VARCHAR(20)  DEFAULT 'BACKLOG',
    priority         VARCHAR(5)   DEFAULT 'P2',
    kpi_id           BIGINT,
    kpi_contribution DECIMAL(15,2),
    due_date         DATE,
    estimated_hours  DECIMAL(5,1),
    sort_order       INT          DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

-- KPI 카테고리
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    color       VARCHAR(20)   DEFAULT '#4F46E5',
    icon        VARCHAR(100)  DEFAULT 'TrendingUp',
    description VARCHAR(500),
    sort_order  INT           DEFAULT 0,
    owner_id    BIGINT,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- KPI 정의
-- scope: PERSONAL(개인), TEAM(팀 전체 공유)
CREATE TABLE IF NOT EXISTS kpi (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    unit         VARCHAR(50),
    kpi_type     VARCHAR(20)   DEFAULT 'NUMERIC',
    target_value DECIMAL(15,2),
    frequency    VARCHAR(20)   DEFAULT 'DAILY',
    start_date   DATE          NOT NULL,
    end_date     DATE,
    status       VARCHAR(20)   DEFAULT 'ACTIVE',
    sort_order   INT           DEFAULT 0,
    owner_id     BIGINT,
    org_id       BIGINT,
    scope        VARCHAR(20)   DEFAULT 'PERSONAL',
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

-- KPI 기록
CREATE TABLE IF NOT EXISTS kpi_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    kpi_id         BIGINT        NOT NULL,
    actual_value   DECIMAL(15,2),
    boolean_value  BOOLEAN,
    recorded_date  DATE          NOT NULL,
    note           VARCHAR(1000),
    owner_id       BIGINT,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kpi_id) REFERENCES kpi(id) ON DELETE CASCADE,
    UNIQUE KEY uq_kpi_record_date (kpi_id, recorded_date)
);

-- 팀 채팅
CREATE TABLE IF NOT EXISTS chat_message (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id     BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    content    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_org ON chat_message(org_id, created_at);

-- AI 채팅 히스토리 (유저별)
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    role       VARCHAR(10)   NOT NULL,
    content    VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_chat_user ON ai_chat_history(user_id, created_at);
