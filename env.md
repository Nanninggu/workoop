# 환경 설정 (포트 구성)

## 포트 요약

| 구분 | 포트 | 설명 |
|---|---|---|
| 백엔드 (Spring Boot) | **48080** | 실제 서버 실행 포트 |
| 프론트엔드 개발 서버 (Vite) | **51300** | `npm run dev` 시 접속 주소 |

---

## 백엔드

- **파일**: `backend/src/main/resources/application.yml`
- **포트**: `48080`
- **접속 주소**: `http://localhost:48080`

```yaml
server:
  port: 48080
```

---

## 프론트엔드

- **파일**: `frontend/vite.config.js`
- **개발 서버 포트**: `51300`
- **접속 주소**: `http://localhost:51300`
- **API proxy**: `/api` 요청을 `http://localhost:48080` 으로 전달

```js
server: {
  port: 51300,
  proxy: {
    '/api': {
      target: 'http://localhost:48080',
      changeOrigin: true
    }
  }
}
```

---

## 스크립트별 포트 참조

| 스크립트 | 참조 포트 | 역할 |
|---|---|---|
| `run.sh` | 48080 | 서버 헬스체크 및 브라우저 오픈 |
| `create-app.sh` | 48080 | macOS 앱 번들 실행 포트 |
| `create-app.sh` | 58083 | 로딩 화면용 임시 HTTP 서버 포트 |

---

## 주의사항

- 포트 번호는 **0~65535** 범위 내여야 합니다.
- 포트를 변경할 경우 아래 파일을 모두 함께 수정해야 합니다.
  - `backend/src/main/resources/application.yml`
  - `frontend/vite.config.js`
  - `run.sh`
  - `create-app.sh`
