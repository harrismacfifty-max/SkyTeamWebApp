#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

bad_c3="$(printf '\303\203')"
bad_c2="$(printf '\303\202')"
bad_e2="$(printf '\303\242')"
bad_fffd="$(printf '\357\277\275')"
found=0

while IFS= read -r -d '' file; do
    for pattern in "$bad_c3" "$bad_c2" "$bad_e2" "$bad_fffd"; do
        if LC_ALL=C grep -q "$pattern" "$file"; then
            relative="${file#$PROJECT_ROOT/}"
            LC_ALL=C grep -n "$pattern" "$file" | while IFS= read -r line; do
                echo "$relative:$line"
            done
            found=1
        fi
    done
done < <(
    find "$PROJECT_ROOT" \
        \( -path "$PROJECT_ROOT/.git" -o -path "$PROJECT_ROOT/backend/target" \) -prune -o \
        -type f \( \
            -name '*.bat' -o \
            -name '*.cmd' -o \
            -name '*.css' -o \
            -name '*.html' -o \
            -name '*.java' -o \
            -name '*.js' -o \
            -name '*.json' -o \
            -name '*.md' -o \
            -name '*.php' -o \
            -name '*.properties' -o \
            -name '*.ps1' -o \
            -name '*.sh' -o \
            -name '*.sql' -o \
            -name '*.txt' -o \
            -name '*.xml' -o \
            -name '*.yaml' -o \
            -name '*.yml' \
        \) -print0
)

if [ "$found" -ne 0 ]; then
    echo "Encoding check failed: suspicious mojibake markers found. Save text files as UTF-8 and repair the listed lines." >&2
    exit 1
fi

echo "Encoding check passed."
