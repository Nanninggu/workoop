#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  Workoop 실행 스크립트
#  사용법:
#    ./run.sh          — 빌드 후 실행 (기본)
#    ./run.sh --dev    — 빌드 없이 최신 JAR로 바로 실행
#    ./run.sh --stop   — 실행 중인 서버 종료
# ─────────────────────────────────────────────────────────────

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="/tmp/coopwork-server.pid"
LOG_FILE="/tmp/coopwork-server.log"
PORT=48080
APP_URL="http://localhost:$PORT"

# ── 색상 ──
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

print_banner() {
  echo ""
  echo -e "${BLUE}${BOLD}╔══════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}${BOLD}║    Workoop — 팀 협업 & KPI 관리 플랫폼   ║${NC}"
  echo -e "${BLUE}${BOLD}╚══════════════════════════════════════════╝${NC}"
  echo ""
}

# ── 기존 서버 종료 ──
stop_server() {
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
      echo -e "${YELLOW}▶ 기존 서버(PID: $PID) 종료 중...${NC}"
      kill "$PID"
      sleep 1
    fi
    rm -f "$PID_FILE"
  fi
  # 포트 점유 프로세스도 정리
  LEFTOVER=$(lsof -ti tcp:$PORT 2>/dev/null)
  if [ -n "$LEFTOVER" ]; then
    kill $LEFTOVER 2>/dev/null
    sleep 1
  fi
}

# ── --stop 옵션 ──
if [ "$1" = "--stop" ]; then
  stop_server
  echo -e "${GREEN}✅ 서버가 종료되었습니다.${NC}"
  exit 0
fi

print_banner

# .env 로드
if [ -f "$APP_DIR/.env" ]; then
  export $(grep -v '^#' "$APP_DIR/.env" | xargs)
fi

stop_server

# ── --dev 아니면 빌드 ──
if [ "$1" != "--dev" ]; then
  # Step 1: 프론트엔드 빌드
  echo -e "${CYAN}▶ [1/2] 프론트엔드 빌드 중...${NC}"
  cd "$APP_DIR/frontend"
  if ! npm run build --silent 2>&1; then
    echo -e "${RED}✗ 프론트엔드 빌드 실패${NC}"
    exit 1
  fi
  echo -e "${GREEN}   ✅ 프론트엔드 빌드 완료${NC}"
  echo ""

  # Step 2: 백엔드 JAR 패키징
  echo -e "${CYAN}▶ [2/2] 백엔드 빌드 중...${NC}"
  cd "$APP_DIR/backend"
  if ! mvn package -DskipTests -q 2>&1; then
    echo -e "${RED}✗ 백엔드 빌드 실패${NC}"
    exit 1
  fi
  echo -e "${GREEN}   ✅ 백엔드 빌드 완료${NC}"
  echo ""
fi

# ── JAR 찾기 ──
JAR=$(ls "$APP_DIR/backend/target/"*.jar 2>/dev/null | grep -v original | head -1)
if [ -z "$JAR" ]; then
  echo -e "${RED}✗ JAR 파일을 찾을 수 없습니다. 먼저 ./run.sh 로 빌드하세요.${NC}"
  exit 1
fi

# ── 서버 실행 ──
echo -e "${CYAN}▶ 서버 시작 중...${NC}"
java -jar "$JAR" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!
echo $SERVER_PID > "$PID_FILE"

# ── 서버 준비 대기 (최대 30초) ──
echo -ne "   대기 중"
for i in $(seq 1 30); do
  sleep 1
  echo -ne "."
  if curl -s "$APP_URL/actuator/health" > /dev/null 2>&1 || \
     curl -s "$APP_URL" > /dev/null 2>&1; then
    break
  fi
  if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo ""
    echo -e "${RED}✗ 서버가 비정상 종료되었습니다. 로그:${NC}"
    tail -20 "$LOG_FILE"
    exit 1
  fi
done
echo ""

# ── 브라우저 열기 ──
echo -e "${GREEN}✅ 서버 실행 완료!${NC}"
echo ""
echo -e "  ${BOLD}주소:${NC}  ${CYAN}${APP_URL}${NC}"
echo -e "  ${BOLD}PID:${NC}   $SERVER_PID"
echo -e "  ${BOLD}로그:${NC}  $LOG_FILE"
echo ""
echo -e "  종료하려면: ${YELLOW}./run.sh --stop${NC}"
echo ""

open "$APP_URL"

# ── 로그 실시간 출력 (Ctrl+C 로 종료 가능) ──
echo -e "${BLUE}────────────── 서버 로그 ──────────────${NC}"
trap "echo ''; echo -e '${YELLOW}로그 출력 종료 (서버는 계속 실행 중)${NC}'; exit 0" INT
tail -f "$LOG_FILE"
