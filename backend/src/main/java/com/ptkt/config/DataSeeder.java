package com.ptkt.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    private static final long ORG_ID = 1L;

    private long ADMIN_ID;
    private long TEST_ID;
    private long DEV_ID;
    private long DES_ID;
    private long PARK_ID;   // 박주현
    private long JOH_ID;    // 오정한
    private long JHLEE_ID;  // 이정환
    private long KIMNW_ID;  // 김남우
    private long SEUNG_ID;  // 김승현

    @Override
    public void run(String... args) {
        log.info("=== DataSeeder 시작 (local 프로파일) ===");

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS seed_meta (
                id BIGINT PRIMARY KEY,
                seeded_at TIMESTAMP
            )
            """);

        // 조직 1 이름/슬러그 보장 (멱등) — 'CoopWork 데모팀' / coopwork-demo
        ensureOrg1Name();

        // 사용자/멤버십은 매번 보장 (멱등) — 시드 마커와 무관하게 추가 가능
        ensureUsersAndMemberships();

        if ("true".equalsIgnoreCase(System.getenv("SEED_FORCE_RESET"))) {
            log.warn("[seed] SEED_FORCE_RESET=true → 마커 삭제하고 강제 재시드");
            jdbc.update("DELETE FROM seed_meta WHERE id = 1");
        }

        Integer marker = jdbc.queryForObject(
            "SELECT COUNT(*) FROM seed_meta WHERE id = 1", Integer.class);
        if (marker != null && marker > 0) {
            log.info("[seed] 이미 시드 완료 (seed_meta). skip");
            return;
        }

        log.info("[seed] 풀 리셋 모드 — 기존 데이터 삭제 후 시드");
        resetAll();

        seedKpis();
        seedKpiRecords();
        seedProjects();
        seedTasks();
        seedScrums();
        seedChatMessages();
        seedNotifications();
        seedAiChatHistory();
        seedStarNotes();
        seedReviews();

        jdbc.update("INSERT INTO seed_meta (id, seeded_at) VALUES (1, CURRENT_TIMESTAMP)");
        log.info("=== DataSeeder 완료 (seed_meta 마킹) ===");
    }

    private void ensureOrg1Name() {
        // 시드 데이터가 들어있는 조직(id=1)의 이름을 'CoopWork 데모팀'으로 통일
        int n = jdbc.update(
            "UPDATE organization SET name = ?, slug = ? WHERE id = ?",
            "CoopWork 데모팀", "coopwork-demo", ORG_ID);
        if (n > 0) log.info("[seed] org id={} 이름 통일: 'CoopWork 데모팀' / slug=coopwork-demo", ORG_ID);
    }

    private void ensureUsersAndMemberships() {
        // 시드 데이터의 주인 4명 (인스턴스 필드 갱신)
        ADMIN_ID = ensureUser("admin@coopwork.io",  "관리자");
        TEST_ID  = ensureUser("test@coopwork.io",   "테스트유저");
        DEV_ID   = ensureUser("dev@coopwork.io",    "김개발");
        DES_ID   = ensureUser("design@coopwork.io", "박디자인");

        // 실제 사용자 (개인 KPI·스크럼 데이터 포함)
        PARK_ID  = ensureUser("parkjh10@asianaidt.com", "박주현");
        JOH_ID   = ensureUser("joh@hist.co.kr",         "오정환");
        JHLEE_ID = ensureUser("jh-lee@hist.co.kr",      "이준희");
        KIMNW_ID = ensureUser("kimnw@asianaidt.com",    "김남욱");
        SEUNG_ID = ensureUser("seungkim@hist.co.kr",    "김승현");

        // 이름 강제 통일 (기존 계정도 덮어씀)
        forceResetName("parkjh10@asianaidt.com", "박주현");
        forceResetName("joh@hist.co.kr",         "오정환");
        forceResetName("jh-lee@hist.co.kr",      "이준희");
        forceResetName("kimnw@asianaidt.com",    "김남욱");
        forceResetName("seungkim@hist.co.kr",    "김승현");

        log.info("[seed] user IDs: admin={}, test={}, dev={}, design={}, park={}, joh={}, jhlee={}, kimnw={}, seung={}",
                 ADMIN_ID, TEST_ID, DEV_ID, DES_ID, PARK_ID, JOH_ID, JHLEE_ID, KIMNW_ID, SEUNG_ID);

        ensureMembership(ORG_ID, ADMIN_ID,  "OWNER");
        ensureMembership(ORG_ID, TEST_ID,   "MEMBER");
        ensureMembership(ORG_ID, DEV_ID,    "MEMBER");
        ensureMembership(ORG_ID, DES_ID,    "MEMBER");
        ensureMembership(ORG_ID, PARK_ID,   "MEMBER");
        ensureMembership(ORG_ID, JOH_ID,    "MEMBER");
        ensureMembership(ORG_ID, JHLEE_ID,  "MEMBER");
        ensureMembership(ORG_ID, KIMNW_ID,  "MEMBER");
        ensureMembership(ORG_ID, SEUNG_ID,  "MEMBER");

        // 데모 계정들은 ORG_ID(1) 외 다른 조직 멤버십 제거 (잘못 합류된 경우 정리)
        for (long uid : new long[]{ PARK_ID, JOH_ID, JHLEE_ID, KIMNW_ID, SEUNG_ID,
                                    DEV_ID, DES_ID, TEST_ID }) {
            int removed = jdbc.update(
                "DELETE FROM membership WHERE user_id = ? AND org_id != ?", uid, ORG_ID);
            if (removed > 0) log.info("[seed] 잉여 org 멤버십 정리: user_id={}", uid);
        }

        // 데모 사용자 비밀번호 강제 통일 (test1234)
        // admin/test는 data.sql에서 관리하므로 제외, 사용자 본인(may9noy@)도 손대지 않음
        forceResetPassword("dev@coopwork.io",        "test1234");
        forceResetPassword("design@coopwork.io",     "test1234");
        forceResetPassword("parkjh10@asianaidt.com", "test1234");
        forceResetPassword("joh@hist.co.kr",          "test1234");
        forceResetPassword("jh-lee@hist.co.kr",       "test1234");
        forceResetPassword("kimnw@asianaidt.com",     "test1234");
        forceResetPassword("seungkim@hist.co.kr",     "test1234");
    }

    private void forceResetPassword(String email, String rawPassword) {
        int n = jdbc.update(
            "UPDATE users SET password = ? WHERE email = ?",
            passwordEncoder.encode(rawPassword), email);
        if (n > 0) log.info("[seed] password 통일: {}", email);
    }

    private void forceResetName(String email, String name) {
        int n = jdbc.update(
            "UPDATE users SET name = ? WHERE email = ?",
            name, email);
        if (n > 0) log.info("[seed] name 통일: {} → {}", email, name);
    }

    private long ensureUser(String email, String name) {
        List<Long> ids = jdbc.queryForList(
            "SELECT id FROM users WHERE email = ?", Long.class, email);
        if (!ids.isEmpty()) {
            log.info("[seed] user 존재: {} → id={}", email, ids.get(0));
            return ids.get(0);
        }
        String hashed = passwordEncoder.encode("test1234");
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (email, password, name, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                new String[] { "id" });
            ps.setString(1, email);
            ps.setString(2, hashed);
            ps.setString(3, name);
            return ps;
        }, kh);
        long id = kh.getKey().longValue();
        log.info("[seed] user 생성: {} → id={}", email, id);
        return id;
    }

    private void ensureMembership(long orgId, long userId, String role) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM membership WHERE org_id = ? AND user_id = ?",
            Integer.class, orgId, userId);
        if (c != null && c > 0) return;
        jdbc.update(
            "INSERT INTO membership (org_id, user_id, role, joined_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            orgId, userId, role);
        log.info("[seed] membership: org={} user={} role={}", orgId, userId, role);
    }

    private void resetAll() {
        // FK 의존성 순서: 자식 → 부모
        jdbc.execute("DELETE FROM task");
        jdbc.execute("DELETE FROM kpi_record");
        jdbc.execute("DELETE FROM project");
        jdbc.execute("DELETE FROM kpi");
        jdbc.execute("DELETE FROM scrum");
        jdbc.execute("DELETE FROM chat_message");
        jdbc.execute("DELETE FROM notification");
        jdbc.execute("DELETE FROM ai_chat_history");
        jdbc.execute("DELETE FROM star_note");
        jdbc.execute("DELETE FROM review");
        // ID 시퀀스 리셋 (H2 MySQL 모드)
        jdbc.execute("ALTER TABLE task           ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE kpi_record     ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE project        ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE kpi            ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE scrum          ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE chat_message   ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE notification   ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE ai_chat_history ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE star_note      ALTER COLUMN id RESTART WITH 1");
        jdbc.execute("ALTER TABLE review         ALTER COLUMN id RESTART WITH 1");
        log.info("[seed] 리셋 완료");
    }

    private boolean isEmpty(String table) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return c == null || c == 0;
    }

    // 1. KPI (admin/test 개인 + 팀)
    private void seedKpis() {
        if (!isEmpty("kpi")) {
            log.info("[seed] kpi 이미 존재, skip");
            return;
        }
        LocalDate start = LocalDate.now().minusDays(60);
        // 개인 KPI - admin
        insertKpi(1, "월 매출 목표", "이번 달 신규 매출", "만원", "NUMERIC", 5000, "MONTHLY", ADMIN_ID, "PERSONAL", start);
        insertKpi(3, "주간 코드 리뷰 수", "주간 PR 리뷰 건수", "건", "NUMERIC", 15, "WEEKLY", ADMIN_ID, "PERSONAL", start);
        insertKpi(5, "일일 학습 30분", "매일 30분 이상 학습", null, "BOOLEAN", 1, "DAILY", ADMIN_ID, "PERSONAL", start);
        // 개인 KPI - test
        insertKpi(2, "신규 고객 미팅", "주간 신규 고객 미팅 횟수", "건", "NUMERIC", 5, "WEEKLY", TEST_ID, "PERSONAL", start);
        insertKpi(3, "일일 처리 태스크", "하루에 완료한 태스크 수", "건", "NUMERIC", 4, "DAILY", TEST_ID, "PERSONAL", start);
        // 팀 KPI
        insertKpi(2, "분기 프로젝트 완료", "Q2 프로젝트 출시", "건", "NUMERIC", 3, "MONTHLY", ADMIN_ID, "TEAM", start);
        insertKpi(4, "팀 데일리 스크럼 참여율", "팀원 데일리 스크럼 작성", "%", "NUMERIC", 100, "DAILY", ADMIN_ID, "TEAM", start);
        insertKpi(1, "팀 매출", "팀 전체 분기 매출", "만원", "NUMERIC", 30000, "MONTHLY", ADMIN_ID, "TEAM", start);
        // 개인 KPI - 김개발
        insertKpi(2, "주간 배포 횟수",       "주간 프로덕션 배포 건수",   "회", "NUMERIC", 3,  "WEEKLY", DEV_ID, "PERSONAL", start);
        insertKpi(3, "일일 코드 커밋",        "하루 Git 커밋 횟수",        "건", "NUMERIC", 5,  "DAILY",  DEV_ID, "PERSONAL", start);
        insertKpi(5, "주간 기술 문서 작성",   "주간 기술 문서/위키 작성",  "건", "NUMERIC", 2,  "WEEKLY", DEV_ID, "PERSONAL", start);
        // 개인 KPI - 박디자인
        insertKpi(3, "주간 디자인 산출물",    "주간 디자인 시안 완성 건수","건", "NUMERIC", 5,  "WEEKLY", DES_ID, "PERSONAL", start);
        insertKpi(4, "UI 컴포넌트 기여",      "주간 컴포넌트 라이브러리 기여", "건", "NUMERIC", 3, "WEEKLY", DES_ID, "PERSONAL", start);
        insertKpi(5, "사용성 테스트 진행",    "월간 사용성 테스트 횟수",   "회", "NUMERIC", 2,  "MONTHLY",DES_ID, "PERSONAL", start);
        // 개인 KPI - 박주현
        insertKpi(2, "주간 PR 리뷰 완료", "주간 PR 리뷰 건수", "건", "NUMERIC", 10, "WEEKLY", PARK_ID, "PERSONAL", start);
        insertKpi(3, "일일 태스크 처리", "하루 완료 태스크 수", "건", "NUMERIC", 5, "DAILY", PARK_ID, "PERSONAL", start);
        insertKpi(5, "주간 기술 학습", "주간 자기계발 학습 시간", "시간", "NUMERIC", 4, "WEEKLY", PARK_ID, "PERSONAL", start);
        // 개인 KPI - 오정한
        insertKpi(3, "주간 버그 수정", "주간 버그 처리 건수", "건", "NUMERIC", 8, "WEEKLY", JOH_ID, "PERSONAL", start);
        insertKpi(2, "월 배포 횟수", "월간 프로덕션 배포 횟수", "회", "NUMERIC", 4, "MONTHLY", JOH_ID, "PERSONAL", start);
        insertKpi(5, "일일 학습 기록", "매일 학습 여부", null, "BOOLEAN", 1, "DAILY", JOH_ID, "PERSONAL", start);
        // 개인 KPI - 이정환
        insertKpi(1, "고객 문의 처리", "일일 고객 문의 처리 건수", "건", "NUMERIC", 10, "DAILY", JHLEE_ID, "PERSONAL", start);
        insertKpi(5, "자격증 취득 준비", "주간 자격증 공부 시간", "시간", "NUMERIC", 5, "WEEKLY", JHLEE_ID, "PERSONAL", start);
        insertKpi(3, "주간 코드 커밋", "주간 Git 커밋 횟수", "건", "NUMERIC", 15, "WEEKLY", JHLEE_ID, "PERSONAL", start);
        // 개인 KPI - 김남우
        insertKpi(4, "팀 코드리뷰 참여", "주간 코드리뷰 참여 건수", "건", "NUMERIC", 12, "WEEKLY", KIMNW_ID, "PERSONAL", start);
        insertKpi(2, "스프린트 완료율", "스프린트 태스크 완료율", "%", "NUMERIC", 90, "WEEKLY", KIMNW_ID, "PERSONAL", start);
        insertKpi(3, "일일 집중 개발", "매일 3시간 이상 집중 개발", null, "BOOLEAN", 1, "DAILY", KIMNW_ID, "PERSONAL", start);
        // 개인 KPI - 김승현
        insertKpi(3, "일일 개발 집중 시간", "하루 집중 개발 시간", "시간", "NUMERIC", 6, "DAILY", SEUNG_ID, "PERSONAL", start);
        insertKpi(2, "주간 커밋 수", "주간 Git 커밋 횟수", "건", "NUMERIC", 20, "WEEKLY", SEUNG_ID, "PERSONAL", start);
        insertKpi(4, "팀 미팅 준비", "매일 스크럼 준비 여부", null, "BOOLEAN", 1, "DAILY", SEUNG_ID, "PERSONAL", start);
        log.info("[seed] kpi +29");
    }

    private void insertKpi(int categoryId, String name, String desc, String unit,
                           String type, int target, String freq, long ownerId,
                           String scope, LocalDate start) {
        jdbc.update("""
            INSERT INTO kpi (category_id, name, description, unit, kpi_type, target_value,
                             frequency, start_date, status, owner_id, org_id, scope,
                             created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, categoryId, name, desc, unit, type, target, freq, start, ownerId, scope);
    }

    // 3. KPI 기록 (최근 30일, 70% 확률로 기록)
    private void seedKpiRecords() {
        if (!isEmpty("kpi_record")) {
            log.info("[seed] kpi_record 이미 존재, skip");
            return;
        }
        List<Long> kpiIds = jdbc.queryForList("SELECT id FROM kpi", Long.class);
        Random r = new Random(42);
        int inserted = 0;
        for (Long kpiId : kpiIds) {
            String type = jdbc.queryForObject("SELECT kpi_type FROM kpi WHERE id = ?", String.class, kpiId);
            Double target = jdbc.queryForObject("SELECT target_value FROM kpi WHERE id = ?", Double.class, kpiId);
            Long ownerId = jdbc.queryForObject("SELECT owner_id FROM kpi WHERE id = ?", Long.class, kpiId);
            for (int d = 0; d < 30; d++) {
                if (r.nextDouble() > 0.7) continue; // 70% 확률로 기록
                LocalDate date = LocalDate.now().minusDays(d);
                if ("BOOLEAN".equals(type)) {
                    boolean done = r.nextDouble() < 0.75;
                    jdbc.update("""
                        INSERT INTO kpi_record (kpi_id, boolean_value, recorded_date, owner_id, created_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """, kpiId, done, date, ownerId);
                } else {
                    double base = target == null ? 100 : target;
                    double actual = base * (0.4 + r.nextDouble() * 0.9); // 40~130% 분포
                    jdbc.update("""
                        INSERT INTO kpi_record (kpi_id, actual_value, recorded_date, owner_id, created_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """, kpiId, Math.round(actual * 100) / 100.0, date, ownerId);
                }
                inserted++;
            }
        }
        log.info("[seed] kpi_record +{}", inserted);
    }

    // 4. 프로젝트
    private void seedProjects() {
        if (!isEmpty("project")) {
            log.info("[seed] project 이미 존재, skip");
            return;
        }
        jdbc.update("INSERT INTO project (id, org_id, name, description, owner_id, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
            1, ORG_ID, "신규 SaaS 출시", "Q2 출시 목표 - 결제 모듈 + 온보딩", ADMIN_ID);
        jdbc.update("INSERT INTO project (id, org_id, name, description, owner_id, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
            2, ORG_ID, "고객 대시보드 리뉴얼", "사용성 개선 + 신규 차트 추가", TEST_ID);
        jdbc.update("INSERT INTO project (id, org_id, name, description, owner_id, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
            3, ORG_ID, "모바일 앱 베타", "iOS/Android 베타 출시 준비", ADMIN_ID);
        log.info("[seed] project +3");
    }

    // 5. 태스크
    private void seedTasks() {
        if (!isEmpty("task")) {
            log.info("[seed] task 이미 존재, skip");
            return;
        }
        // (project_id, title, status, priority, assignee_id, kpi_id, assignee_ids)
        Object[][] tasks = {
            // Project 1 - 신규 SaaS
            {1, "결제 게이트웨이 연동",        "DONE",        "P1", ADMIN_ID, 6L,   "[" + ADMIN_ID + "," + DEV_ID + "]"},
            {1, "온보딩 플로우 와이어프레임",   "DONE",        "P2", DES_ID,   6L,   "[" + DES_ID + "]"},
            {1, "결제 정산 리포트 화면",        "IN_PROGRESS", "P1", DEV_ID,   6L,   "[" + DEV_ID + "," + PARK_ID + "]"},
            {1, "이메일 인증 모듈",             "IN_PROGRESS", "P2", TEST_ID,  null, "[" + TEST_ID + "]"},
            {1, "관리자 대시보드 권한 분리",    "REVIEW",      "P2", ADMIN_ID, null, "[" + ADMIN_ID + "," + DEV_ID + "," + SEUNG_ID + "]"},
            {1, "결제 에러 핸들링 개선",        "TODO",        "P1", DEV_ID,   null, "[" + DEV_ID + "," + JOH_ID + "]"},
            {1, "쿠폰 시스템 설계",             "TODO",        "P3", TEST_ID,  null, "[" + TEST_ID + "]"},
            {1, "QA 테스트 시나리오 작성",      "BACKLOG",     "P2", null,     null, null},
            // Project 2 - 대시보드 리뉴얼
            {2, "기존 대시보드 사용자 분석",    "DONE",        "P2", TEST_ID,  null, "[" + TEST_ID + "]"},
            {2, "신규 차트 라이브러리 선정",    "DONE",        "P2", DEV_ID,   null, "[" + DEV_ID + "," + KIMNW_ID + "]"},
            {2, "메인 차트 컴포넌트 구현",      "IN_PROGRESS", "P1", DEV_ID,   null, "[" + DEV_ID + "," + SEUNG_ID + "]"},
            {2, "필터 UI 리디자인",             "IN_PROGRESS", "P2", DES_ID,   null, "[" + DES_ID + "," + PARK_ID + "]"},
            {2, "성능 최적화 (lazy loading)",   "TODO",        "P2", DEV_ID,   null, "[" + DEV_ID + "]"},
            {2, "접근성 검토",                  "TODO",        "P3", DES_ID,   null, "[" + DES_ID + "]"},
            {2, "베타 사용자 피드백 수집",      "BACKLOG",     "P3", TEST_ID,  null, "[" + TEST_ID + "," + JHLEE_ID + "]"},
            // Project 3 - 모바일 앱 베타
            {3, "iOS 앱 스토어 등록 준비",      "DONE",        "P1", ADMIN_ID, null, "[" + ADMIN_ID + "]"},
            {3, "Android 빌드 파이프라인",      "IN_PROGRESS", "P1", DEV_ID,   null, "[" + DEV_ID + "," + SEUNG_ID + "]"},
            {3, "푸시 알림 SDK 통합",           "IN_PROGRESS", "P2", DEV_ID,   null, "[" + DEV_ID + "]"},
            {3, "베타 테스터 모집 페이지",      "REVIEW",      "P2", DES_ID,   null, "[" + DES_ID + "," + PARK_ID + "]"},
            {3, "사용자 가이드 영상 제작",      "TODO",        "P3", DES_ID,   null, "[" + DES_ID + "]"},
            {3, "크래시 리포팅 도구 도입",      "TODO",        "P2", DEV_ID,   null, "[" + DEV_ID + "," + JOH_ID + "]"},
            {3, "프리미엄 기능 가격 정책",      "BACKLOG",     "P2", ADMIN_ID, null, "[" + ADMIN_ID + "," + TEST_ID + "]"},
            {3, "온보딩 튜토리얼 UX",           "BACKLOG",     "P3", DES_ID,   null, "[" + DES_ID + "]"},
            {3, "성능 모니터링 대시보드",       "BACKLOG",     "P3", TEST_ID,  null, "[" + TEST_ID + "," + KIMNW_ID + "]"},
        };
        int idx = 0;
        for (Object[] t : tasks) {
            LocalDate due = LocalDate.now().plusDays((idx % 14) - 7);
            jdbc.update("""
                INSERT INTO task (project_id, title, status, priority, assignee_id, kpi_id,
                                  assignee_ids, due_date, estimated_hours, sort_order,
                                  created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, t[0], t[1], t[2], t[3], t[4], t[5], t[6], due, 4 + (idx % 8), idx);
            idx++;
        }
        log.info("[seed] task +{}", tasks.length);
    }

    // 6. 스크럼 (모든 유저 최근 14일치, 80% 확률)
    private void seedScrums() {
        if (!isEmpty("scrum")) {
            log.info("[seed] scrum 이미 존재, skip");
            return;
        }
        Long[] userIds = { ADMIN_ID, TEST_ID, DEV_ID, DES_ID, PARK_ID, JOH_ID, JHLEE_ID, KIMNW_ID, SEUNG_ID };
        String[] focuses = {
            "결제 게이트웨이 마무리", "신규 차트 컴포넌트 구현", "와이어프레임 검토",
            "베타 테스터 모집 페이지", "사용자 분석 보고서 작성", "코드 리뷰 + 리팩토링",
            "QA 시나리오 작성", "온보딩 플로우 폴리싱", "성능 최적화 측정"
        };
        String[] blockers = {
            "", "", "", "결제 API 응답 지연",
            "디자인 시스템 컴포넌트 부족", "QA 환경 불안정"
        };
        String[] severities = { "LOW", "MEDIUM", "HIGH" };
        Random r = new Random(7);
        int inserted = 0;
        for (Long uid : userIds) {
            for (int d = 0; d < 14; d++) {
                if (r.nextDouble() > 0.8) continue;
                LocalDate date = LocalDate.now().minusDays(d);
                String focus = focuses[r.nextInt(focuses.length)];
                String blocker = blockers[r.nextInt(blockers.length)];
                String severity = severities[r.nextInt(severities.length)];
                int energy = 3 + r.nextInt(5); // 3~7
                String tasksJson = String.format(
                    "[{\"title\":\"%s\",\"done\":%b},{\"title\":\"%s\",\"done\":%b},{\"title\":\"%s\",\"done\":%b}]",
                    "오전 핵심 업무 처리", r.nextDouble() < 0.85,
                    "팀 미팅 및 의사결정", r.nextDouble() < 0.9,
                    "오후 집중 개발", r.nextDouble() < 0.7
                );
                jdbc.update("""
                    INSERT INTO scrum (user_id, org_id, scrum_date, tasks_json, blocker,
                                       blocker_severity, energy, focus, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, uid, ORG_ID, date, tasksJson, blocker, severity, energy, focus);
                inserted++;
            }
        }
        log.info("[seed] scrum +{}", inserted);
    }

    // 7. 팀 채팅
    private void seedChatMessages() {
        if (!isEmpty("chat_message")) {
            log.info("[seed] chat_message 이미 존재, skip");
            return;
        }
        Object[][] msgs = {
            { ADMIN_ID, "팀원 여러분, 오늘 스크럼 진행 부탁드려요!" },
            { TEST_ID,  "결제 모듈 연동 PR 올렸습니다. 리뷰 부탁해요 🙏" },
            { DEV_ID,   "리뷰 시작합니다. 30분 안에 코멘트 남길게요." },
            { DES_ID,   "온보딩 와이어프레임 v2 공유드립니다. 피드백 환영!" },
            { ADMIN_ID, "디자인 좋네요. 회원가입 단계는 좀 더 단순화하면 좋겠어요." },
            { TEST_ID,  "고객사 미팅에서 모바일 푸시 알림 요청이 많았어요." },
            { DEV_ID,   "푸시 SDK 통합 작업 이번 주에 시작합니다." },
            { DES_ID,   "@박디자인 모바일 푸시 알림 디자인 가이드도 같이 준비할게요." },
            { ADMIN_ID, "내일 오전 10시 스프린트 회고 미팅 잊지 마세요!" },
            { TEST_ID,  "다들 수고하셨어요. 좋은 주말 되세요 🎉" }
        };
        int idx = 0;
        for (Object[] m : msgs) {
            jdbc.update("""
                INSERT INTO chat_message (org_id, user_id, content, created_at)
                VALUES (?, ?, ?, DATEADD('HOUR', ?, CURRENT_TIMESTAMP))
                """, ORG_ID, m[0], m[1], -(msgs.length - idx) * 2);
            idx++;
        }
        log.info("[seed] chat_message +{}", msgs.length);
    }

    // 8. 알림
    private void seedNotifications() {
        if (!isEmpty("notification")) {
            log.info("[seed] notification 이미 존재, skip");
            return;
        }
        Object[][] notifs = {
            { ADMIN_ID, "TASK_ASSIGNED", "새 태스크 배정",   "결제 정산 리포트 화면 작업이 배정되었습니다", "/projects/1/board" },
            { ADMIN_ID, "KPI_ACHIEVED",  "KPI 달성!",        "월 매출 목표 100% 달성을 축하합니다 🎉",      "/kpis" },
            { ADMIN_ID, "BLOCKER_HIGH",  "긴급 블로커",      "팀 스크럼에 HIGH 블로커가 등록되었습니다",     "/scrum" },
            { TEST_ID,  "TASK_DONE",     "태스크 완료",      "기존 대시보드 사용자 분석이 완료되었습니다",   "/projects/2/board" },
            { TEST_ID,  "TASK_ASSIGNED", "리뷰 요청",        "결제 모듈 PR 리뷰 요청",                       "/projects/1/board" },
            { DEV_ID,   "TASK_ASSIGNED", "새 태스크 배정",   "Android 빌드 파이프라인",                      "/projects/3/board" },
            { DEV_ID,    "KPI_ACHIEVED",  "주간 KPI 달성",    "이번 주 코드 리뷰 목표를 달성했어요",            "/kpis" },
            { DES_ID,    "TASK_ASSIGNED", "디자인 요청",       "사용자 가이드 영상 제작 배정",                  "/projects/3/board" },
            { PARK_ID,   "TASK_ASSIGNED", "새 태스크 배정",    "필터 UI 리디자인 작업이 배정되었습니다",         "/projects/2/board" },
            { PARK_ID,   "KPI_ACHIEVED",  "KPI 달성!",         "주간 PR 리뷰 목표를 달성했습니다 🎉",           "/kpis" },
            { JOH_ID,    "TASK_ASSIGNED", "버그 수정 요청",    "결제 에러 핸들링 개선 작업이 배정되었습니다",   "/projects/1/board" },
            { JOH_ID,    "BLOCKER_HIGH",  "블로커 등록",       "QA 환경 불안정 블로커가 등록되었습니다",         "/scrum" },
            { JHLEE_ID,  "TASK_ASSIGNED", "새 태스크 배정",    "베타 사용자 피드백 수집 작업이 배정되었습니다",  "/projects/2/board" },
            { JHLEE_ID,  "KPI_ACHIEVED",  "KPI 달성!",         "고객 문의 처리 목표를 달성했습니다 🎉",          "/kpis" },
            { KIMNW_ID,  "TASK_ASSIGNED", "코드리뷰 요청",     "메인 차트 컴포넌트 구현 PR 리뷰 요청",          "/projects/2/board" },
            { KIMNW_ID,  "KPI_ACHIEVED",  "스프린트 완료",     "이번 스프린트 목표 90% 달성했습니다 🎉",         "/kpis" },
            { SEUNG_ID,  "TASK_ASSIGNED", "새 태스크 배정",    "Android 빌드 파이프라인 작업이 배정되었습니다",  "/projects/3/board" },
            { SEUNG_ID,  "KPI_ACHIEVED",  "KPI 달성!",         "주간 커밋 목표를 달성했습니다 🎉",               "/kpis" }
        };
        for (Object[] n : notifs) {
            jdbc.update("""
                INSERT INTO notification (user_id, type, title, body, link, created_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, n[0], n[1], n[2], n[3], n[4]);
        }
        log.info("[seed] notification +{}", notifs.length);
    }

    // 10. STAR 노트
    private void seedStarNotes() {
        if (!isEmpty("star_note")) { log.info("[seed] star_note 이미 존재, skip"); return; }
        Object[][] notes = {
            // userId, title, lpTag, situation, task, action, result
            { ADMIN_ID, "API 레이턴시 50% 개선 프로젝트",
              "bias_action",
              "2024년 Q1, 사용자 API 레이턴시가 급증하여 SLA 99.9%가 위협받고 있었습니다.",
              "API 응답시간을 200ms 이하로 줄여 SLA를 회복하고 사용자 경험을 개선해야 했습니다.",
              "CloudWatch로 병목을 파악하고, ElastiCache 캐시 계층을 새로 설계했습니다. 이후 비동기 처리로 전환했습니다.",
              "API 레이턴시 52% 감소(380ms→182ms), SLA 99.9% 회복, 고객 불만 티켓 80% 감소." },
            { ADMIN_ID, "팀 온보딩 프로세스 구축",
              "hire_develop",
              "신규 팀원이 3개월 연속 합류하면서 기존 비공식 온보딩 방식의 한계가 드러났습니다.",
              "2주 안에 신규 팀원이 독립적으로 기여할 수 있는 체계적인 온보딩 프로세스를 만들어야 했습니다.",
              "기존 팀원 인터뷰로 Pain Point를 수집하고, 온보딩 체크리스트·Runbook·페어 프로그래밍 세션을 설계했습니다.",
              "신규 팀원 첫 PR 소요기간 15일→6일 단축, 팀 만족도 조사 4.2/5.0 달성." },
            { TEST_ID, "신규 고객 미팅 월간 목표 초과 달성",
              "deliver_results",
              "Q2 영업 목표로 월 5건 신규 고객 미팅이 설정됐으나 첫 달 2건으로 마감했습니다.",
              "2개월 안에 월 5건 이상 달성하고 파이프라인을 안정화해야 했습니다.",
              "LinkedIn 아웃바운드를 주 20건으로 늘리고, 데모 자료를 업종별로 맞춤 제작했습니다.",
              "3개월 차에 월 8건 달성, 분기 누적 파이프라인 2.3억원 확보." },
            { DEV_ID, "CI/CD 파이프라인 구축으로 배포 시간 단축",
              "invent_simplify",
              "매 배포마다 수동 빌드·테스트·배포 과정에 평균 90분이 소요됐습니다.",
              "배포 자동화로 엔지니어 당 주당 2시간 이상 절감하는 것이 목표였습니다.",
              "GitHub Actions로 CI 파이프라인을 구성하고, Staging→Production Blue/Green 배포를 자동화했습니다.",
              "배포 시간 90분→12분(87% 단축), 롤백 소요 시간 30분→2분, 장애 발생률 40% 감소." },
            { DES_ID, "디자인 시스템 구축 및 컴포넌트 라이브러리 제작",
              "highest_standards",
              "제품 내 버튼·폼·모달 등 UI 요소가 팀마다 제각각으로 구현되어 일관성이 없었습니다.",
              "핵심 컴포넌트 30종을 표준화하고 Figma 라이브러리와 코드 컴포넌트를 동기화해야 했습니다.",
              "컴포넌트 감사를 진행해 중복 요소를 파악하고, 디자인 토큰(색상·타이포·여백)을 정의했습니다.",
              "컴포넌트 30종 표준화, 개발팀 UI 구현 시간 35% 단축, 디자인 QA 이슈 60% 감소." },
            { PARK_ID, "PR 리뷰 프로세스 표준화로 머지 대기시간 단축",
              "highest_standards",
              "팀 내 PR 리뷰 기준이 불명확해 같은 코드에 대해 리뷰어마다 다른 기준을 적용했습니다.",
              "PR 리뷰 체크리스트와 기준을 정의하고, 평균 머지 대기 시간을 3일 이내로 줄여야 했습니다.",
              "팀원들과 리뷰 기준 워크숍을 진행하고, PR 템플릿과 자동 리뷰어 지정 룰을 설정했습니다.",
              "평균 PR 머지 대기시간 5.2일→2.1일 단축, 리뷰 관련 갈등 0건, 코드 품질 지표 12% 향상." },
            { JOH_ID, "배포 파이프라인 자동화로 장애율 0%대 달성",
              "invent_simplify",
              "수동 배포 과정에서 체크리스트 누락으로 분기에 2~3건의 배포 장애가 발생했습니다.",
              "배포 장애를 완전히 없애고, 배포 소요 시간을 절반 이하로 줄이는 것이 목표였습니다.",
              "배포 체크리스트를 자동화 스크립트로 전환하고, 스테이징 환경 검증을 의무화했습니다.",
              "배포 장애 연간 0건 달성, 배포 소요 시간 45분→18분 단축, 팀 야간 대응 50% 감소." },
            { JHLEE_ID, "고객 응대 응답시간 2시간→30분 단축",
              "customer_obsession",
              "고객 지원 채널의 평균 응답 시간이 2시간을 넘어 NPS 점수가 하락하고 있었습니다.",
              "고객 문의 응답 시간을 30분 이내로 줄이고, CSAT 점수를 4.0 이상으로 올려야 했습니다.",
              "문의 유형별 FAQ를 구축하고, 자동 응답 템플릿과 에스컬레이션 기준을 재설계했습니다.",
              "평균 응답시간 2시간→28분, CSAT 3.4→4.3, 반복 문의 42% 감소." },
            { KIMNW_ID, "스프린트 완료율 65%→90% 개선",
              "deliver_results",
              "스프린트 마감마다 30~40%의 태스크가 미완료로 다음 스프린트로 이월됐습니다.",
              "스프린트 완료율을 90% 이상으로 끌어올리고, 이월 태스크를 5% 미만으로 줄여야 했습니다.",
              "스토리 포인트 추정 워크숍을 도입하고, 데일리 스크럼에서 블로커를 즉시 가시화했습니다.",
              "스프린트 완료율 65%→91%, 이월 태스크 비율 35%→4%, 팀 속도 지표 25% 향상." },
            { SEUNG_ID, "레거시 코드 리팩토링으로 빌드 시간 40% 단축",
              "dive_deep",
              "6년된 레거시 모듈의 빌드 시간이 28분으로 개발 생산성을 저해하고 있었습니다.",
              "코드 품질을 유지하면서 빌드 시간을 15분 이하로 단축하고, 테스트 커버리지를 높여야 했습니다.",
              "의존성 그래프를 분석해 순환 의존성을 제거하고, 멀티 모듈 병렬 빌드로 전환했습니다.",
              "빌드 시간 28분→16분(43% 단축), 테스트 커버리지 41%→68%, 배포 주기 2주→1주 단축." }
        };
        for (Object[] n : notes) {
            jdbc.update("""
                INSERT INTO star_note (user_id, title, lp_tag, situation, task, action, result, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, n[0], n[1], n[2], n[3], n[4], n[5], n[6]);
        }
        log.info("[seed] star_note +{}", notes.length);
    }

    // 11. 회고 기록
    private void seedReviews() {
        if (!isEmpty("review")) { log.info("[seed] review 이미 존재, skip"); return; }
        // (userId, type, period, plans, progress, problems, memo, selfScore)
        Object[][] reviews = {
            { ADMIN_ID, "weekly", "21주차 (5/19~5/25)",
              "분기 SaaS 출시 마무리, 팀 스프린트 회고 진행, 신규 온보딩 1명",
              "SaaS 결제 모듈 배포 완료, 스프린트 회고 진행, 온보딩 자료 업데이트",
              "QA 일정 촉박, 문서 업데이트 누락", "다음 주는 문서화 먼저!", 4 },
            { ADMIN_ID, "weekly", "20주차 (5/12~5/18)",
              "API 레이턴시 개선 마무리, 팀 KPI 리뷰",
              "레이턴시 52% 개선 완료, KPI 리뷰 진행",
              "일부 팀원 스크럼 미작성", "꾸준함이 중요", 5 },
            { TEST_ID, "weekly", "21주차 (5/19~5/25)",
              "신규 고객 미팅 5건, 데모 자료 업데이트",
              "고객 미팅 6건 완료, 데모 자료 개선",
              "미팅 후 후속 조치 지연", "다음 주 CRM 정리부터", 4 },
            { PARK_ID, "weekly", "21주차 (5/19~5/25)",
              "PR 리뷰 10건, 컴포넌트 라이브러리 기여 2건",
              "PR 리뷰 11건, 컴포넌트 2개 추가",
              "리뷰 기준 팀원 간 이견 발생", "기준 문서 보완 필요", 4 },
            { JOH_ID, "weekly", "21주차 (5/19~5/25)",
              "배포 자동화 스크립트 개선, 버그 수정 8건",
              "스크립트 개선 완료, 버그 9건 해결",
              "QA 환경 불안정으로 지연", "QA 서버 점검 요청 예정", 3 },
            { JHLEE_ID, "weekly", "21주차 (5/19~5/25)",
              "고객 문의 50건 처리, FAQ 5건 추가",
              "문의 52건 처리, FAQ 업데이트 완료",
              "일부 복잡한 문의 에스컬레이션 지연", "에스컬레이션 기준 재정비 필요", 4 },
            { KIMNW_ID, "weekly", "21주차 (5/19~5/25)",
              "스프린트 플래닝 주도, 코드리뷰 12건",
              "플래닝 완료, 리뷰 13건, 완료율 92%",
              "태스크 추정 오류 2건", "추정 정확도 개선 방법 논의 필요", 5 },
            { SEUNG_ID, "weekly", "21주차 (5/19~5/25)",
              "레거시 리팩토링 2모듈, 커밋 20건",
              "리팩토링 2모듈 완료, 커밋 22건",
              "테스트 작성 시간 부족", "TDD 습관화 필요", 4 },
            { DEV_ID, "weekly", "21주차 (5/19~5/25)",
              "Android 파이프라인 완성, 푸시 알림 SDK 통합",
              "파이프라인 완성, SDK 통합 90% 진행",
              "SDK 문서 오류로 이슈 발생", "다음 주 마무리 예정", 3 },
            { DES_ID, "monthly", "2026년 05월",
              "디자인 시스템 컴포넌트 10종 추가, 베타 페이지 완성",
              "컴포넌트 12종 완성, 베타 페이지 배포",
              "컴포넌트 명세 문서 미완성", "다음 달: 문서화 + 다크모드 대응", 4 }
        };
        for (Object[] r : reviews) {
            jdbc.update("""
                INSERT INTO review (user_id, type, period, plans, progress, problems, memo, self_score, saved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7]);
        }
        log.info("[seed] review +{}", reviews.length);
    }

    // 9. AI 채팅 히스토리 (admin)
    private void seedAiChatHistory() {
        if (!isEmpty("ai_chat_history")) {
            log.info("[seed] ai_chat_history 이미 존재, skip");
            return;
        }
        jdbc.update("""
            INSERT INTO ai_chat_history (user_id, role, content, created_at)
            VALUES (?, 'user', '이번 주 내 KPI 진행 상황 요약해줘', DATEADD('HOUR', -3, CURRENT_TIMESTAMP))
            """, ADMIN_ID);
        jdbc.update("""
            INSERT INTO ai_chat_history (user_id, role, content, created_at)
            VALUES (?, 'assistant',
            '이번 주 KPI 요약입니다:\n- 월 매출 목표: 약 78% 진행\n- 주간 코드 리뷰: 12/15건 완료\n- 일일 학습: 5/7일 달성\n전반적으로 양호한 페이스입니다.',
            DATEADD('HOUR', -3, CURRENT_TIMESTAMP))
            """, ADMIN_ID);
        jdbc.update("""
            INSERT INTO ai_chat_history (user_id, role, content, created_at)
            VALUES (?, 'user', '최근 블로커들 정리해줘', DATEADD('HOUR', -1, CURRENT_TIMESTAMP))
            """, ADMIN_ID);
        jdbc.update("""
            INSERT INTO ai_chat_history (user_id, role, content, created_at)
            VALUES (?, 'assistant',
            '최근 14일간 등록된 주요 블로커:\n- 결제 API 응답 지연 (HIGH)\n- 디자인 시스템 컴포넌트 부족 (MEDIUM)\n- QA 환경 불안정 (HIGH)\nQA 환경 안정화를 우선 검토하시는 게 좋겠습니다.',
            DATEADD('HOUR', -1, CURRENT_TIMESTAMP))
            """, ADMIN_ID);
        log.info("[seed] ai_chat_history +4");
    }
}
