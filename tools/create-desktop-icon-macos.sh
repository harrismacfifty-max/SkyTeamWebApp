#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SOURCE="$PROJECT_ROOT/assets/skyteam-icon.png"
OUTPUT_DIRECTORY="$PROJECT_ROOT/assets"
DESKTOP_NAME="SkyTeam Flugschule"
CREATE_APP=1

usage() {
    cat <<USAGE
Usage:
  ./tools/create-desktop-icon-macos.sh [options]

Options:
  --source <png>              PNG source image. Default: assets/skyteam-icon.png
  --output-directory <dir>    Directory for skyteam-icon.icns. Default: assets
  --desktop-name <name>       Name of the Desktop .app. Default: SkyTeam Flugschule
  --no-desktop-app            Only create the .icns file, no Desktop launcher
  -h, --help                  Show this help

The default source should already be cropped to the white circle with the airplane.
The script uses macOS built-in tools: sips and iconutil.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --source)
            SOURCE="${2:-}"
            shift 2
            ;;
        --output-directory)
            OUTPUT_DIRECTORY="${2:-}"
            shift 2
            ;;
        --desktop-name)
            DESKTOP_NAME="${2:-}"
            shift 2
            ;;
        --no-desktop-app)
            CREATE_APP=0
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
done

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command not found: $1" >&2
        exit 1
    fi
}

xml_escape() {
    local value="$1"
    value="${value//&/&amp;}"
    value="${value//</&lt;}"
    value="${value//>/&gt;}"
    value="${value//\"/&quot;}"
    value="${value//\'/&apos;}"
    printf '%s' "$value"
}

make_icon_png() {
    local size="$1"
    local output="$2"
    sips -s format png -z "$size" "$size" "$SOURCE" --out "$output" >/dev/null
}

write_launcher_executable() {
    local launcher_path="$1"
    local escaped_project_root
    escaped_project_root="$(printf '%q' "$PROJECT_ROOT")"

    cat > "$launcher_path" <<LAUNCHER
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$escaped_project_root
BACKEND_PORT="\${BACKEND_PORT:-8080}"
FRONTEND_PORT="\${FRONTEND_PORT:-8000}"
APP_PROFILE="\${APP_PROFILE:-dev}"

BACKEND_CLASSES="\$PROJECT_ROOT/backend/target/classes"
BACKEND_LOG_DIR="\$PROJECT_ROOT/backend/target/logs"
BACKEND_OUT_LOG="\$BACKEND_LOG_DIR/backend.out.log"
BACKEND_ERR_LOG="\$BACKEND_LOG_DIR/backend.err.log"
FRONTEND_OUT_LOG="\$BACKEND_LOG_DIR/frontend.out.log"
FRONTEND_ERR_LOG="\$BACKEND_LOG_DIR/frontend.err.log"

health_ok() {
    curl -fsS "http://localhost:\$BACKEND_PORT/api/health" >/dev/null 2>&1
}

http_ok() {
    curl -fsS "\$1" >/dev/null 2>&1
}

port_in_use() {
    lsof -nP -iTCP:"\$1" -sTCP:LISTEN >/dev/null 2>&1
}

show_error() {
    local message="\$1"
    echo "\$message" >&2
    if command -v osascript >/dev/null 2>&1; then
        osascript -e "display dialog \"\$message\" buttons {\"OK\"} with icon stop" >/dev/null 2>&1 || true
    fi
}

cd "\$PROJECT_ROOT"
mkdir -p "\$BACKEND_CLASSES" "\$BACKEND_LOG_DIR"

if ! health_ok; then
    if port_in_use "\$BACKEND_PORT"; then
        show_error "Backend-Port \$BACKEND_PORT ist belegt, aber /api/health antwortet nicht."
        exit 1
    fi

    if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
        show_error "Java/JDK wurde nicht gefunden. Bitte Java 17 oder neuer installieren."
        exit 1
    fi

    find "\$PROJECT_ROOT/backend/src/main/java" -name "*.java" > "\$PROJECT_ROOT/backend/target/sources.list"
    javac -encoding UTF-8 -d "\$BACKEND_CLASSES" @"\$PROJECT_ROOT/backend/target/sources.list"

    CLASSPATH="\$BACKEND_CLASSES"
    if [[ -n "\${EXTRA_CLASSPATH:-}" ]]; then
        CLASSPATH="\$CLASSPATH:\$EXTRA_CLASSPATH"
    fi

    export APP_PROFILE
    nohup java "-Dserver.port=\$BACKEND_PORT" -cp "\$CLASSPATH" de.skyteam.flightschool.FlightSchoolApplication > "\$BACKEND_OUT_LOG" 2> "\$BACKEND_ERR_LOG" &

    for _ in {1..40}; do
        if health_ok; then
            break
        fi
        sleep 0.5
    done

    if ! health_ok; then
        show_error "Backend konnte nicht gestartet werden. Logs: \$BACKEND_LOG_DIR"
        exit 1
    fi
fi

FRONTEND_URL="http://localhost:\$FRONTEND_PORT"
if command -v php >/dev/null 2>&1; then
    if ! http_ok "\$FRONTEND_URL"; then
        if port_in_use "\$FRONTEND_PORT"; then
            open "\$PROJECT_ROOT/frontend/index.html"
            exit 0
        fi

        API_BASE_URL="http://localhost:\$BACKEND_PORT/api" nohup php -S "localhost:\$FRONTEND_PORT" -t "\$PROJECT_ROOT/frontend" > "\$FRONTEND_OUT_LOG" 2> "\$FRONTEND_ERR_LOG" &
        sleep 1
    fi

    if http_ok "\$FRONTEND_URL"; then
        open "\$FRONTEND_URL"
    else
        open "\$PROJECT_ROOT/frontend/index.html"
    fi
else
    open "\$PROJECT_ROOT/frontend/index.html"
fi
LAUNCHER

    chmod +x "$launcher_path"
}

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "This script must be run on macOS." >&2
    exit 1
fi

if [[ -z "$SOURCE" || ! -f "$SOURCE" ]]; then
    echo "Source image not found: $SOURCE" >&2
    echo "Create assets/skyteam-icon.png first or pass --source <png>." >&2
    exit 1
fi

require_command sips
require_command iconutil

mkdir -p "$OUTPUT_DIRECTORY"
OUTPUT_ICNS="$OUTPUT_DIRECTORY/skyteam-icon.icns"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

ICONSET="$TMP_DIR/SkyTeam.iconset"
mkdir -p "$ICONSET"

make_icon_png 16 "$ICONSET/icon_16x16.png"
make_icon_png 32 "$ICONSET/icon_16x16@2x.png"
make_icon_png 32 "$ICONSET/icon_32x32.png"
make_icon_png 64 "$ICONSET/icon_32x32@2x.png"
make_icon_png 128 "$ICONSET/icon_128x128.png"
make_icon_png 256 "$ICONSET/icon_128x128@2x.png"
make_icon_png 256 "$ICONSET/icon_256x256.png"
make_icon_png 512 "$ICONSET/icon_256x256@2x.png"
make_icon_png 512 "$ICONSET/icon_512x512.png"
make_icon_png 1024 "$ICONSET/icon_512x512@2x.png"

iconutil -c icns "$ICONSET" -o "$OUTPUT_ICNS"
echo "Icon-ICNS erstellt: $OUTPUT_ICNS"

if [[ "$CREATE_APP" -eq 0 ]]; then
    exit 0
fi

DESKTOP_DIR="$HOME/Desktop"
APP_PATH="$DESKTOP_DIR/$DESKTOP_NAME.app"
CONTENTS_DIR="$APP_PATH/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RESOURCES_DIR="$CONTENTS_DIR/Resources"

if [[ -e "$APP_PATH" && ! -d "$APP_PATH" ]]; then
    echo "Desktop target exists but is not an app directory: $APP_PATH" >&2
    exit 1
fi

mkdir -p "$MACOS_DIR" "$RESOURCES_DIR"
cp "$OUTPUT_ICNS" "$RESOURCES_DIR/SkyTeam.icns"

BUNDLE_NAME_XML="$(xml_escape "$DESKTOP_NAME")"
cat > "$CONTENTS_DIR/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>de</string>
    <key>CFBundleDisplayName</key>
    <string>$BUNDLE_NAME_XML</string>
    <key>CFBundleExecutable</key>
    <string>start-skyteam</string>
    <key>CFBundleIconFile</key>
    <string>SkyTeam</string>
    <key>CFBundleIdentifier</key>
    <string>de.skyteam.flightschool.launcher</string>
    <key>CFBundleName</key>
    <string>$BUNDLE_NAME_XML</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.13</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

write_launcher_executable "$MACOS_DIR/start-skyteam"
touch "$APP_PATH"

echo "Desktop-App erstellt: $APP_PATH"
echo "Hinweis: Falls Finder das Icon nicht sofort aktualisiert, einmal Finder neu laden oder kurz warten."
