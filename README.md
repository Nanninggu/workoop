# Workoop - 팀 협업 & KPI 관리 플랫폼

KPI 연동 칸반, 데일리 스크럼, 번아웃 감지까지 팀 협업을 한 곳에서 관리하는 플랫폼입니다.

## 기술 스택

### Backend
- **Spring Boot 3.2** (Java 17)
- **MyBatis 3** - SQL Mapper
- **H2 Database** - 로컬 파일 DB (`./data/ptkt-db`)
- **Layered Architecture** + **MVC Pattern**

### Frontend
- **Vue.js 3** (Composition API)
- **Vite 5** - 빌드 도구
- **Pinia** - 상태 관리
- **Element Plus** - UI 컴포넌트
- **ECharts** - 차트
- **Tailwind CSS** - 유틸리티 CSS

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **대시보드** | 오늘의 KPI 현황, 카테고리별 달성률, 통계 요약 |
| **KPI 관리** | KPI 생성/수정/삭제, 상태 관리 (활성/일시정지/완료) |
| **실적 입력** | 날짜별 실적 입력, 달성률 실시간 미리보기 |
| **KPI 상세** | 기간별 추이 차트, 기록 내역 조회/삭제 |
| **카테고리** | 카테고리 CRUD, 색상 지정 |

### KPI 타입
- **수치 (NUMERIC)**: 걸음 수, 시간, 금액 등 숫자로 측정
- **퍼센트 (PERCENTAGE)**: 0~100% 범위의 달성률
- **달성 여부 (BOOLEAN)**: 했다/안했다 이진 타입

### 측정 주기
- 매일 (DAILY) / 매주 (WEEKLY) / 매월 (MONTHLY)

---

## 프로젝트 구조

```
Personal-KPI-ManagementTool/
├── backend/                       # Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ptkt/
│       │   ├── config/            # CORS, GlobalExceptionHandler
│       │   ├── controller/        # REST API (MVC - Controller)
│       │   ├── service/           # 비즈니스 로직 (MVC - Model)
│       │   ├── mapper/            # MyBatis Mapper Interface
│       │   ├── model/             # Domain Model (Category, Kpi, KpiRecord)
│       │   └── dto/               # Data Transfer Object
│       └── resources/
│           ├── application.yml
│           ├── schema.sql         # DDL (자동 실행)
│           ├── data.sql           # 초기 데이터
│           └── mapper/            # MyBatis XML Mapper
└── frontend/                      # Vue.js
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/                   # Axios API 클라이언트
        ├── router/                # Vue Router
        ├── store/                 # Pinia Store
        ├── views/                 # 페이지 컴포넌트
        └── components/            # 재사용 컴포넌트
```

---

## 실행 방법

### 사전 요구사항
- Java 17+
- Node.js 18+
- Maven 3.6+

### 1. Backend 실행

```bash
cd backend

# Maven으로 빌드 및 실행
./mvnw spring-boot:run
# 또는
mvn spring-boot:run
```

백엔드 서버: http://localhost:8080  
H2 콘솔: http://localhost:8080/h2-console  
- JDBC URL: `jdbc:h2:file:./data/ptkt-db`
- Username: `sa` / Password: (없음)

### 2. Frontend 실행

```bash
cd frontend

# 패키지 설치
npm install

# 개발 서버 실행
npm run dev
```

프론트엔드: http://localhost:5173

> Vite 프록시 설정으로 `/api` 요청이 자동으로 백엔드(8080)로 전달됩니다.

---

## API 명세

### 대시보드
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/dashboard` | 전체 대시보드 데이터 |

### 카테고리
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/categories` | 전체 카테고리 |
| POST | `/api/categories` | 카테고리 생성 |
| PUT | `/api/categories/{id}` | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | 카테고리 삭제 |

### KPI
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/kpis` | KPI 목록 (status, categoryId 필터) |
| GET | `/api/kpis/{id}` | KPI 상세 |
| POST | `/api/kpis` | KPI 생성 |
| PUT | `/api/kpis/{id}` | KPI 수정 |
| PATCH | `/api/kpis/{id}/status` | 상태 변경 |
| DELETE | `/api/kpis/{id}` | KPI 삭제 |

### 실적 기록
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/records?date=YYYY-MM-DD` | 날짜별 기록 |
| GET | `/api/records/kpi/{id}?startDate=&endDate=` | KPI별 기록 |
| POST | `/api/records` | 기록 저장 (upsert) |
| DELETE | `/api/records/{id}` | 기록 삭제 |

---

## DB 구조

```
category
  id, name, color, icon, description, sort_order, created_at, updated_at

kpi
  id, category_id (FK), name, description, unit
  kpi_type (NUMERIC|PERCENTAGE|BOOLEAN)
  target_value, frequency (DAILY|WEEKLY|MONTHLY)
  start_date, end_date, status (ACTIVE|PAUSED|COMPLETED)
  sort_order, created_at, updated_at

kpi_record
  id, kpi_id (FK), actual_value, boolean_value
  recorded_date (UNIQUE per kpi), note, created_at
```
