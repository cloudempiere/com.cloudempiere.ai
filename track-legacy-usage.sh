#!/bin/bash
# track-legacy-usage.sh - Track runtime usage of legacy plugin classes

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================="
echo "Legacy Plugin Runtime Usage Tracker"
echo "========================================="
echo ""

# Check if iDempiere is running
if ! pgrep -f "idempiere" > /dev/null; then
    echo -e "${RED}❌ iDempiere is not running${NC}"
    echo "Start iDempiere to track runtime usage"
    exit 1
fi

echo -e "${GREEN}✅ iDempiere is running${NC}"
echo ""

# Find iDempiere log file
LOG_FILE="${IDEMPIERE_HOME:-$HOME/idempiere}/log/idempiere.log"

if [ ! -f "$LOG_FILE" ]; then
    echo -e "${RED}❌ Log file not found: $LOG_FILE${NC}"
    echo "Set IDEMPIERE_HOME or check log location"
    exit 1
fi

echo "Monitoring log file: $LOG_FILE"
echo "Duration: 30 seconds"
echo "Perform actions in iDempiere (open chat, send queries, etc.)"
echo ""
echo "Press Ctrl+C to stop early..."
echo ""

# Create temporary file for tracking
TEMP_FILE=$(mktemp)

# Monitor log for legacy plugin class usage
timeout 30s tail -f "$LOG_FILE" | grep --line-buffered "com.cloudempiere.ai" | \
    grep -v "com.cloudempiere.ai.core" | \
    grep -v "com.cloudempiere.ai.sales" | \
    grep -v "com.cloudempiere.ai.inventory" | \
    grep -v "com.cloudempiere.ai.purchasing" | \
    grep -v "com.cloudempiere.ai.support" | \
    grep -v "com.cloudempiere.ai.kb" | tee "$TEMP_FILE" || true

echo ""
echo "========================================="
echo "ANALYSIS"
echo "========================================="
echo ""

if [ ! -s "$TEMP_FILE" ]; then
    echo -e "${GREEN}✅ No legacy plugin usage detected!${NC}"
    echo "This suggests the legacy plugin is not being used at runtime"
else
    echo -e "${YELLOW}⚠️  Legacy plugin usage detected:${NC}"
    echo ""

    # Extract unique class names
    grep -o "com\.cloudempiere\.ai\.[a-zA-Z0-9.]*" "$TEMP_FILE" | sort | uniq | while read class; do
        echo "  - $class"
    done

    echo ""
    echo -e "${RED}Action required:${NC} These classes are still being loaded from legacy plugin"
fi

rm -f "$TEMP_FILE"

echo ""
echo "========================================="
echo "NEXT STEPS"
echo "========================================="
echo ""
echo "1. Run: ./validate-legacy-plugin.sh"
echo "   To see which packages are duplicated"
echo ""
echo "2. Run unit tests with legacy plugin disabled:"
echo "   mvn test -pl !com.cloudempiere.ai.plugin"
echo ""
echo "3. Check OSGi bundle usage:"
echo "   telnet localhost 12612"
echo "   osgi> bundles | grep cloudempiere"
echo ""
