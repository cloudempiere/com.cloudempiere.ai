#!/bin/bash
# Debug helper for zoom link implementation
# Usage: ./debug-zoom.sh [idempiere-log-file]

LOG_FILE="${1:-/opt/idempiere/log/idempiere.log}"

echo "=== Zoom Link Debug Helper ==="
echo "Monitoring: $LOG_FILE"
echo ""
echo "Looking for [Zoom Handler] messages..."
echo "Press Ctrl+C to stop"
echo ""

if [ ! -f "$LOG_FILE" ]; then
    echo "ERROR: Log file not found: $LOG_FILE"
    echo "Usage: $0 [path-to-idempiere.log]"
    exit 1
fi

# Tail the log file and filter for zoom-related messages
tail -f "$LOG_FILE" | grep --line-buffered -E "\[Zoom|AEnv\.zoom|onZoom|ZoomCommand"
