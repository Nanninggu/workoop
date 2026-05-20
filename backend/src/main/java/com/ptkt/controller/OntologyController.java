package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.service.OntologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ontology")
@RequiredArgsConstructor
public class OntologyController {

    private final OntologyService ontologyService;

    /** H2 → TDB2 전체 재동기화 */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<OntologyService.SyncStats>> sync() {
        OntologyService.SyncStats stats = ontologyService.syncAll();
        return ResponseEntity.ok(ApiResponse.ok("동기화 완료", stats));
    }

    /** 트리플 통계 */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(ontologyService.stats()));
    }

    /** SPARQL SELECT 쿼리 */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> query(
            @RequestBody Map<String, String> body) {
        String sparql = body.get("sparql");
        if (sparql == null || sparql.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("sparql 필드가 필요합니다."));
        }
        List<Map<String, String>> results = ontologyService.sparqlSelect(sparql);
        return ResponseEntity.ok(ApiResponse.ok("조회 완료", results));
    }

    /** 리소스 URI로 상세 조회 */
    @GetMapping("/describe")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> describe(
            @RequestParam String uri) {
        List<Map<String, String>> result = ontologyService.describeResource(uri);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 그래프 시각화용 노드+엣지 데이터 (기본) */
    @GetMapping("/graph")
    public ResponseEntity<ApiResponse<Map<String, Object>>> graph(
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(ApiResponse.ok(ontologyService.graphData(filter)));
    }

    /** 역량추론 + 클러스터링 + 협업 네트워크 분석 실행 */
    @PostMapping("/enrich")
    public ResponseEntity<ApiResponse<OntologyService.EnrichStats>> enrich() {
        OntologyService.EnrichStats stats = ontologyService.enrichAll();
        return ResponseEntity.ok(ApiResponse.ok("심화 분석 완료", stats));
    }

    /** 심화 분석 그래프 데이터 (skills / clusters / collaboration) */
    @GetMapping("/graph/enriched")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enrichedGraph(
            @RequestParam(required = false, defaultValue = "skills") String mode) {
        return ResponseEntity.ok(ApiResponse.ok(ontologyService.enrichedGraphData(mode)));
    }
}
