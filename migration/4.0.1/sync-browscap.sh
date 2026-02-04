#!/bin/bash
set -euo pipefail

# Browscap User-Agent Database Sync Script
# Usage: ./sync-browscap.sh
# Or with custom values: PGHOST=myhost PGDATABASE=mydb ./sync-browscap.sh

# Configuration with defaults
BROWSCAP_URL="${BROWSCAP_URL:-http://browscap.org/stream?q=BrowsCapCSV}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-postgres}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-postgres}"
USE_SUDO="${USE_SUDO:-false}"

TEMP_CSV="/tmp/browscap_$(date +%s).csv"

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

psql_exec() {
    if [ "$USE_SUDO" = "true" ]; then
        if [ "$PGHOST" != "localhost" ]; then
            sudo -u "$PGUSER" psql -h "$PGHOST" -p "$PGPORT" "$@"
        else
            sudo -u "$PGUSER" psql "$@"
        fi
    else
        PGPASSWORD="$PGPASSWORD" psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$@"
    fi
}

cleanup() {
    rm -f "$TEMP_CSV" "$TEMP_CLEAN"
}

trap cleanup EXIT

log "=== Starting Browscap synchronization ==="
log "Database: $PGUSER@$PGHOST:$PGPORT/$PGDATABASE"

# Download
log "Downloading CSV..."
if ! curl -sS -L -o "$TEMP_CSV" "$BROWSCAP_URL"; then
    log "ERROR: Download failed"
    exit 1
fi

FILE_SIZE=$(wc -c < "$TEMP_CSV" | tr -d ' ')
log "Downloaded: $FILE_SIZE bytes"

if [ "$FILE_SIZE" -lt 1000 ]; then
    log "ERROR: File too small"
    exit 1
fi

# Skip first 2 metadata lines (keep header + data)
log "Preparing CSV for import..."
TEMP_CLEAN="/tmp/browscap_clean_$(date +%s).csv"
tail -n +3 "$TEMP_CSV" > "$TEMP_CLEAN"
rm "$TEMP_CSV"
TEMP_CSV="$TEMP_CLEAN"

CLEAN_SIZE=$(wc -c < "$TEMP_CSV" | tr -d ' ')
CLEAN_LINES=$(wc -l < "$TEMP_CSV")
log "Cleaned: $CLEAN_LINES lines, $CLEAN_SIZE bytes"

# Import with atomic swap
log "Importing to PostgreSQL..."

psql_exec -d "$PGDATABASE" <<'SQL'

-- Create NEW table with all columns
DROP TABLE IF EXISTS utils.user_agents_new;

CREATE TABLE utils.user_agents_new (
    property_name TEXT PRIMARY KEY,
    master_parent TEXT,
    lite_mode TEXT,
    parent TEXT,
    comment TEXT,
    browser TEXT,
    browser_type TEXT,
    browser_bits TEXT,
    browser_maker TEXT,
    browser_modus TEXT,
    version TEXT,
    major_ver TEXT,
    minor_ver TEXT,
    platform TEXT,
    platform_version TEXT,
    platform_description TEXT,
    platform_bits TEXT,
    platform_maker TEXT,
    alpha TEXT,
    beta TEXT,
    win16 TEXT,
    win32 TEXT,
    win64 TEXT,
    frames TEXT,
    iframes TEXT,
    tables TEXT,
    cookies TEXT,
    background_sounds TEXT,
    javascript TEXT,
    vbscript TEXT,
    java_applets TEXT,
    activex_controls TEXT,
    is_mobile_device TEXT,
    is_tablet TEXT,
    is_syndication_reader TEXT,
    crawler TEXT,
    is_fake TEXT,
    is_anonymized TEXT,
    is_modified TEXT,
    css_version TEXT,
    aol_version TEXT,
    device_name TEXT,
    device_maker TEXT,
    device_type TEXT,
    device_pointing_method TEXT,
    device_code_name TEXT,
    device_brand_name TEXT,
    rendering_engine_name TEXT,
    rendering_engine_version TEXT,
    rendering_engine_description TEXT,
    rendering_engine_maker TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

SQL

# Feed the cleaned CSV to psql via COPY (with HEADER to skip first line)
# Specify only the 50 columns from CSV, updated_at will use DEFAULT NOW()
cat "$TEMP_CSV" | psql_exec -d "$PGDATABASE" -c "COPY utils.user_agents_new (property_name, master_parent, lite_mode, parent, comment, browser, browser_type, browser_bits, browser_maker, browser_modus, version, major_ver, minor_ver, platform, platform_version, platform_description, platform_bits, platform_maker, alpha, beta, win16, win32, win64, frames, iframes, tables, cookies, background_sounds, javascript, vbscript, java_applets, activex_controls, is_mobile_device, is_tablet, is_syndication_reader, crawler, is_fake, is_anonymized, is_modified, css_version, aol_version, device_name, device_maker, device_type, device_pointing_method, device_code_name, device_brand_name, rendering_engine_name, rendering_engine_version, rendering_engine_description, rendering_engine_maker) FROM STDIN WITH (FORMAT csv, HEADER true, DELIMITER ',', QUOTE '\"');"

if [ ${PIPESTATUS[1]} -ne 0 ]; then
    log "ERROR: COPY failed"
    exit 1
fi

# Clean data and perform atomic swap
psql_exec -d "$PGDATABASE" <<'SQL'

-- Drop old indexes if they exist
DROP INDEX IF EXISTS utils.idx_ua_platform;
DROP INDEX IF EXISTS utils.idx_ua_device_type;
DROP INDEX IF EXISTS utils.idx_ua_browser;
DROP INDEX IF EXISTS utils.idx_ua_is_mobile;

-- Create indexes
CREATE INDEX idx_ua_platform ON utils.user_agents_new(platform);
CREATE INDEX idx_ua_device_type ON utils.user_agents_new(device_type);
CREATE INDEX idx_ua_browser ON utils.user_agents_new(browser);
CREATE INDEX idx_ua_is_mobile ON utils.user_agents_new(is_mobile_device);

-- Count imported rows
SELECT COUNT(*) as rows_imported FROM utils.user_agents_new;

-- ATOMIC SWAP
DROP TABLE IF EXISTS utils.user_agents CASCADE;
ALTER TABLE utils.user_agents_new RENAME TO user_agents;

-- Final stats
SELECT 
    COUNT(*) as total,
    COUNT(DISTINCT platform) as platforms,
    COUNT(DISTINCT device_type) as device_types,
    COUNT(DISTINCT browser) as browsers
FROM utils.user_agents;

SQL

if [ $? -ne 0 ]; then
    log "ERROR: Import or swap failed"
    exit 1
fi

log "=== Synchronization completed ==="

exit 0