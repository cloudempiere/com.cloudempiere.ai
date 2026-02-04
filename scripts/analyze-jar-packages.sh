#!/bin/bash
###############################################################################
# Analyze JAR Packages - Detect ALL missing OSGi exports/imports
#
# Extracts all packages from JARs and compares with MANIFEST.MF
# to find missing exports and required imports.
#
# Usage: ./analyze-jar-packages.sh <bundle-dir>
# Example: ./analyze-jar-packages.sh com.cloudempiere.ai.deps
###############################################################################

set -e

BUNDLE_DIR="${1:-.}"
LIB_DIR="${BUNDLE_DIR}/lib"
MANIFEST="${BUNDLE_DIR}/META-INF/MANIFEST.MF"
TEMP_DIR="/tmp/jar-analysis-$$"

if [[ ! -d "$LIB_DIR" ]]; then
    echo "❌ No lib/ directory found in $BUNDLE_DIR"
    exit 1
fi

mkdir -p "$TEMP_DIR"

echo "🔍 Analyzing all packages in JARs..."
echo ""

# Parse MANIFEST.MF (handle line continuations)
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

EXPORT_PACKAGE=$(echo "$MANIFEST_PARSED" | grep "^Export-Package:" | sed 's/^Export-Package: *//' || true)

echo "📦 Scanning JARs and extracting packages..."
ALL_PACKAGES_FILE="$TEMP_DIR/all_packages.txt"
> "$ALL_PACKAGES_FILE"

for jar in "$LIB_DIR"/*.jar; do
    jar_name=$(basename "$jar")
    echo "  Analyzing: $jar_name"

    # Extract all .class files and derive package names
    unzip -l "$jar" 2>/dev/null | \
        grep "\.class$" | \
        awk '{print $NF}' | \
        sed 's|\.class$||' | \
        sed 's|/[^/]*$||' | \
        tr '/' '.' | \
        sort -u | \
        while read pkg; do
            # Skip empty, META-INF, module-info
            if [[ -n "$pkg" ]] && [[ "$pkg" != "META-INF" ]] && [[ "$pkg" != "module-info" ]]; then
                echo "$pkg"
            fi
        done >> "$ALL_PACKAGES_FILE"
done

# Deduplicate and sort
sort -u "$ALL_PACKAGES_FILE" > "$TEMP_DIR/unique_packages.txt"
TOTAL_PACKAGES=$(wc -l < "$TEMP_DIR/unique_packages.txt" | tr -d ' ')

echo ""
echo "════════════════════════════════════════════════════════════"
echo "ANALYSIS RESULTS"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "📊 Total unique packages found in JARs: $TOTAL_PACKAGES"
echo ""

# Find packages NOT exported
echo "🔍 Checking which packages are NOT exported..."
echo ""
MISSING_EXPORTS="$TEMP_DIR/missing_exports.txt"
> "$MISSING_EXPORTS"

while read pkg; do
    if ! echo "$EXPORT_PACKAGE" | grep -q "$pkg"; then
        echo "$pkg" >> "$MISSING_EXPORTS"
    fi
done < "$TEMP_DIR/unique_packages.txt"

MISSING_COUNT=$(wc -l < "$MISSING_EXPORTS" | tr -d ' ')

if [[ $MISSING_COUNT -gt 0 ]]; then
    echo "⚠️  Found $MISSING_COUNT packages NOT exported:"
    echo ""

    # Group by top-level package
    echo "By Library:"
    cat "$MISSING_EXPORTS" | awk -F. '{print $1"."$2"."$3}' | sort -u | while read prefix; do
        count=$(grep "^$prefix" "$MISSING_EXPORTS" | wc -l | tr -d ' ')
        echo "  $prefix.* ($count packages)"
    done
    echo ""

    # Generate Export-Package entries
    echo "════════════════════════════════════════════════════════════"
    echo "SUGGESTED Export-Package additions for MANIFEST.MF:"
    echo "════════════════════════════════════════════════════════════"
    echo ""

    # Detect common AWS SDK packages
    if grep -q "^software.amazon.awssdk" "$MISSING_EXPORTS"; then
        echo "# AWS SDK packages (Bedrock/S3):"
        grep "^software.amazon.awssdk" "$MISSING_EXPORTS" | \
            grep -v "\.internal" | \
            head -20 | \
            while read pkg; do
                echo " $pkg;version=\"2.20.162\","
            done
        echo ""
    fi

    # Detect Netty packages
    if grep -q "^io.netty" "$MISSING_EXPORTS"; then
        echo "# Netty packages:"
        grep "^io.netty" "$MISSING_EXPORTS" | \
            grep -v "\.internal" | \
            head -10 | \
            while read pkg; do
                echo " $pkg;version=\"4.1.100\","
            done
        echo ""
    fi

    # Detect Jackson packages
    if grep -q "^com.fasterxml.jackson" "$MISSING_EXPORTS"; then
        echo "# Jackson packages:"
        grep "^com.fasterxml.jackson" "$MISSING_EXPORTS" | \
            head -10 | \
            while read pkg; do
                echo " $pkg;version=\"2.17.0\","
            done
        echo ""
    fi

    # Show all others
    echo "# Other packages (review if needed):"
    grep -v "^software.amazon.awssdk" "$MISSING_EXPORTS" | \
        grep -v "^io.netty" | \
        grep -v "^com.fasterxml.jackson" | \
        grep -v "\.internal" | \
        head -20 | \
        while read pkg; do
            echo " $pkg;version=\"???\","
        done

    echo ""
    echo "💡 Review these and add relevant ones to Export-Package"
    echo "   (Skip internal packages unless really needed)"
    echo ""
else
    echo "✅ All packages are exported!"
fi

# Cleanup
rm -rf "$TEMP_DIR"

echo "════════════════════════════════════════════════════════════"
