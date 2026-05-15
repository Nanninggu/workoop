#!/bin/bash
# ─────────────────────────────────────────────
#  PTKT 전체 빌드 스크립트
#  실행: ./build.sh
# ─────────────────────────────────────────────
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo "╔══════════════════════════════════════╗"
echo "║      PTKT 빌드 시작                  ║"
echo "╚══════════════════════════════════════╝"
echo ""

# ── Step 1: 프론트엔드 빌드 ──
echo "▶ [1/2] 프론트엔드 빌드 중..."
cd "$APP_DIR/frontend"
npm run build
echo "   ✅ 프론트엔드 빌드 완료"
echo ""

# ── Step 2: Spring Boot JAR 패키징 ──
echo "▶ [2/2] Spring Boot JAR 패키징 중..."
cd "$APP_DIR/backend"
mvn package -DskipTests -q
echo "   ✅ JAR 패키징 완료"
echo ""

JAR_FILE=$(ls "$APP_DIR/backend/target/ptkt-backend-"*.jar | grep -v original | head -1)

echo "╔══════════════════════════════════════╗"
echo "║      빌드 완료!                      ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "  JAR 파일: $JAR_FILE"
echo ""
echo "  다음 단계: ./create-app.sh 를 실행해서"
echo "  PTKT.app 을 생성하세요."
echo ""
