#!/bin/bash
# =============================================================
#  Workoop — EC2 최초 환경 세팅 (Ubuntu 22.04 LTS 기준)
#  실행: bash 01-setup.sh
#  ⚠️  이 스크립트는 처음 한 번만 실행합니다.
# =============================================================
set -e

REPO_URL="https://github.com/Nanninggu/workoop.git"
APP_DIR="/home/ubuntu/workoop"
DATA_DIR="/home/ubuntu/workoop-data"
ENV_FILE="/etc/workoop.env"
SERVICE_FILE="/etc/systemd/system/workoop.service"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

log()  { echo -e "${CYAN}▶ $1${NC}"; }
ok()   { echo -e "${GREEN}✅ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }
err()  { echo -e "${RED}✗ $1${NC}"; exit 1; }

# ── root 확인 ──
[ "$EUID" -ne 0 ] && err "sudo 로 실행하세요: sudo bash 01-setup.sh"

# ─────────────────────────────────────────
# 1. 패키지 업데이트
# ─────────────────────────────────────────
log "[1/7] 패키지 업데이트"
apt-get update -y && apt-get upgrade -y
ok "패키지 업데이트 완료"

# ─────────────────────────────────────────
# 2. Java 17 (Amazon Corretto)
# ─────────────────────────────────────────
log "[2/7] Java 17 설치"
apt-get install -y wget gnupg
wget -q https://apt.corretto.aws/corretto.key -O - | gpg --dearmor -o /usr/share/keyrings/corretto.gpg
echo "deb [signed-by=/usr/share/keyrings/corretto.gpg] https://apt.corretto.aws stable main" \
  > /etc/apt/sources.list.d/corretto.list
apt-get update -y
apt-get install -y java-17-amazon-corretto-jdk
java -version
ok "Java 17 설치 완료"

# ─────────────────────────────────────────
# 3. Maven
# ─────────────────────────────────────────
log "[3/7] Maven 설치"
apt-get install -y maven
mvn -version
ok "Maven 설치 완료"

# ─────────────────────────────────────────
# 4. Node.js 20 (프론트 빌드용)
# ─────────────────────────────────────────
log "[4/7] Node.js 20 설치"
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs
node -v && npm -v
ok "Node.js 설치 완료"

# ─────────────────────────────────────────
# 5. Nginx
# ─────────────────────────────────────────
log "[5/7] Nginx 설치 및 설정"
apt-get install -y nginx

cat > /etc/nginx/sites-available/workoop <<'NGINX'
server {
    listen 80;
    server_name _;

    client_max_body_size 10m;

    location / {
        proxy_pass         http://127.0.0.1:48080;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 120s;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/workoop /etc/nginx/sites-enabled/workoop
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable nginx
systemctl restart nginx
ok "Nginx 설정 완료"

# ─────────────────────────────────────────
# 6. GitHub 소스 클론
# ─────────────────────────────────────────
log "[6/7] 소스 클론: $REPO_URL"
mkdir -p "$DATA_DIR"

if [ -d "$APP_DIR" ]; then
  warn "이미 클론된 폴더가 있습니다. 삭제 후 재클론합니다."
  rm -rf "$APP_DIR"
fi
git clone "$REPO_URL" "$APP_DIR"
chown -R ubuntu:ubuntu "$APP_DIR" "$DATA_DIR"
ok "클론 완료: $APP_DIR"

# ─────────────────────────────────────────
# 7. 환경변수 파일 생성
# ─────────────────────────────────────────
log "[7/7] 환경변수 파일 생성"
if [ ! -f "$ENV_FILE" ]; then
cat > "$ENV_FILE" <<'ENV'
# Workoop 환경변수 — 실제 값으로 수정하세요
GROQ_API_KEY=your_groq_api_key_here
JWT_SECRET=your_jwt_secret_at_least_32chars_here
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
SLACK_CHANNEL_ID=C0XXXXXXXXX
ENV
chmod 600 "$ENV_FILE"
warn "👉 $ENV_FILE 파일을 열어 실제 API 키를 입력하세요!"
else
  ok "환경변수 파일 이미 존재: $ENV_FILE"
fi

# ─────────────────────────────────────────
# systemd 서비스 등록 (heredoc으로 직접 생성)
# ─────────────────────────────────────────
log "systemd 서비스 등록"
cat > "$SERVICE_FILE" <<'SERVICE'
[Unit]
Description=Workoop Team Collaboration Platform
After=network.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
EnvironmentFile=/etc/workoop.env
Environment="SPRING_DATASOURCE_URL=jdbc:h2:file:/home/ubuntu/workoop-data/ptkt-db;AUTO_SERVER=TRUE;MODE=MySQL"
WorkingDirectory=/home/ubuntu/workoop
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /home/ubuntu/workoop/backend/target/ptkt-backend-1.0.0.jar
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal
SyslogIdentifier=workoop

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable workoop
ok "서비스 등록 완료"

# ─────────────────────────────────────────
echo ""
echo -e "${BOLD}════════════════════════════════════════${NC}"
echo -e "${GREEN}  EC2 세팅 완료!${NC}"
echo -e "${BOLD}════════════════════════════════════════${NC}"
echo ""
echo "다음 단계:"
echo "  1) sudo nano $ENV_FILE  ← API 키 입력"
echo "  2) sudo bash $APP_DIR/deploy/02-deploy.sh  ← 빌드 & 실행"
echo ""
