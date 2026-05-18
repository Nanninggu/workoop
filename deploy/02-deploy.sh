#!/bin/bash
# =============================================================
#  Workoop — 배포 스크립트 (업데이트 시마다 실행)
#  실행: sudo bash 02-deploy.sh
#  옵션: --skip-build   빌드 없이 서비스만 재시작
# =============================================================
set -e

APP_DIR="/home/ubuntu/workoop"
DATA_DIR="/home/ubuntu/workoop-data"
LOG_FILE="/var/log/workoop-deploy.log"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

log()  { echo -e "${CYAN}▶ $1${NC}" | tee -a "$LOG_FILE"; }
ok()   { echo -e "${GREEN}✅ $1${NC}" | tee -a "$LOG_FILE"; }
warn() { echo -e "${YELLOW}⚠️  $1${NC}" | tee -a "$LOG_FILE"; }
err()  { echo -e "${RED}✗ $1${NC}" | tee -a "$LOG_FILE"; exit 1; }

[ "$EUID" -ne 0 ] && err "sudo 로 실행하세요: sudo bash 02-deploy.sh"

echo "" >> "$LOG_FILE"
echo "=== 배포 시작: $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$LOG_FILE"

echo -e "${BOLD}"
echo "╔══════════════════════════════════════════╗"
echo "║    Workoop — EC2 배포                    ║"
echo "╚══════════════════════════════════════════╝"
echo -e "${NC}"

# ─────────────────────────────────────────
# 1. Git Pull
# ─────────────────────────────────────────
log "[1/4] GitHub에서 최신 소스 pull"
cd "$APP_DIR"
git pull origin main
ok "소스 업데이트 완료 ($(git log -1 --format='%h %s'))"

if [ "$1" = "--skip-build" ]; then
  warn "--skip-build 옵션: 빌드 건너뜀"
else
  # ─────────────────────────────────────────
  # 2. 프론트엔드 빌드
  # ─────────────────────────────────────────
  log "[2/4] 프론트엔드 빌드 (Vue → Spring static)"
  cd "$APP_DIR/frontend"
  # ubuntu 유저로 npm 실행 (root에서 npm 빌드 시 권한 문제 방지)
  sudo -u ubuntu npm ci --silent
  sudo -u ubuntu npm run build --silent
  ok "프론트엔드 빌드 완료"

  # ─────────────────────────────────────────
  # 3. 백엔드 빌드
  # ─────────────────────────────────────────
  log "[3/4] 백엔드 JAR 빌드 (Maven)"
  cd "$APP_DIR/backend"
  sudo -u ubuntu mvn package -DskipTests -q
  JAR=$(ls "$APP_DIR/backend/target/"*.jar 2>/dev/null | grep -v original | head -1)
  [ -z "$JAR" ] && err "JAR 빌드 실패: target 폴더에 JAR 없음"
  ok "백엔드 빌드 완료: $(basename $JAR)"
fi

# ─────────────────────────────────────────
# 4. 서비스 재시작
# ─────────────────────────────────────────
log "[4/4] Workoop 서비스 재시작"
systemctl restart workoop
sleep 3

# 헬스체크 (최대 30초 대기)
echo -n "   서버 기동 대기 중"
for i in $(seq 1 30); do
  sleep 1
  echo -n "."
  STATUS=$(systemctl is-active workoop 2>/dev/null)
  if [ "$STATUS" = "active" ]; then
    if curl -sf http://localhost:48080 > /dev/null 2>&1; then
      echo ""
      ok "서버 정상 응답 확인 완료"
      break
    fi
  elif [ "$STATUS" = "failed" ]; then
    echo ""
    err "서비스 실행 실패. 로그: journalctl -u workoop -n 50"
  fi
  if [ $i -eq 30 ]; then
    echo ""
    warn "30초 내 응답 없음. 로그를 확인하세요: journalctl -u workoop -n 50"
  fi
done

# ─────────────────────────────────────────
echo ""
echo -e "${BOLD}════════════════════════════════════════${NC}"
echo -e "${GREEN}  배포 완료! 🚀${NC}"
echo -e "${BOLD}════════════════════════════════════════${NC}"
echo ""
EC2_IP=$(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "<EC2-퍼블릭-IP>")
echo -e "  접속 주소: ${CYAN}http://$EC2_IP${NC}"
echo -e "  서비스 로그: ${YELLOW}journalctl -u workoop -f${NC}"
echo -e "  서비스 상태: ${YELLOW}systemctl status workoop${NC}"
echo ""
