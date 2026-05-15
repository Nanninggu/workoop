#!/bin/bash
# ─────────────────────────────────────────────
#  Workoop.app macOS 앱 번들 생성 스크립트
#  실행: ./create-app.sh
# ─────────────────────────────────────────────

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Coop"
APP_BUNDLE="/Applications/${APP_NAME}.app"
PORT=48080
JAVA_PATH="/usr/bin/java"

echo ""
echo "╔══════════════════════════════════════╗"
echo "║      Workoop.app 생성 중...          ║"
echo "╚══════════════════════════════════════╝"
echo ""

# JAR 파일 확인
JAR_FILE=$(ls "$APP_DIR/backend/target/ptkt-backend-"*.jar 2>/dev/null | grep -v original | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 오류: JAR 파일을 찾을 수 없습니다."
    echo "   먼저 ./build.sh 를 실행해주세요."
    exit 1
fi

echo "  JAR 확인: $JAR_FILE"

# 기존 앱 삭제
if [ -d "$APP_BUNDLE" ]; then
    echo "  기존 Workoop.app 삭제 중..."
    rm -rf "$APP_BUNDLE"
fi

# 앱 번들 디렉토리 구조 생성
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# ── Info.plist 생성 ──
cat > "$APP_BUNDLE/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>Workoop</string>
    <key>CFBundleIdentifier</key>
    <string>com.personal.workoop</string>
    <key>CFBundleName</key>
    <string>Workoop</string>
    <key>CFBundleDisplayName</key>
    <string>Workoop</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleVersion</key>
    <string>1.0.0</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>LSMinimumSystemVersion</key>
    <string>12.0</string>
    <key>LSUIElement</key>
    <false/>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

# ── 앱 아이콘 생성 (Python으로 PNG → ICNS) ──
echo "  아이콘 생성 중..."

ICONSET_DIR="/tmp/workoop-icon.iconset"
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"

python3 << 'PYEOF'
import struct, zlib, os

def make_png(size):
    w = h = size
    rows = []
    for y in range(h):
        row = [0]  # filter byte
        for x in range(w):
            # 모서리 둥글게 마스크 (간단 버전)
            margin = size // 8
            in_corner = False

            # 배경: 다크 블루 그라디언트
            t = (x + y) / (w + h)
            r = int(30 + t * 7)
            g = int(58 + t * 41)
            b = int(138 + t * 97)

            # 바 차트 그리기 (3개 막대)
            bar_w = size // 10
            gap   = size // 20
            base_y = int(h * 0.85)

            # Bar 1 (초록)
            b1x = int(w * 0.15)
            b1h = int(h * 0.35)
            if b1x <= x < b1x + bar_w and base_y - b1h <= y < base_y:
                r, g, b = 52, 211, 153

            # Bar 2 (주황)
            b2x = int(w * 0.42)
            b2h = int(h * 0.55)
            if b2x <= x < b2x + bar_w and base_y - b2h <= y < base_y:
                r, g, b = 251, 146, 60

            # Bar 3 (하늘)
            b3x = int(w * 0.68)
            b3h = int(h * 0.75)
            if b3x <= x < b3x + bar_w and base_y - b3h <= y < base_y:
                r, g, b = 147, 197, 253

            # 화살표 선 (대각)
            ax1, ay1 = int(w * 0.12), int(h * 0.82)
            ax2, ay2 = int(w * 0.80), int(h * 0.15)
            # 선 두께: 2px
            dx, dy = ax2 - ax1, ay2 - ay1
            length = (dx*dx + dy*dy) ** 0.5
            if length > 0:
                dist = abs((x - ax1) * dy - (y - ay1) * dx) / length
                t_param = ((x - ax1) * dx + (y - ay1) * dy) / (length * length)
                if dist < size * 0.025 and 0.05 < t_param < 0.95:
                    r, g, b = 255, 255, 255

            # 별(스파클)
            sx, sy = int(w * 0.80), int(h * 0.15)
            if abs(x - sx) + abs(y - sy) < size * 0.07:
                r, g, b = 253, 224, 71

            row.extend([r, g, b])
        rows.append(bytes(row))

    raw = b''.join(rows)

    def chunk(tag, data):
        c = tag + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n' +
            chunk(b'IHDR', ihdr) +
            chunk(b'IDAT', zlib.compress(raw)) +
            chunk(b'IEND', b''))

iconset = '/tmp/workoop-icon.iconset'
sizes = [16, 32, 64, 128, 256, 512, 1024]
names = {
    16:   'icon_16x16.png',
    32:   'icon_16x16@2x.png',
    64:   'icon_32x32@2x.png',
    128:  'icon_128x128.png',
    256:  'icon_128x128@2x.png',
    512:  'icon_256x256@2x.png',
    1024: 'icon_512x512@2x.png',
}
for size, name in names.items():
    with open(os.path.join(iconset, name), 'wb') as f:
        f.write(make_png(size))

print('  PNG 파일 생성 완료')
PYEOF

# iconutil로 ICNS 생성
iconutil -c icns "$ICONSET_DIR" -o "$APP_BUNDLE/Contents/Resources/AppIcon.icns" 2>/dev/null \
    && echo "  ✅ 아이콘 생성 완료" \
    || echo "  ⚠️  아이콘 생성 건너뜀 (기본 아이콘 사용)"

rm -rf "$ICONSET_DIR"

# ── 실행 스크립트 생성 ──
cat > "$APP_BUNDLE/Contents/MacOS/Workoop" << 'SCRIPT'
#!/bin/bash
# Workoop macOS App Launcher
# - 서버가 이미 실행 중이면 브라우저만 열기
# - 서버가 꺼져 있으면 로딩 화면을 즉시 보여주고 백그라운드에서 서버 시작
# - 최적화된 JVM 플래그로 기동 시간 단축

export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:$PATH"

PORT=48080
LOADER_PORT=58083
APP_DIR="$HOME/Documents/Coop-Application"
JAR="$APP_DIR/backend/target/ptkt-backend-1.0.0.jar"
PID_FILE="/tmp/coop-server.pid"
LOADER_PID_FILE="/tmp/coop-loader.pid"
SERVER_LOG="/tmp/coop-server.log"
LOADING_HTML="/tmp/workoop-loading.html"

# ── 앱 창 열기 ──
open_app_window() {
    local url="$1"
    if [ -d "/Applications/Google Chrome.app" ]; then
        open -na "Google Chrome" --args --new-window --app="$url"
    elif [ -d "/Applications/Microsoft Edge.app" ]; then
        open -na "Microsoft Edge" --args --new-window --app="$url"
    else
        open "$url"
    fi
}

# ── 로딩 서버 정리 ──
cleanup_loader() {
    if [ -f "$LOADER_PID_FILE" ]; then
        kill "$(cat $LOADER_PID_FILE)" 2>/dev/null
        rm -f "$LOADER_PID_FILE"
    fi
}

# ── 이미 실행 중인지 확인 ──
if [ -f "$PID_FILE" ]; then
    EXISTING_PID=$(cat "$PID_FILE")
    if kill -0 "$EXISTING_PID" 2>/dev/null; then
        # 타임아웃 3초 — 서버가 hang 상태여도 launcher가 멈추지 않음
        if curl -s --max-time 3 "http://localhost:$PORT/" > /dev/null 2>&1; then
            osascript -e 'display notification "Workoop이 이미 실행 중입니다." with title "Workoop"'
            open_app_window "http://localhost:$PORT"
            exit 0
        fi
    fi
    # 프로세스가 없거나 응답 없으면 → 강제 종료 후 재시작
    kill "$EXISTING_PID" 2>/dev/null
    sleep 1
    rm -f "$PID_FILE"
fi

# ── JAR 파일 확인 ──
if [ ! -f "$JAR" ]; then
    osascript -e "display alert \"Workoop 시작 실패\" message \"JAR 파일이 없습니다.\n\n터미널에서 아래 명령어를 실행하세요:\n  cd ~/Documents/Coop-Application\n  ./build.sh\" as warning"
    exit 1
fi

# ── 로딩 HTML 생성 ──
cat > "$LOADING_HTML" << 'LOADING_EOF'
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Workoop 시작 중...</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif;
      background: #0f172a;
      color: #e2e8f0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      gap: 0;
      user-select: none;
      -webkit-app-region: drag;
    }
    .logo {
      font-size: 52px;
      font-weight: 800;
      background: linear-gradient(135deg, #3b82f6, #8b5cf6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      letter-spacing: -2px;
      margin-bottom: 8px;
    }
    .tagline {
      font-size: 13px;
      color: #475569;
      margin-bottom: 48px;
      letter-spacing: 0.05em;
    }
    .ring {
      position: relative;
      width: 56px;
      height: 56px;
      margin-bottom: 28px;
    }
    .ring svg {
      position: absolute;
      top: 0; left: 0;
      animation: rotate 1.2s linear infinite;
    }
    @keyframes rotate { to { transform: rotate(360deg); } }
    .ring-bg { stroke: #1e293b; }
    .ring-fg {
      stroke: url(#grad);
      stroke-dasharray: 120;
      stroke-dashoffset: 30;
      stroke-linecap: round;
    }
    .status {
      font-size: 13px;
      color: #475569;
      min-height: 20px;
      transition: color 0.3s;
    }
    .status.ready { color: #10b981; }
    .dots { display: inline-block; }
    .elapsed {
      font-size: 11px;
      color: #334155;
      margin-top: 8px;
    }
  </style>
</head>
<body>
  <div class="logo">Workoop</div>
  <div class="tagline">팀 협업 & KPI 관리 플랫폼</div>
  <div class="ring">
    <svg viewBox="0 0 56 56" fill="none" width="56" height="56">
      <defs>
        <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#3b82f6"/>
          <stop offset="100%" stop-color="#8b5cf6"/>
        </linearGradient>
      </defs>
      <circle cx="28" cy="28" r="22" stroke-width="4" class="ring-bg"/>
      <circle cx="28" cy="28" r="22" stroke-width="4" class="ring-fg"
              transform="rotate(-90 28 28)"/>
    </svg>
  </div>
  <div class="status" id="status">서버 시작 중<span class="dots" id="dots"></span></div>
  <div class="elapsed" id="elapsed"></div>

  <script>
    var start = Date.now();
    var dots = ['', '.', '..', '...'];
    var dotIdx = 0;
    var attempts = 0;

    // 점 애니메이션
    setInterval(function() {
      dotIdx = (dotIdx + 1) % dots.length;
      document.getElementById('dots').textContent = dots[dotIdx];
    }, 500);

    // 경과시간 표시
    setInterval(function() {
      var sec = Math.floor((Date.now() - start) / 1000);
      if (sec > 2) {
        document.getElementById('elapsed').textContent = sec + '초 경과';
      }
    }, 1000);

    // 서버 폴링 (mode: no-cors → connection refused면 catch, 응답오면 성공)
    function checkServer() {
      attempts++;
      fetch('http://localhost:48080/', { mode: 'no-cors', cache: 'no-store' })
        .then(function() {
          document.getElementById('status').textContent = '준비 완료! 잠시 후 이동합니다';
          document.getElementById('status').className = 'status ready';
          document.getElementById('elapsed').textContent = '';
          setTimeout(function() {
            window.location.href = 'http://localhost:48080/';
          }, 400);
        })
        .catch(function() {
          setTimeout(checkServer, 700);
        });
    }

    setTimeout(checkServer, 1500);
  </script>
</body>
</html>
LOADING_EOF

# ── 로딩 서버 시작 (Python 내장 HTTP 서버) ──
cleanup_loader
python3 -m http.server "$LOADER_PORT" --bind 127.0.0.1 --directory /tmp > /dev/null 2>&1 &
echo $! > "$LOADER_PID_FILE"
sleep 0.4

# ── 즉시 로딩 화면 열기 ──
open_app_window "http://127.0.0.1:$LOADER_PORT/workoop-loading.html"

# ── JVM 최적화 플래그로 서버 시작 ──
# -XX:TieredStopAtLevel=1 : C1 컴파일러만 사용 (기동 속도 우선)
# -XX:+UseSerialGC        : 단일 사용자 앱에 적합한 가벼운 GC
# -Xms64m -Xmx512m       : 힙 범위 명시 (초기 힙 줄임)
# -Djava.security.egd    : SecureRandom 엔트로피 병목 제거
nohup /usr/bin/java \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseSerialGC \
    -Xms64m -Xmx512m \
    -Djava.security.egd=file:/dev/./urandom \
    -jar "$JAR" > "$SERVER_LOG" 2>&1 &
echo $! > "$PID_FILE"

# ── 서버 준비 대기 (로딩 서버 종료 타이밍 결정용) ──
for i in $(seq 1 60); do
    sleep 1
    if curl -s --max-time 3 "http://localhost:$PORT/" > /dev/null 2>&1; then
        sleep 2   # 로딩 화면이 리다이렉트 처리할 시간 여유
        cleanup_loader
        osascript -e 'display notification "Workoop 준비 완료!" with title "Workoop" subtitle "서비스를 시작하세요 ✅"'
        exit 0
    fi
done

# 60초 초과 시 실패 처리
cleanup_loader
osascript -e "display alert \"Workoop 시작 실패\" message \"60초 내에 서버가 응답하지 않았습니다.\n로그 확인: $SERVER_LOG\" as warning"
exit 1
SCRIPT

chmod +x "$APP_BUNDLE/Contents/MacOS/Workoop"

echo ""
echo "╔══════════════════════════════════════╗"
echo "║      Workoop.app 생성 완료! 🎉       ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "  위치: $APP_BUNDLE"
echo ""
echo "  ✅ Launchpad에서 Workoop을 찾을 수 있습니다."
echo "  ✅ Dock에 드래그해서 고정할 수 있습니다."
echo ""
echo "  ──────────────────────────────────────"
echo "  Dock 고정 방법:"
echo "  1. Launchpad 열기 (F4 또는 Dock의 로켓 아이콘)"
echo "  2. Workoop 앱 찾기"
echo "  3. Workoop 아이콘을 Dock으로 드래그"
echo "  ──────────────────────────────────────"
echo ""
