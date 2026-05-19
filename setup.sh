#!/bin/bash
# ══════════════════════════════════════════════════════════════
#  Workoop — EC2 원클릭 설치 스크립트
#
#  사용법:
#    bash setup.sh                      (대화형: Git URL 직접 입력)
#    bash setup.sh https://github.com/…  (URL 인자로 전달)
#
#  재실행 안전: 이미 설치된 경우 업데이트로 동작
# ══════════════════════════════════════════════════════════════
set -e

# ── 색상 ──────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

ok()   { echo -e "${GREEN}[✔]${NC} $1"; }
info() { echo -e "${CYAN}[→]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✘]${NC} $1"; exit 1; }
header() {
  echo ""
  echo -e "${BLUE}${BOLD}══════════════════════════════════════${NC}"
  echo -e "${BLUE}${BOLD}  $1${NC}"
  echo -e "${BLUE}${BOLD}══════════════════════════════════════${NC}"
  echo ""
}

# ── 상수 ──────────────────────────────────────────────────────
INSTALL_DIR="/home/ubuntu/workoop"
DATA_DIR="/home/ubuntu/workoop-data"
ENV_FILE="/etc/workoop.env"
SERVICE_FILE="/etc/systemd/system/workoop.service"
JAR_NAME="ptkt-backend-1.0.0.jar"

# ══════════════════════════════════════════════════════════════
header "Workoop 설치 시작"
# ══════════════════════════════════════════════════════════════

# ── STEP 1: Git 저장소 URL ────────────────────────────────────
REPO_URL="${1:-}"
if [ -z "$REPO_URL" ]; then
  echo -e "${BOLD}Git 저장소 URL을 입력하세요:${NC}"
  echo -e "  (예: https://github.com/yourname/Coop-Application.git)"
  read -rp "  → " REPO_URL
  [ -z "$REPO_URL" ] && err "URL을 입력해야 합니다."
fi

# ── STEP 2: Clone 또는 Pull ───────────────────────────────────
header "1/6  소스 코드"
if [ -d "$INSTALL_DIR/.git" ]; then
  info "이미 설치된 저장소가 있습니다. 최신 코드로 업데이트합니다."
  git -C "$INSTALL_DIR" fetch origin
  git -C "$INSTALL_DIR" reset --hard origin/main
  ok "코드 업데이트 완료"
else
  info "저장소를 클론합니다: $REPO_URL"
  git clone "$REPO_URL" "$INSTALL_DIR"
  ok "클론 완료 → $INSTALL_DIR"
fi

# ── STEP 3: 환경변수 설정 ─────────────────────────────────────
header "2/6  환경변수 설정"

prompt_env() {
  local KEY="$1"
  local DESC="$2"
  local CURRENT
  CURRENT=$(grep "^${KEY}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2- || true)

  if [ -n "$CURRENT" ]; then
    echo -e "  ${BOLD}${KEY}${NC} (현재값 있음 — Enter로 유지)"
  else
    echo -e "  ${BOLD}${KEY}${NC} — ${DESC}"
  fi
  read -rp "  → " VAL
  if [ -n "$VAL" ]; then
    echo "$VAL"
  else
    echo "$CURRENT"
  fi
}

if [ -f "$ENV_FILE" ]; then
  warn "/etc/workoop.env 가 이미 존재합니다. 값을 수정하려면 Enter 대신 새 값을 입력하세요."
else
  info "환경변수를 입력합니다. (각 항목 입력 후 Enter)"
fi

echo ""
GROQ_API_KEY=$(prompt_env   "GROQ_API_KEY"    "Groq API 키 (console.groq.com)")
JWT_SECRET=$(prompt_env     "JWT_SECRET"      "JWT 서명 키 (최소 32자 이상 아무 문자열)")
SLACK_BOT_TOKEN=$(prompt_env "SLACK_BOT_TOKEN" "Slack Bot Token (xoxb-... / 미사용이면 Enter)")
SLACK_CHANNEL_ID=$(prompt_env "SLACK_CHANNEL_ID" "Slack Channel ID (C로 시작 / 미사용이면 Enter)")

# JWT_SECRET 미입력 시 랜덤 생성
if [ -z "$JWT_SECRET" ]; then
  JWT_SECRET=$(tr -dc 'A-Za-z0-9!@#$%^&*' </dev/urandom | head -c 64 || true)
  warn "JWT_SECRET가 비어 있어 자동 생성했습니다."
fi

sudo tee "$ENV_FILE" > /dev/null <<EOF
GROQ_API_KEY=${GROQ_API_KEY}
JWT_SECRET=${JWT_SECRET}
SLACK_BOT_TOKEN=${SLACK_BOT_TOKEN:-disabled}
SLACK_CHANNEL_ID=${SLACK_CHANNEL_ID:-disabled}
EOF
sudo chmod 600 "$ENV_FILE"
ok "환경변수 저장 완료: $ENV_FILE"

# ── STEP 4: Java 17 + Maven ───────────────────────────────────
header "3/6  Java 17 + Maven"

if java -version 2>&1 | grep -qE '"(17|21)'; then
  ok "Java 이미 설치됨: $(java -version 2>&1 | head -1)"
else
  info "Java 17 설치 중..."
  sudo apt-get update -qq
  sudo apt-get install -y openjdk-17-jdk
  ok "Java 17 설치 완료"
fi

if command -v mvn &>/dev/null; then
  ok "Maven 이미 설치됨: $(mvn -version 2>&1 | head -1)"
else
  info "Maven 설치 중..."
  sudo apt-get install -y maven
  ok "Maven 설치 완료"
fi

# ── STEP 5: 디렉토리 + 빌드 ──────────────────────────────────
header "4/6  빌드"

mkdir -p "$DATA_DIR"
ok "데이터 디렉토리: $DATA_DIR"

info "백엔드 빌드 중... (1~2분 소요)"
cd "$INSTALL_DIR/backend"
mvn package -DskipTests -q
ok "빌드 완료: target/$JAR_NAME"

# ── STEP 6: systemd 서비스 ────────────────────────────────────
header "5/6  서비스 등록"

sudo cp "$INSTALL_DIR/deploy/workoop.service" "$SERVICE_FILE"
sudo systemctl daemon-reload
sudo systemctl enable workoop
ok "systemd 서비스 등록 완료 (재부팅 시 자동 시작)"

# ── STEP 7: 서비스 시작 ───────────────────────────────────────
header "6/6  서비스 시작"

if sudo systemctl is-active --quiet workoop; then
  info "기존 서비스를 재시작합니다..."
  sudo systemctl restart workoop
else
  sudo systemctl start workoop
fi

# 최대 40초 대기
info "앱 준비 대기 중..."
for i in $(seq 1 40); do
  sleep 1
  if curl -sf http://localhost:48080 > /dev/null 2>&1; then
    break
  fi
  echo -ne "."
done
echo ""

if sudo systemctl is-active --quiet workoop; then
  ok "서비스 실행 중"
else
  err "서비스 시작 실패. 로그 확인: sudo journalctl -u workoop -n 50"
fi

# ── 완료 안내 ─────────────────────────────────────────────────
PUBLIC_IP=$(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null \
         || curl -sf http://checkip.amazonaws.com 2>/dev/null \
         || echo "EC2-PUBLIC-IP")

echo ""
echo -e "${GREEN}${BOLD}══════════════════════════════════════${NC}"
echo -e "${GREEN}${BOLD}  설치 완료!${NC}"
echo -e "${GREEN}${BOLD}══════════════════════════════════════${NC}"
echo ""
echo -e "  접속 주소  : ${CYAN}${BOLD}http://${PUBLIC_IP}:48080${NC}"
echo -e "  로그 확인  : ${YELLOW}sudo journalctl -u workoop -f${NC}"
echo -e "  서비스 상태: ${YELLOW}sudo systemctl status workoop${NC}"
echo -e "  업데이트   : ${YELLOW}git -C $INSTALL_DIR pull && bash $INSTALL_DIR/setup.sh${NC}"
echo ""
