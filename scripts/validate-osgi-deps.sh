#!/bin/bash
###############################################################################
# OSGi Dependency Validator
#
# Analyzes embedded JARs in lib/ directories and validates:
# 1. All JARs are in Bundle-ClassPath
# 2. All packages from JARs are exported (if OSGi bundles)
# 3. Detects missing transitive dependencies
#
# Usage: ./validate-osgi-deps.sh <bundle-dir>
# Example: ./validate-osgi-deps.sh com.cloudempiere.ai.deps
###############################################################################

set -e

BUNDLE_DIR="${1:-.}"
LIB_DIR="${BUNDLE_DIR}/lib"
MANIFEST="${BUNDLE_DIR}/META-INF/MANIFEST.MF"

if [[ ! -d "$LIB_DIR" ]]; then
    echo "❌ No lib/ directory found in $BUNDLE_DIR"
    exit 1
fi

if [[ ! -f "$MANIFEST" ]]; then
    echo "❌ No MANIFEST.MF found in $BUNDLE_DIR"
    exit 1
fi

echo "🔍 Validating OSGi dependencies for: $BUNDLE_DIR"
echo ""

# Parse MANIFEST.MF (handle line continuations with leading spaces)
# Join continuation lines (lines starting with space)
MANIFEST_PARSED=$(awk '
    /^[^ ]/ {
        if (line) print line;
        line=$0;
        next
    }
    {
        gsub(/^ +/, "");
        line=line $0
    }
    END {
        if (line) print line
    }
' "$MANIFEST")

# Extract Bundle-ClassPath
echo "📋 Bundle-ClassPath entries:"
BUNDLE_CLASSPATH=$(echo "$MANIFEST_PARSED" | grep "^Bundle-ClassPath:" | sed 's/^Bundle-ClassPath: *//' | tr ',' '\n')
echo "$BUNDLE_CLASSPATH" | head -10
echo ""

# Extract Export-Package
echo "📦 Export-Package entries:"
EXPORT_PACKAGE=$(echo "$MANIFEST_PARSED" | grep "^Export-Package:" | sed 's/^Export-Package: *//')
echo "$EXPORT_PACKAGE" | tr ',' '\n' | head -20
echo ""

# Check each JAR
echo "🔎 Analyzing JARs in lib/..."
echo ""

MISSING_IN_CLASSPATH=()
MISSING_EXPORTS=()
OSGI_BUNDLES=()

for jar in "$LIB_DIR"/*.jar; do
    jar_name=$(basename "$jar")

    # Check if in Bundle-ClassPath
    if ! echo "$BUNDLE_CLASSPATH" | grep -q "$jar_name"; then
        MISSING_IN_CLASSPATH+=("$jar_name")
    fi

    # Extract MANIFEST.MF from JAR to check if it's an OSGi bundle
    TEMP_MANIFEST=$(mktemp)
    unzip -p "$jar" META-INF/MANIFEST.MF > "$TEMP_MANIFEST" 2>/dev/null || true

    if grep -q "Export-Package:" "$TEMP_MANIFEST" 2>/dev/null; then
        OSGI_BUNDLES+=("$jar_name")

        # Extract exported packages from the JAR
        JAR_EXPORTS=$(grep -A 100 "^Export-Package:" "$TEMP_MANIFEST" | sed '/^[^ ]/q' | grep -v "^Export-Package:" | sed 's/;.*//' | tr -d ' ' | sort -u)

        # Check if these packages are re-exported by our bundle
        for pkg in $JAR_EXPORTS; do
            if [[ -n "$pkg" ]] && ! echo "$EXPORT_PACKAGE" | grep -q "$pkg"; then
                MISSING_EXPORTS+=("$pkg (from $jar_name)")
            fi
        done
    fi

    # Check for Import-Package (transitive dependencies)
    if grep -q "Import-Package:" "$TEMP_MANIFEST" 2>/dev/null; then
        JAR_IMPORTS=$(grep -A 100 "^Import-Package:" "$TEMP_MANIFEST" | sed '/^[^ ]/q' | grep -v "^Import-Package:" | sed 's/;.*//' | tr -d ' ' | sort -u)

        for pkg in $JAR_IMPORTS; do
            if [[ -n "$pkg" ]] && ! echo "$EXPORT_PACKAGE" | grep -q "$pkg" && ! echo "$BUNDLE_CLASSPATH" | grep -q "slf4j" 2>/dev/null; then
                # This is a transitive dependency we might need
                # (We skip some obvious ones like javax.* which come from JDK)
                if [[ ! "$pkg" =~ ^javax\. ]] && [[ ! "$pkg" =~ ^java\. ]] && [[ ! "$pkg" =~ ^org\.xml\. ]]; then
                    echo "  ⚠️  $jar_name imports: $pkg (may need to be provided)"
                fi
            fi
        done
    fi

    rm -f "$TEMP_MANIFEST"
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "VALIDATION RESULTS"
echo "════════════════════════════════════════════════════════════"
echo ""

# Report missing in Bundle-ClassPath
if [[ ${#MISSING_IN_CLASSPATH[@]} -gt 0 ]]; then
    echo "❌ JARs NOT in Bundle-ClassPath:"
    for jar in "${MISSING_IN_CLASSPATH[@]}"; do
        echo "   - $jar"
    done
    echo ""
else
    echo "✅ All JARs are in Bundle-ClassPath"
    echo ""
fi

# Report OSGi bundles found
if [[ ${#OSGI_BUNDLES[@]} -gt 0 ]]; then
    echo "📦 OSGi bundles detected (${#OSGI_BUNDLES[@]}):"
    for jar in "${OSGI_BUNDLES[@]}"; do
        echo "   - $jar"
    done
    echo ""
fi

# Report missing exports
if [[ ${#MISSING_EXPORTS[@]} -gt 0 ]]; then
    echo "⚠️  Packages NOT re-exported (${#MISSING_EXPORTS[@]}):"
    for pkg in "${MISSING_EXPORTS[@]}"; do
        echo "   - $pkg"
    done
    echo ""
    echo "💡 Consider adding these to Export-Package in MANIFEST.MF"
    echo "   if they need to be accessible to other bundles."
    echo ""
else
    echo "✅ All OSGi bundle packages are re-exported"
    echo ""
fi

echo "════════════════════════════════════════════════════════════"
echo ""

# Exit with error if critical issues found
if [[ ${#MISSING_IN_CLASSPATH[@]} -gt 0 ]]; then
    echo "❌ CRITICAL: Fix Bundle-ClassPath issues before deployment"
    exit 1
fi

echo "✅ Validation passed!"
exit 0
