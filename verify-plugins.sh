#!/bin/bash
# Verify domain plugins structure

echo "Verifying domain plugins..."
echo ""

PLUGINS="sales inventory purchasing support kb"
ERRORS=0

for plugin in $PLUGINS; do
    echo "=== Checking com.cloudempiere.ai.$plugin ==="

    # Check directory structure
    if [ -d "com.cloudempiere.ai.$plugin" ]; then
        echo "✓ Plugin directory exists"
    else
        echo "✗ Plugin directory missing"
        ERRORS=$((ERRORS + 1))
        continue
    fi

    # Check source files
    BOUNDARY="com.cloudempiere.ai.$plugin/src/com/cloudempiere/ai/$plugin/boundary"
    TOOLS="com.cloudempiere.ai.$plugin/src/com/cloudempiere/ai/$plugin/tools"
    AGENT="com.cloudempiere.ai.$plugin/src/com/cloudempiere/ai/$plugin/agent"

    if [ -f "${BOUNDARY}/${plugin^}DomainBoundary.java" ]; then
        echo "✓ Boundary class exists"
    else
        echo "✗ Boundary class missing"
        ERRORS=$((ERRORS + 1))
    fi

    if [ -f "${TOOLS}/${plugin^}Tools.java" ]; then
        echo "✓ Tools class exists"
    else
        echo "✗ Tools class missing"
        ERRORS=$((ERRORS + 1))
    fi

    if [ -f "${AGENT}/${plugin^}Agent.java" ]; then
        echo "✓ Agent class exists"
    else
        echo "✗ Agent class missing"
        ERRORS=$((ERRORS + 1))
    fi

    # Check config files
    if [ -f "com.cloudempiere.ai.$plugin/META-INF/MANIFEST.MF" ]; then
        echo "✓ MANIFEST.MF exists"
    else
        echo "✗ MANIFEST.MF missing"
        ERRORS=$((ERRORS + 1))
    fi

    if [ -f "com.cloudempiere.ai.$plugin/pom.xml" ]; then
        echo "✓ pom.xml exists"
    else
        echo "✗ pom.xml missing"
        ERRORS=$((ERRORS + 1))
    fi

    # Check OSGI-INF
    OSGI_COUNT=$(find "com.cloudempiere.ai.$plugin/OSGI-INF" -name "*.xml" 2>/dev/null | wc -l)
    if [ "$OSGI_COUNT" -ge 2 ]; then
        echo "✓ OSGI-INF XML files exist ($OSGI_COUNT files)"
    else
        echo "✗ OSGI-INF XML files missing or incomplete"
        ERRORS=$((ERRORS + 1))
    fi

    echo ""
done

# Check feature.xml
echo "=== Checking feature.xml ==="
if grep -q "com.cloudempiere.ai.sales" com.cloudempiere.ai.feature/feature.xml && \
   grep -q "com.cloudempiere.ai.inventory" com.cloudempiere.ai.feature/feature.xml && \
   grep -q "com.cloudempiere.ai.purchasing" com.cloudempiere.ai.feature/feature.xml && \
   grep -q "com.cloudempiere.ai.support" com.cloudempiere.ai.feature/feature.xml && \
   grep -q "com.cloudempiere.ai.kb" com.cloudempiere.ai.feature/feature.xml; then
    echo "✓ All domain plugins referenced in feature.xml"
else
    echo "✗ Some domain plugins missing from feature.xml"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "======================================"
if [ $ERRORS -eq 0 ]; then
    echo "✓ All checks passed!"
    echo "Domain plugins are ready for building"
    exit 0
else
    echo "✗ $ERRORS errors found"
    echo "Please fix the issues above"
    exit 1
fi
