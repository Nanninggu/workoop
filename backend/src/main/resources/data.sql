-- ============================================
-- CoopWork 초기 샘플 데이터
-- ============================================
SELECT 1;

-- ── 테스트 계정 ──
-- admin@coopwork.io / admin1234
INSERT INTO users (id, email, password, name, created_at)
SELECT 1, 'admin@coopwork.io', '$2a$10$q6bHaokODSZodwe4/AGu5uX30lg/cCqz/hLcGi589DKXdnM2T.qAG', '관리자', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

-- test@coopwork.io / test1234
INSERT INTO users (id, email, password, name, created_at)
SELECT 2, 'test@coopwork.io', '$2a$10$ddxngHqFTa2oY.PArDsjSOxcAYoJB2mTThSTJ7jWNAQ/GA6W6hCui', '테스트유저', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 2);

-- 글로벌 카테고리 (owner_id NULL → 모든 사용자에게 노출)
INSERT INTO category (id, name, color, icon, description, sort_order)
SELECT 1, '매출/영업', '#10B981', 'TrendingUp', '매출 목표, 신규 계약, 고객 관리 KPI', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 1);

INSERT INTO category (id, name, color, icon, description, sort_order)
SELECT 2, '프로젝트 관리', '#3B82F6', 'Briefcase', '프로젝트 진행률, 납기 준수, 마일스톤 KPI', 2
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 2);

INSERT INTO category (id, name, color, icon, description, sort_order)
SELECT 3, '업무 효율', '#8B5CF6', 'Zap', '업무 집중도, 처리 건수, 품질 관련 KPI', 3
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 3);

INSERT INTO category (id, name, color, icon, description, sort_order)
SELECT 4, '팀 협업', '#F97316', 'Users', '팀 커뮤니케이션, 협업, 기여도 KPI', 4
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 4);

INSERT INTO category (id, name, color, icon, description, sort_order)
SELECT 5, '역량 개발', '#06B6D4', 'BookOpen', '교육 이수, 자격증, 스킬 향상 KPI', 5
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 5);
