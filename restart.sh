#!/bin/bash
# Coop-Application 재시작 스크립트

PID_FILE="/tmp/coop-server.pid"

echo "Coop 서버 종료 중..."
kill $(cat "$PID_FILE") 2>/dev/null
rm -f "$PID_FILE"
sleep 1

echo "Coop 앱 시작 중..."
open /Applications/Coop.app

echo "완료! 잠시 후 Coop이 열립니다."
