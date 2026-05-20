package com.ptkt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyService {

    private final Dataset          jenaDataset;
    private final JdbcTemplate     jdbc;
    private final ChatClient.Builder chatClientBuilder;

    private static final String NS       = "http://coopwork.io/ontology#";
    private static final String RES_NS   = "http://coopwork.io/resource/";
    private static final String GRAPH    = "http://coopwork.io/graph/main";
    private static final String GRAPH_EN = "http://coopwork.io/graph/enriched";

    @Value("${jena.sync.on-startup:true}")
    private boolean syncOnStartup;

    // ── 기동 시 자동 동기화 ──────────────────────────────────
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (syncOnStartup) {
            log.info("[Ontology] 기동 시 H2→TDB2 동기화 시작");
            syncAll();
        }
    }

    // ── 전체 동기화 ──────────────────────────────────────────
    public SyncStats syncAll() {
        long start = System.currentTimeMillis();
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("cw", NS);

        int users = syncUsers(model);
        int orgs  = syncOrganizations(model);
        int projs = syncProjects(model);
        int tasks = syncTasks(model);
        int kpis  = syncKpis(model);
        int scrums = syncScrums(model);

        // TDB2에 저장 (기존 그래프 교체)
        jenaDataset.begin(org.apache.jena.query.ReadWrite.WRITE);
        try {
            if (jenaDataset.containsNamedModel(GRAPH)) {
                jenaDataset.removeNamedModel(GRAPH);
            }
            jenaDataset.addNamedModel(GRAPH, model);
            jenaDataset.commit();
        } catch (Exception e) {
            jenaDataset.abort();
            throw e;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[Ontology] 동기화 완료: user={}, org={}, project={}, task={}, kpi={}, scrum={} ({}ms)",
                users, orgs, projs, tasks, kpis, scrums, elapsed);
        return new SyncStats(users, orgs, projs, tasks, kpis, scrums, (int) model.size());
    }

    // ── SPARQL SELECT 쿼리 ───────────────────────────────────
    public List<Map<String, String>> sparqlSelect(String sparqlQuery) {
        String prefixed = """
                PREFIX cw:   <http://coopwork.io/ontology#>
                PREFIX res:  <http://coopwork.io/resource/>
                PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>
                """ + sparqlQuery;

        List<Map<String, String>> results = new ArrayList<>();
        jenaDataset.begin(ReadWrite.READ);
        try {
            Model model = jenaDataset.getNamedModel(GRAPH);
            try (QueryExecution qe = QueryExecutionFactory.create(prefixed, model)) {
                ResultSet rs = qe.execSelect();
                List<String> vars = rs.getResultVars();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String var : vars) {
                        RDFNode node = sol.get(var);
                        row.put(var, node != null ? nodeToString(node) : "");
                    }
                    results.add(row);
                }
            }
        } finally {
            jenaDataset.end();
        }
        return results;
    }

    // ── 트리플 통계 ──────────────────────────────────────────
    public Map<String, Object> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        jenaDataset.begin(ReadWrite.READ);
        try {
            Model model = jenaDataset.getNamedModel(GRAPH);
            map.put("totalTriples", model.size());
            map.put("graph", GRAPH);
            // 클래스별 인스턴스 수
            for (String cls : List.of("User","Organization","Project","Task","KPI","ScrumRecord")) {
                long count = model.listSubjectsWithProperty(
                        RDF.type, model.getResource(NS + cls)).toList().size();
                map.put("count" + cls, count);
            }
        } finally {
            jenaDataset.end();
        }
        return map;
    }

    // ── 단일 리소스 조회 ─────────────────────────────────────
    public List<Map<String, String>> describeResource(String resourceUri) {
        String query = "SELECT ?p ?o WHERE { <" + resourceUri + "> ?p ?o }";
        return sparqlSelect(query);
    }

    // ── H2 → RDF 변환 (내부) ─────────────────────────────────

    private int syncUsers(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT u.id, u.name, u.email, ms.org_id, ms.role " +
                "FROM users u LEFT JOIN membership ms ON u.id = ms.user_id");
        Set<Long> seen = new HashSet<>();
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            if (seen.add(id)) {
                Resource user = m.createResource(RES_NS + "user/" + id);
                user.addProperty(RDF.type, m.createResource(NS + "User"))
                    .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id))
                    .addProperty(m.createProperty(NS, "hasName"), str(r.get("NAME")))
                    .addProperty(m.createProperty(NS, "hasEmail"), str(r.get("EMAIL")));
            }
            // 조직 멤버십
            if (r.get("ORG_ID") != null) {
                Resource user = m.createResource(RES_NS + "user/" + id);
                Resource org  = m.createResource(RES_NS + "org/" + toLong(r.get("ORG_ID")));
                user.addProperty(m.createProperty(NS, "memberOf"), org);
                if (r.get("ROLE") != null) {
                    user.addProperty(m.createProperty(NS, "hasRole"), str(r.get("ROLE")));
                }
            }
        }
        return seen.size();
    }

    private int syncOrganizations(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name FROM organization");
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            m.createResource(RES_NS + "org/" + id)
             .addProperty(RDF.type, m.createResource(NS + "Organization"))
             .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id))
             .addProperty(m.createProperty(NS, "hasName"), str(r.get("NAME")));
        }
        return rows.size();
    }

    private int syncProjects(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name, org_id, description FROM project");
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            Resource proj = m.createResource(RES_NS + "project/" + id)
                .addProperty(RDF.type, m.createResource(NS + "Project"))
                .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id))
                .addProperty(m.createProperty(NS, "hasName"), str(r.get("NAME")));
            if (r.get("ORG_ID") != null) {
                Resource org = m.createResource(RES_NS + "org/" + toLong(r.get("ORG_ID")));
                org.addProperty(m.createProperty(NS, "ownsProject"), proj);
            }
            if (r.get("DESCRIPTION") != null) {
                proj.addProperty(m.createProperty(NS, "hasDescription"), str(r.get("DESCRIPTION")));
            }
        }
        return rows.size();
    }

    private int syncTasks(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.id, t.title, t.status, t.priority, t.due_date, " +
                "t.assignee_id, t.project_id, t.kpi_id " +
                "FROM task t");
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            Resource task = m.createResource(RES_NS + "task/" + id)
                .addProperty(RDF.type, m.createResource(NS + "Task"))
                .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id))
                .addProperty(m.createProperty(NS, "hasTitle"), str(r.get("TITLE")))
                .addProperty(m.createProperty(NS, "hasStatus"), str(r.get("STATUS")));
            if (r.get("PRIORITY") != null)
                task.addProperty(m.createProperty(NS, "hasPriority"), str(r.get("PRIORITY")));
            if (r.get("DUE_DATE") != null)
                task.addProperty(m.createProperty(NS, "hasDueDate"), str(r.get("DUE_DATE")));
            if (r.get("ASSIGNEE_ID") != null)
                task.addProperty(m.createProperty(NS, "assignedTo"),
                        m.createResource(RES_NS + "user/" + toLong(r.get("ASSIGNEE_ID"))));
            if (r.get("PROJECT_ID") != null)
                task.addProperty(m.createProperty(NS, "belongsToProject"),
                        m.createResource(RES_NS + "project/" + toLong(r.get("PROJECT_ID"))));
            if (r.get("KPI_ID") != null)
                task.addProperty(m.createProperty(NS, "linkedKPI"),
                        m.createResource(RES_NS + "kpi/" + toLong(r.get("KPI_ID"))));
        }
        return rows.size();
    }

    private int syncKpis(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name, kpi_type, target_value, unit, status, owner_id FROM kpi");
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            Resource kpi = m.createResource(RES_NS + "kpi/" + id)
                .addProperty(RDF.type, m.createResource(NS + "KPI"))
                .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id))
                .addProperty(m.createProperty(NS, "hasName"), str(r.get("NAME")))
                .addProperty(m.createProperty(NS, "hasStatus"), str(r.get("STATUS")));
            if (r.get("KPI_TYPE") != null)
                kpi.addProperty(m.createProperty(NS, "hasKpiType"), str(r.get("KPI_TYPE")));
            if (r.get("TARGET_VALUE") != null)
                kpi.addProperty(m.createProperty(NS, "hasTargetValue"), str(r.get("TARGET_VALUE")));
            if (r.get("UNIT") != null)
                kpi.addProperty(m.createProperty(NS, "hasUnit"), str(r.get("UNIT")));
            if (r.get("OWNER_ID") != null) {
                Resource owner = m.createResource(RES_NS + "user/" + toLong(r.get("OWNER_ID")));
                owner.addProperty(m.createProperty(NS, "ownsKPI"), kpi);
            }
        }
        return rows.size();
    }

    private int syncScrums(Model m) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, user_id, scrum_date, blocker, energy, focus FROM scrum " +
                "WHERE scrum_date >= DATEADD(DAY, -30, CURRENT_DATE)");
        for (Map<String, Object> r : rows) {
            long id = toLong(r.get("ID"));
            Resource scrum = m.createResource(RES_NS + "scrum/" + id)
                .addProperty(RDF.type, m.createResource(NS + "ScrumRecord"))
                .addProperty(m.createProperty(NS, "hasId"), m.createTypedLiteral(id));
            if (r.get("SCRUM_DATE") != null)
                scrum.addProperty(m.createProperty(NS, "hasScrumDate"), str(r.get("SCRUM_DATE")));
            if (r.get("BLOCKER") != null)
                scrum.addProperty(m.createProperty(NS, "hasBlockerText"), str(r.get("BLOCKER")));
            if (r.get("ENERGY") != null)
                scrum.addProperty(m.createProperty(NS, "hasEnergy"), str(r.get("ENERGY")));
            if (r.get("FOCUS") != null)
                scrum.addProperty(m.createProperty(NS, "hasFocus"), str(r.get("FOCUS")));
            if (r.get("USER_ID") != null) {
                Resource user = m.createResource(RES_NS + "user/" + toLong(r.get("USER_ID")));
                user.addProperty(m.createProperty(NS, "hasScrum"), scrum);
            }
        }
        return rows.size();
    }

    // ── 유틸 ──────────────────────────────────────────────────
    private long toLong(Object v) {
        if (v == null) return 0L;
        return ((Number) v).longValue();
    }

    private String str(Object v) {
        return v == null ? "" : v.toString();
    }

    private String nodeToString(RDFNode node) {
        if (node.isLiteral()) return node.asLiteral().getString();
        if (node.isURIResource()) {
            String uri = node.asResource().getURI();
            return uri.startsWith(RES_NS) ? uri.substring(RES_NS.length()) : uri;
        }
        return node.toString();
    }

    // ── 그래프 시각화용 노드+엣지 반환 ──────────────────────────────
    public Map<String, Object> graphData(String filter) {
        // 포함할 클래스 결정
        List<String> classes = switch (filter == null ? "all" : filter) {
            case "task"    -> List.of("Task", "User", "Project");
            case "kpi"     -> List.of("KPI", "User");
            case "org"     -> List.of("Organization", "User", "Project");
            default        -> List.of("User", "Organization", "Project", "Task", "KPI");
        };

        String classFilter = classes.stream()
            .map(c -> "cw:" + c)
            .reduce((a, b) -> a + ", " + b).orElse("cw:User");

        // 노드 조회
        String nodeQuery = """
            SELECT DISTINCT ?uri ?name ?type WHERE {
              ?uri rdf:type ?type .
              FILTER(?type IN (%s))
              OPTIONAL { ?uri cw:hasName ?name }
              OPTIONAL { ?uri cw:hasTitle ?name }
            } LIMIT 150
            """.formatted(classFilter);

        // 엣지 조회 (URI 간 관계만)
        String edgeQuery = """
            SELECT DISTINCT ?src ?rel ?tgt WHERE {
              ?src rdf:type ?sType . FILTER(?sType IN (%s))
              ?tgt rdf:type ?tType . FILTER(?tType IN (%s))
              ?src ?rel ?tgt .
              FILTER(?rel != rdf:type)
            } LIMIT 300
            """.formatted(classFilter, classFilter);

        List<Map<String, String>> nodeRows = sparqlSelect(nodeQuery);
        List<Map<String, String>> edgeRows = sparqlSelect(edgeQuery);

        // ECharts graph format 변환
        List<Map<String, Object>> nodes = nodeRows.stream().map(r -> {
            Map<String, Object> n = new LinkedHashMap<>();
            String uri = r.getOrDefault("uri", "");
            String type = r.getOrDefault("type", "").replace(NS, "");
            String name = r.getOrDefault("name", uri.contains("/") ? uri.substring(uri.lastIndexOf('/') + 1) : uri);
            n.put("id", uri);
            n.put("name", name.length() > 15 ? name.substring(0, 14) + "…" : name);
            n.put("fullName", name);
            n.put("category", type);
            n.put("symbolSize", symbolSize(type));
            return n;
        }).toList();

        Set<String> nodeIds = nodes.stream()
            .map(n -> (String) n.get("id")).collect(java.util.stream.Collectors.toSet());

        List<Map<String, Object>> edges = edgeRows.stream()
            .filter(r -> nodeIds.contains(r.get("src")) && nodeIds.contains(r.get("tgt")))
            .map(r -> {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("source", r.get("src"));
                e.put("target", r.get("tgt"));
                e.put("label",  r.getOrDefault("rel", "").replace(NS, ""));
                return e;
            }).toList();

        List<Map<String, String>> categories = List.of(
            Map.of("name", "User"),
            Map.of("name", "Organization"),
            Map.of("name", "Project"),
            Map.of("name", "Task"),
            Map.of("name", "KPI")
        );

        return Map.of("nodes", nodes, "edges", edges, "categories", categories);
    }

    private int symbolSize(String type) {
        return switch (type) {
            case "Organization" -> 50;
            case "User"         -> 38;
            case "Project"      -> 34;
            case "KPI"          -> 26;
            default             -> 22;   // Task
        };
    }

    // ════════════════════════════════════════════════════════════════════
    //  지식 그래프 심화 분석 (역량추론 / 클러스터링 / 협업 네트워크)
    // ════════════════════════════════════════════════════════════════════

    public EnrichStats enrichAll() {
        Model enriched = ModelFactory.createDefaultModel();
        enriched.setNsPrefix("cw", NS);

        int skills  = inferSkills(enriched);
        int clusters = clusterTasks(enriched);
        int collabs = buildCollabNetwork(enriched);

        jenaDataset.begin(ReadWrite.WRITE);
        try {
            if (jenaDataset.containsNamedModel(GRAPH_EN))
                jenaDataset.removeNamedModel(GRAPH_EN);
            jenaDataset.addNamedModel(GRAPH_EN, enriched);
            jenaDataset.commit();
        } catch (Exception e) {
            jenaDataset.abort();
            throw e;
        }
        log.info("[Ontology] 심화 분석 완료: skills={}, clusters={}, collabs={}", skills, clusters, collabs);
        return new EnrichStats(skills, clusters, collabs);
    }

    // ── ① 역량 추론: STAR 노트 → AI 스킬 태그 ────────────────────
    private int inferSkills(Model m) {
        List<Map<String, Object>> notes = jdbc.queryForList(
            "SELECT user_id, title, action, result FROM star_note");
        if (notes.isEmpty()) return 0;

        // 사용자별 그룹화
        Map<Long, List<Map<String, Object>>> byUser = new LinkedHashMap<>();
        for (Map<String, Object> n : notes) {
            long uid = toLong(n.get("USER_ID"));
            byUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(n);
        }

        int total = 0;
        for (Map.Entry<Long, List<Map<String, Object>>> entry : byUser.entrySet()) {
            long uid = entry.getKey();
            StringBuilder text = new StringBuilder();
            for (Map<String, Object> n : entry.getValue()) {
                text.append(str(n.get("TITLE"))).append(". ");
                text.append(str(n.get("ACTION"))).append(". ");
                text.append(str(n.get("RESULT"))).append("\n");
            }
            String snippet = text.length() > 800 ? text.substring(0, 800) : text.toString();
            try {
                String prompt = """
                        아래는 한 팀원의 STAR 업무 경험 기록입니다.
                        이 경험에서 나타나는 핵심 스킬/역량을 3~5개 추출하세요.
                        JSON 배열로만 응답 (다른 텍스트 없이):
                        ["스킬1","스킬2","스킬3"]
                        스킬은 짧고 명확하게(예:"API 설계","데이터 분석","팀 리더십","UI/UX","문서화")

                        [경험 기록]
                        %s
                        """.formatted(snippet);

                String res = chatClientBuilder.build().prompt(prompt).call().content().trim();
                int s = res.indexOf('['), e = res.lastIndexOf(']');
                if (s < 0 || e <= s) continue;

                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> skills = om.readValue(res.substring(s, e + 1),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Resource user = m.createResource(RES_NS + "user/" + uid);
                for (String skill : skills) {
                    if (skill == null || skill.isBlank()) continue;
                    Resource skillNode = m.createResource(RES_NS + "skill/" + skill.replace(" ", "_").replace("/", "-"));
                    skillNode.addProperty(RDF.type, m.createResource(NS + "Skill"))
                             .addProperty(m.createProperty(NS, "hasName"), skill);
                    user.addProperty(m.createProperty(NS, "hasSkill"), skillNode);
                    total++;
                }
            } catch (Exception ex) {
                log.warn("[Ontology] 스킬 추론 실패 uid={}: {}", uid, ex.getMessage());
            }
        }
        return total;
    }

    // ── ② 태스크 클러스터링: 키워드 기반 ──────────────────────────
    private int clusterTasks(Model m) {
        List<Map<String, Object>> tasks = jdbc.queryForList("SELECT id, title FROM task");
        for (Map<String, Object> t : tasks) {
            String cluster = detectCluster(str(t.get("TITLE")));
            m.createResource(RES_NS + "task/" + toLong(t.get("ID")))
             .addProperty(m.createProperty(NS, "taskCluster"), cluster);
        }
        return tasks.size();
    }

    private String detectCluster(String title) {
        String t = title.toLowerCase();
        if (anyMatch(t, "구현","개발","api","sdk","코드","빌드","모듈","컴포넌트","백엔드","프론트"))
            return "개발";
        if (anyMatch(t, "ui","ux","디자인","화면","레이아웃","스타일","색상","아이콘"))
            return "디자인";
        if (anyMatch(t, "테스트","qa","검증","버그","오류","수정","fix"))
            return "테스트/QA";
        if (anyMatch(t, "회의","미팅","발표","데모","보고","커뮤니케이션","리뷰"))
            return "커뮤니케이션";
        if (anyMatch(t, "문서","docs","정리","작성","가이드","매뉴얼","위키"))
            return "문서화";
        if (anyMatch(t, "분석","기획","요구","스펙","설계","아키텍처","erd"))
            return "기획/분석";
        if (anyMatch(t, "배포","인프라","서버","ec2","docker","ci","cd","빌드파이프"))
            return "인프라/DevOps";
        if (anyMatch(t, "kpi","목표","성과","달성","지표","측정"))
            return "성과관리";
        return "기타";
    }

    private boolean anyMatch(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    // ── ③ 협업 네트워크: 공유 프로젝트 + 태스크 기반 ────────────
    private int buildCollabNetwork(Model m) {
        // 같은 프로젝트에 태스크가 있는 사용자 쌍 + 강도 계산
        List<Map<String, Object>> pairs = jdbc.queryForList("""
            SELECT t1.assignee_id AS uid1, t2.assignee_id AS uid2,
                   COUNT(DISTINCT t1.project_id) AS shared_projects,
                   COUNT(*) AS shared_tasks
            FROM task t1
            JOIN task t2 ON t1.project_id = t2.project_id
              AND t1.assignee_id < t2.assignee_id
            WHERE t1.assignee_id IS NOT NULL AND t2.assignee_id IS NOT NULL
            GROUP BY t1.assignee_id, t2.assignee_id
            """);

        Property workedWith = m.createProperty(NS, "collaboratedWith");
        Property strength   = m.createProperty(NS, "collaborationStrength");

        for (Map<String, Object> p : pairs) {
            long uid1 = toLong(p.get("UID1")), uid2 = toLong(p.get("UID2"));
            long score = toLong(p.get("SHARED_PROJECTS")) * 3 + toLong(p.get("SHARED_TASKS"));
            Resource u1 = m.createResource(RES_NS + "user/" + uid1);
            Resource u2 = m.createResource(RES_NS + "user/" + uid2);
            u1.addProperty(workedWith, u2);
            u1.addProperty(strength,   String.valueOf(score));
        }
        return pairs.size();
    }

    // ── 심화 분석 그래프 데이터 ───────────────────────────────────
    public Map<String, Object> enrichedGraphData(String mode) {
        return switch (mode == null ? "skills" : mode) {
            case "clusters"     -> clusterGraphData();
            case "collaboration"-> collabGraphData();
            default             -> skillGraphData();
        };
    }

    private Map<String, Object> skillGraphData() {
        String q = """
            SELECT ?userUri ?userName ?skillUri ?skillName WHERE {
              GRAPH <%s> {
                ?userUri cw:hasSkill ?skillUri .
                ?skillUri cw:hasName ?skillName .
              }
              GRAPH <%s> {
                ?userUri cw:hasName ?userName .
              }
            }""".formatted(GRAPH_EN, GRAPH);

        List<Map<String, String>> rows = sparqlUnion(q);
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (Map<String, String> r : rows) {
            String uid = r.get("userUri"), uName = r.get("userName");
            String sid = r.get("skillUri"), sName = r.get("skillName");
            if (uid == null || sid == null) continue;

            if (seen.add(uid)) nodes.add(Map.of(
                "id", uid, "name", uName != null ? uName : uid, "fullName", uName != null ? uName : uid,
                "category", "User", "symbolSize", 38));
            if (seen.add(sid)) nodes.add(Map.of(
                "id", sid, "name", sName != null ? sName : sid, "fullName", sName != null ? sName : sid,
                "category", "Skill", "symbolSize", 24, "symbol", "diamond"));
            edges.add(Map.of("source", uid, "target", sid, "label", "hasSkill"));
        }
        return Map.of("nodes", nodes, "edges", edges,
            "categories", List.of(Map.of("name","User"), Map.of("name","Skill")));
    }

    private Map<String, Object> clusterGraphData() {
        String nodeQ = """
            SELECT ?uri ?title ?cluster WHERE {
              GRAPH <%s> { ?uri cw:taskCluster ?cluster . }
              GRAPH <%s> { ?uri cw:hasTitle ?title . }
            }""".formatted(GRAPH_EN, GRAPH);

        List<Map<String, String>> rows = sparqlUnion(nodeQ);
        List<Map<String, Object>> nodes = new ArrayList<>();
        Set<String> clusterSet = new LinkedHashSet<>();

        for (Map<String, String> r : rows) {
            String uri = r.get("uri"), title = r.get("title"), cluster = r.get("cluster");
            if (uri == null) continue;
            clusterSet.add(cluster != null ? cluster : "기타");
            String label = title != null && title.length() > 12 ? title.substring(0, 11) + "…" : title;
            nodes.add(Map.of(
                "id", uri, "name", label != null ? label : "", "fullName", title != null ? title : "",
                "category", cluster != null ? cluster : "기타", "symbolSize", 22));
        }
        List<Map<String, String>> cats = clusterSet.stream()
            .map(c -> Map.of("name", c)).collect(Collectors.toList());
        return Map.of("nodes", nodes, "edges", List.of(), "categories", cats);
    }

    private Map<String, Object> collabGraphData() {
        String edgeQ = """
            SELECT ?u1 ?u2 ?strength WHERE {
              GRAPH <%s> {
                ?u1 cw:collaboratedWith ?u2 .
                ?u1 cw:collaborationStrength ?strength .
              }
            }""".formatted(GRAPH_EN);

        String nodeQ = """
            SELECT ?uri ?name WHERE {
              GRAPH <%s> { ?uri rdf:type cw:User . ?uri cw:hasName ?name . }
            }""".formatted(GRAPH);

        List<Map<String, String>> edgeRows = sparqlUnion(edgeQ);
        List<Map<String, String>> nodeRows = sparqlUnion(nodeQ);

        List<Map<String, Object>> nodes = nodeRows.stream().map(r -> {
            String name = r.getOrDefault("name", "");
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", r.getOrDefault("uri", ""));
            n.put("name", name);
            n.put("fullName", name);
            n.put("category", "User");
            n.put("symbolSize", 40);
            return n;
        }).collect(Collectors.toList());

        List<Map<String, Object>> edges = edgeRows.stream().map(r -> {
            long s = 0;
            try { s = Long.parseLong(r.getOrDefault("strength", "1")); } catch (Exception ignored) {}
            int w = (int) Math.min(8, Math.max(1, s));
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("source", r.getOrDefault("u1", ""));
            e.put("target", r.getOrDefault("u2", ""));
            e.put("label", "협업 " + s + "점");
            e.put("lineWidth", w);
            return e;
        }).collect(Collectors.toList());

        return Map.of("nodes", nodes, "edges", edges,
            "categories", List.of(Map.of("name","User")));
    }

    // 두 그래프에 걸친 SPARQL — FROM NAMED 대신 직접 Model 합성
    private List<Map<String, String>> sparqlUnion(String query) {
        String prefixed = "PREFIX cw: <" + NS + "> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                + "PREFIX res: <" + RES_NS + "> " + query;
        List<Map<String, String>> results = new ArrayList<>();
        jenaDataset.begin(ReadWrite.READ);
        try (QueryExecution qe = QueryExecutionFactory.create(prefixed, jenaDataset)) {
            ResultSet rs = qe.execSelect();
            List<String> vars = rs.getResultVars();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                Map<String, String> row = new LinkedHashMap<>();
                for (String v : vars) {
                    RDFNode node = sol.get(v);
                    row.put(v, node != null ? nodeToString(node) : null);
                }
                results.add(row);
            }
        } finally {
            jenaDataset.end();
        }
        return results;
    }

    public record SyncStats(int users, int orgs, int projects, int tasks,
                            int kpis, int scrums, int totalTriples) {}

    public record EnrichStats(int skills, int tasksClustered, int collabPairs) {}
}
