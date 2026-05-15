# Workoop 로드맵

## 현재 배포 전략: 설치형 팀 서버

프론트엔드가 Spring Boot에서 함께 서빙되므로 CORS 이슈 없음 (같은 오리진).
별도 코드 수정 없이 팀원 접속 가능.

---

## 해커톤 당일 체크리스트

### 서버 역할 PC에서

```bash
# 1. 내 IP 확인
ipconfig getifaddr en0

# 2. 서버 실행
./run.sh --dev

# 3. 팀원에게 주소 공유
# → http://192.168.x.x:48080
```

### macOS 방화벽 확인 (막혀있으면)

```
시스템 설정 → 네트워크 → 방화벽 → 48080 포트 허용
```

---

## 개선 로드맵

```
[Phase 0 - 해커톤]       [Phase 1 - 팀 정착]            [Phase 2 - 진짜 협업툴]
설치형 팀 서버            +댓글 +초대 +번아웃서버저장       H2 → PostgreSQL(Supabase)
코드 수정 없음            이전 개선 리스트 순차 적용         외부망 접근 가능
```

### Phase 1 개선 항목 (우선순위 순)

1. **태스크 댓글** — `task_comment` 테이블, WebSocket 실시간 브로드캐스트
2. **팀원 초대** — `invitations` 테이블, 초대 링크 생성/수락 API
3. **번아웃 데이터 서버 저장** — `daily_checkin` 테이블, localStorage → API 전환
4. **파일 첨부** — `task_attachment` 테이블, 로컬 스토리지 저장

### Phase 2 개선 항목

- H2 → PostgreSQL (Supabase 무료 티어) 교체
- 로컬 앱 유지 + 클라우드 DB로 팀 전체 실시간 공유
- 외부망(인터넷) 접근 가능

---

## 장기 배포 구조 (Phase 2 목표)

```
[팀원 A 로컬 .app]  ─────────────────────┐
[팀원 B 로컬 .app]  ──→  [Supabase PostgreSQL (무료)]
[팀원 C 브라우저]   ─────────────────────┘
```

- 앱은 설치형 유지 (빠른 실행, 네이티브 경험)
- 데이터는 클라우드 DB에서 팀 전체 공유
- 충돌 해결 불필요 (DB가 single source of truth)
