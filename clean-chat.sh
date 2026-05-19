#!/bin/bash
# 채팅 메시지 초기화 스크립트
# 사용법: bash clean-chat.sh

set -e

DATA_DIR="/home/ubuntu/workoop-data"
DB_PATH="$DATA_DIR/ptkt-db"

# H2 jar 찾기
H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" | grep -v sources | head -1)

if [ -z "$H2_JAR" ]; then
  echo "[ERROR] H2 jar를 찾을 수 없습니다. mvn package 먼저 실행하세요."
  exit 1
fi

echo "[INFO] H2 jar: $H2_JAR"
echo ""
echo "삭제될 데이터:"
echo "  - 관리자(user_id=1) 채팅 메시지"
echo "  - 테스트유저(user_id=2) 채팅 메시지"
echo "  - 전체 채팅 기록"
echo ""
read -rp "계속하시겠습니까? (y/N): " CONFIRM
[ "$CONFIRM" != "y" ] && echo "취소됐습니다." && exit 0

# 앱 정지
echo "[INFO] 서비스 정지..."
sudo systemctl stop workoop

# SQL 실행
echo "[INFO] 채팅 메시지 삭제 중..."
java -cp "$H2_JAR" org.h2.tools.Shell \
  -url "jdbc:h2:file:${DB_PATH};MODE=MySQL" \
  -user sa -password "" \
  -sql "DELETE FROM chat_message;"

echo "[OK] 채팅 메시지 삭제 완료"

# 앱 재시작
echo "[INFO] 서비스 재시작..."
sudo systemctl start workoop

echo ""
echo "[완료] 채팅창이 깨끗하게 초기화됐습니다."
