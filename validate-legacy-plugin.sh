#!/bin/bash
# validate-legacy-plugin.sh - Verify what's left in legacy plugin vs migrated

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

LEGACY="com.cloudempiere.ai.plugin"
CORE="com.cloudempiere.ai.core"

echo "========================================="
echo "Legacy Plugin Validation Report"
echo "========================================="
echo ""

# Function to check if package exists in both locations
check_package() {
    local package=$1
    local legacy_path="$LEGACY/src/com/cloudempiere/ai/$package"
    local core_path="$CORE/src/com/cloudempiere/ai/$package"

    if [ -d "$legacy_path" ] && [ -d "$core_path" ]; then
        echo -e "${YELLOW}⚠️  DUPLICATE${NC}: $package (exists in both)"
        echo "   Legacy: $(find "$legacy_path" -name "*.java" | wc -l) files"
        echo "   Core:   $(find "$core_path" -name "*.java" | wc -l) files"
        return 0
    elif [ -d "$legacy_path" ]; then
        echo -e "${RED}❌ LEGACY ONLY${NC}: $package"
        echo "   Files: $(find "$legacy_path" -name "*.java" | wc -l)"
        return 1
    elif [ -d "$core_path" ]; then
        echo -e "${GREEN}✅ MIGRATED${NC}: $package"
        return 2
    else
        echo -e "${BLUE}ℹ️  NOT FOUND${NC}: $package"
        return 3
    fi
}

echo "1. PACKAGE MIGRATION STATUS"
echo "----------------------------"
echo ""

PACKAGES=(
    "provider"
    "database"
    "context"
    "component"
    "util"
    "model"
    "process"
    "boundary"
    "orchestrator"
    "rag"
    "guardrails"
    "observability"
    "kb"
    "health"
    "error"
    "event"
    "routing"
    "service"
    "tool"
    "factory"
    "function"
    "agent"
)

DUPLICATES=0
LEGACY_ONLY=0
MIGRATED=0

for pkg in "${PACKAGES[@]}"; do
    check_package "$pkg"
    result=$?
    case $result in
        0) DUPLICATES=$((DUPLICATES + 1)) ;;
        1) LEGACY_ONLY=$((LEGACY_ONLY + 1)) ;;
        2) MIGRATED=$((MIGRATED + 1)) ;;
    esac
    echo ""
done

echo ""
echo "2. SUMMARY"
echo "----------"
echo -e "${GREEN}✅ Migrated:${NC}    $MIGRATED packages"
echo -e "${YELLOW}⚠️  Duplicates:${NC}  $DUPLICATES packages"
echo -e "${RED}❌ Legacy Only:${NC} $LEGACY_ONLY packages"
echo ""

# Check imports from new plugins back to legacy
echo "3. REVERSE DEPENDENCIES (New plugins importing from legacy)"
echo "-----------------------------------------------------------"
echo ""

FOUND_REVERSE_DEPS=false

for plugin in "$CORE" "com.cloudempiere.ai.sales" "com.cloudempiere.ai.inventory" \
              "com.cloudempiere.ai.purchasing" "com.cloudempiere.ai.support" \
              "com.cloudempiere.ai.kb"; do
    if [ -d "$plugin/src" ]; then
        echo "Checking $plugin..."
        IMPORTS=$(grep -r "import com.cloudempiere.ai\." "$plugin/src" 2>/dev/null | \
                  grep -v "import com.cloudempiere.ai.provider" | \
                  grep -v "import com.cloudempiere.ai.database" | \
                  grep -v "import com.cloudempiere.ai.context" | \
                  grep -v "import com.cloudempiere.ai.boundary" | \
                  grep -v "import com.cloudempiere.ai.orchestrator" | \
                  grep -v "import com.cloudempiere.ai.util" | \
                  grep -v "import com.cloudempiere.ai.model" || true)

        if [ ! -z "$IMPORTS" ]; then
            echo -e "${RED}⚠️  Found imports from legacy plugin:${NC}"
            echo "$IMPORTS" | head -5
            echo ""
            FOUND_REVERSE_DEPS=true
        else
            echo -e "${GREEN}✅ No legacy imports${NC}"
        fi
    fi
done

echo ""

if [ "$FOUND_REVERSE_DEPS" = false ]; then
    echo -e "${GREEN}✅ No new plugins import from legacy plugin${NC}"
else
    echo -e "${RED}❌ Some new plugins still import from legacy${NC}"
fi

echo ""

# Count unique files in legacy plugin
echo "4. LEGACY PLUGIN INVENTORY"
echo "--------------------------"
echo ""

TOTAL_FILES=$(find "$LEGACY/src" -name "*.java" | wc -l)
echo "Total Java files in legacy plugin: $TOTAL_FILES"
echo ""

# Check what's unique to legacy
echo "Unique packages (not in core):"
for dir in "$LEGACY/src/com/cloudempiere/ai"/*; do
    if [ -d "$dir" ]; then
        pkg=$(basename "$dir")
        if [ ! -d "$CORE/src/com/cloudempiere/ai/$pkg" ]; then
            file_count=$(find "$dir" -name "*.java" | wc -l)
            echo "  - $pkg ($file_count files)"
        fi
    fi
done

echo ""

# Check if legacy plugin is still active
echo "5. DEPLOYMENT STATUS"
echo "--------------------"
echo ""

if [ -f "$LEGACY/target/com.cloudempiere.ai-*.jar" ]; then
    echo -e "${YELLOW}⚠️  Legacy plugin JAR exists in target/${NC}"
    ls -lh "$LEGACY/target/"*.jar 2>/dev/null || true
else
    echo -e "${GREEN}✅ No legacy plugin JAR in target/${NC}"
fi

echo ""

# Check pom.xml
if grep -q "<module>com.cloudempiere.ai.plugin</module>" pom.xml 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Legacy plugin still in ROOT pom.xml${NC}"
else
    echo -e "${GREEN}✅ Legacy plugin removed from ROOT pom.xml${NC}"
fi

echo ""
echo "========================================="
echo "RECOMMENDATIONS"
echo "========================================="
echo ""

if [ $DUPLICATES -gt 0 ]; then
    echo -e "${YELLOW}1. Delete duplicate packages from legacy plugin:${NC}"
    echo "   Packages that exist in both locations should be removed from legacy"
    echo ""
fi

if [ $LEGACY_ONLY -gt 0 ]; then
    echo -e "${RED}2. Migrate legacy-only packages:${NC}"
    echo "   The following packages need migration:"
    echo "   - rag/ → core/rag/"
    echo "   - guardrails/ → core/guardrails/"
    echo "   - observability/ → core/observability/"
    echo "   - kb/ → kb/kb/ or core/kb/"
    echo ""
fi

if [ "$FOUND_REVERSE_DEPS" = true ]; then
    echo -e "${RED}3. Fix reverse dependencies:${NC}"
    echo "   New plugins should not import from legacy plugin"
    echo "   Update imports to use new plugin packages"
    echo ""
fi

echo -e "${GREEN}4. Safe next steps:${NC}"
echo "   a. Keep legacy plugin as-is for now (deprecated)"
echo "   b. Run unit tests with both plugins active"
echo "   c. Gradually migrate unique packages"
echo "   d. Only remove after 100% validation"
echo ""

echo "========================================="
echo "Validation complete!"
echo "========================================="
