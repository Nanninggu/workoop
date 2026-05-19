#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  Workoop EC2 배포 스크립트
#  EC2 최초 1회 실행: bash deploy.sh
#  이후 업데이트:    bash deploy.sh --update
# ─────────────────────────────────────────────────────────────
set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="/home/ubuntu/workoop-data"
JAR_PATH="$APP_DIR/backend/target/ptkt-backend-1.0.0.jar"
SERVICE_SRC="$APP_DIR/deploy/workoop.service"
ENV_FILE="/etc/workoop.env"

echo "=============================="
echo "  Workoop Deploy"
echo "=============================="

# ── /etc/workoop.env 확인 ──
if [ ! -f "$ENV_FILE" ]; then
  echo ""
  echo "[ERROR] 환경변수 파일이 없습니다. 먼저 생성하세요:"
  echo ""
  echo "  sudo cp $APP_DIR/.env.example $ENV_FILE"
  echo "  sudo vi $ENV_FILE         ← 실제 값 입력"
  echo "  sudo chmod 600 $ENV_FILE"
  echo ""
  exit 1
fi

# ── Java 17 확인·설치 ──
if ! java -version 2>&1 | grep -qE '"(17|21)'; then
  echo "[INFO] Java 17 설치 중..."
  sudo apt-get update -qq
  sudo apt-get install -y openjdk-17-jdk
fi
echo "[OK] $(java -version 2>&1 | head -1)"

# ── Maven 확인·설치 ──
if ! command -v mvn &>/dev/null; then
  echo "[INFO] Maven 설치 중..."
  sudo apt-get install -y maven
fi
echo "[OK] $(mvn -version 2>&1 | head -1)"

# ── H2 데이터 디렉토리 ──
mkdir -p "$DATA_DIR"
echo "[OK] 데이터 디렉토리: $DATA_DIR"

# ── 백엔드 빌드 ──
echo "[INFO] 백엔드 빌드 중..."
cd "$APP_DIR/backend"
mvn package -DskipTests -q
echo "[OK] 빌드 완료"

# ── systemd 서비스 설치 ──
echo "[INFO] systemd 서비스 등록..."
sudo cp "$SERVICE_SRC" /etc/systemd/system/workoop.service
sudo systemctl daemon-reload
sudo systemctl enable workoop

# ── 서비스 시작 (또는 재시작) ──
if sudo systemctl is-active --quiet workoop; then
  sudo systemctl restart workoop
  echo "[OK] 서비스 재시작 완료"
else
  sudo systemctl start workoop
  echo "[OK] 서비스 시작 완료"
fi

echo ""
echo "=============================="
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "EC2-IP")
echo "  접속 주소: http://$PUBLIC_IP:48080"
echo "  로그 확인: sudo journalctl -u workoop -f"
echo "  서비스 상태: sudo systemctl status workoop"
echo "=============================="
