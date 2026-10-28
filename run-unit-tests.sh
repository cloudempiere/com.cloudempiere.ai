#!/bin/bash
# Run AI plugin unit tests from CLI
# Tests are located in com.cloudempiere.ai.test (local test bundle)
# Requires iDempiere classes for CLogger, etc.
#
# Usage:
#   ./run-unit-tests.sh                           # Run all unit tests
#   ./run-unit-tests.sh AIRequestTest             # Run specific test class
#   ./run-unit-tests.sh -v                        # Run all tests with verbose logging
#   ./run-unit-tests.sh -v AIMessageTest          # Run specific test with verbose logging
#   ./run-unit-tests.sh --integration             # Run integration tests (requires API keys)
#   ./run-unit-tests.sh --all                     # Run all tests (unit + integration)
#
# Testing Pyramid Profiles:
#   Default: unit tests only (fast, no external deps)
#   --integration: tests requiring external APIs (Anthropic, etc.)
#   --all: all tests

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Parse arguments
VERBOSE=false
TEST_FILTER=""
TEST_PROFILE="unit"

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --integration)
            TEST_PROFILE="integration"
            shift
            ;;
        --all)
            TEST_PROFILE="all"
            shift
            ;;
        *)
            TEST_FILTER="$1"
            shift
            ;;
    esac
done

# Java 17 (iDempiere v14). JAVA_HOME may point at the Java 11 used for v10 work: then look for a Java 17.
java_major() {
    "$1/bin/java" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]) }'
}
JAVA_MAJOR=""
[ -n "$JAVA_HOME" ] && JAVA_MAJOR=$(java_major "$JAVA_HOME")
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
    JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
    if [ -z "$JAVA_17_HOME" ] && [ -d "/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home" ]; then
        JAVA_17_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
    fi
    if [ -z "$JAVA_17_HOME" ]; then
        echo "ERROR: Java 17 not found. Please install Java 17 (Corretto) or set JAVA_HOME to it."
        exit 1
    fi
    [ -n "$JAVA_HOME" ] && echo "JAVA_HOME is Java ${JAVA_MAJOR:-unknown}, using Java 17 instead"
    export JAVA_HOME="$JAVA_17_HOME"
fi

echo "=== AI Plugin Unit Test Runner ==="
echo "Java: $JAVA_HOME"
echo "Profile: $TEST_PROFILE"
if [ "$VERBOSE" = true ]; then
    echo "Mode: VERBOSE (detailed logging enabled)"
fi
echo ""

# Directories
# iDempiereCLDE keeps upstream iDempiere under core/ since the Phase 2.R restructure
IDEMPIERE_DIR="$SCRIPT_DIR/../iDempiereCLDE/core"
[ -d "$IDEMPIERE_DIR" ] || IDEMPIERE_DIR="$SCRIPT_DIR/../iDempiereCLDE"
TEST_BUNDLE_DIR="$SCRIPT_DIR/com.cloudempiere.ai.test"
TEST_SRC_DIR="$TEST_BUNDLE_DIR/src"
LIB_DIR="$SCRIPT_DIR/lib"
BUILD_DIR="$SCRIPT_DIR/target/test-classes"
# Multi-module project - collect all module target/classes directories
CLASSES_DIRS=(
    "$SCRIPT_DIR/com.cloudempiere.ai.core/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.plugin/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.deps/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.sales/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.purchasing/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.support/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.inventory/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.kb/target/classes"
    "$SCRIPT_DIR/com.cloudempiere.ai.theme/target/classes"
)

# Check test bundle exists
if [ ! -d "$TEST_SRC_DIR" ]; then
    echo "ERROR: Test source directory not found: $TEST_SRC_DIR"
    echo "       Make sure com.cloudempiere.ai.test bundle is set up."
    exit 1
fi

# Download test dependencies if not present
JUNIT_VERSION="5.10.2"
ASSERTJ_VERSION="3.25.3"
TEST_LIB_DIR="$SCRIPT_DIR/target/test-lib"

mkdir -p "$TEST_LIB_DIR"
# start clean: classes of tests deleted or only present on another branch would still be scanned and run
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Download JUnit and AssertJ jars if needed
download_if_missing() {
    local file="$1"
    local url="$2"
    if [ ! -f "$TEST_LIB_DIR/$file" ]; then
        echo "Downloading $file..."
        curl -sL -o "$TEST_LIB_DIR/$file" "$url"
    fi
}

download_if_missing "junit-platform-console-standalone-1.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
download_if_missing "assertj-core-$ASSERTJ_VERSION.jar" \
    "https://repo1.maven.org/maven2/org/assertj/assertj-core/$ASSERTJ_VERSION/assertj-core-$ASSERTJ_VERSION.jar"
# Imported from the target platform, not embedded: Caffeine by org.adempiere.base (CacheMgt; PO and Query need it),
# the SLF4J API by LangChain4j
CAFFEINE_VERSION="3.1.8"
download_if_missing "caffeine-$CAFFEINE_VERSION.jar" \
    "https://repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/$CAFFEINE_VERSION/caffeine-$CAFFEINE_VERSION.jar"
SLF4J_VERSION="1.7.36"
download_if_missing "slf4j-api-$SLF4J_VERSION.jar" \
    "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/$SLF4J_VERSION/slf4j-api-$SLF4J_VERSION.jar"

# Jars a bundle declares in its MANIFEST Bundle-ClassPath. Its lib/ can also hold stale versions of the same
# libraries (e.g. LangChain4j 0.35.0 next to 1.20.0), so the directory is not globbed.
bundle_classpath_jars() {
    local bundle="$1"
    local manifest="$bundle/META-INF/MANIFEST.MF"
    [ -f "$manifest" ] || return 0
    # join the manifest's continuation lines (they start with one space) before reading the header
    tr -d '\r' < "$manifest" \
        | awk '/^ / { h = h substr($0, 2); next } { if (h ~ /^Bundle-ClassPath:/) print h; h = $0 } END { if (h ~ /^Bundle-ClassPath:/) print h }' \
        | sed 's/^Bundle-ClassPath://' | tr -d ' ' | tr ',' '\n' | grep '\.jar$' \
        | while read -r jar; do
            [ -f "$bundle/$jar" ] && echo "$bundle/$jar"
        done
    return 0
}

# Build classpath - main classes first from all modules
CLASSPATH=""
for classes_dir in "${CLASSES_DIRS[@]}"; do
    if [ -d "$classes_dir" ]; then
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$classes_dir"
        else
            CLASSPATH="$CLASSPATH:$classes_dir"
        fi
    fi
done

# Add AI plugin lib jars (from root lib/ if it exists)
if [ -d "$LIB_DIR" ]; then
    for jar in "$LIB_DIR"/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

# Add the embedded jars of each module bundle (LangChain4j in deps, AWS SDK in core, ...)
for classes_dir in "${CLASSES_DIRS[@]}"; do
    for jar in $(bundle_classpath_jars "${classes_dir%/target/classes}"); do
        CLASSPATH="$CLASSPATH:$jar"
    done
done

# Add iDempiere base classes (needed for CLogger, DB, etc.)
IDEMPIERE_BASE_CLASSES="$IDEMPIERE_DIR/org.adempiere.base/target/classes"
if [ -d "$IDEMPIERE_BASE_CLASSES" ]; then
    CLASSPATH="$CLASSPATH:$IDEMPIERE_BASE_CLASSES"
else
    # Try using the p2 repository jar
    IDEMPIERE_BASE_JAR=$(find "$IDEMPIERE_DIR/org.idempiere.p2/target/repository/plugins" -name "org.adempiere.base_*.jar" 2>/dev/null | head -1)
    if [ -n "$IDEMPIERE_BASE_JAR" ]; then
        CLASSPATH="$CLASSPATH:$IDEMPIERE_BASE_JAR"
    else
        echo "WARNING: iDempiere base classes not found. Some tests may fail."
    fi
fi

# Add iDempiere base lib jars (JSON, etc.)
IDEMPIERE_BASE_LIB="$IDEMPIERE_DIR/org.adempiere.base/lib"
if [ -d "$IDEMPIERE_BASE_LIB" ]; then
    for jar in "$IDEMPIERE_BASE_LIB"/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

# Add test lib jars
for jar in "$TEST_LIB_DIR"/*.jar; do
    [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
done

# Check if main classes are compiled (at least one module should have compiled classes)
FOUND_CLASSES=false
for classes_dir in "${CLASSES_DIRS[@]}"; do
    if [ -d "$classes_dir" ] && [ -n "$(ls -A $classes_dir 2>/dev/null)" ]; then
        FOUND_CLASSES=true
        break
    fi
done

if [ "$FOUND_CLASSES" = false ]; then
    echo "ERROR: Main classes not compiled. Run 'mvn compile' first or build in Eclipse."
    echo "       Looking for classes in module target/classes directories"
    exit 1
fi

echo "Compiling tests..."

# Find Java files based on test profile
if [ "$TEST_PROFILE" = "unit" ]; then
    # Unit tests only (fast, no external deps)
    # Include: /unit/ directory, test categories, and test support utilities
    # Exclude: /integration/, /e2e/, /component/ (component tests may need ZK framework)
    ALL_JAVA_FILES=$(find "$TEST_SRC_DIR" -name "*.java" 2>/dev/null | \
        grep -E "/(unit|categories|support)/" | \
        grep -v "/component/" || true)
elif [ "$TEST_PROFILE" = "integration" ]; then
    # Integration tests (require API keys, database, etc.)
    ALL_JAVA_FILES=$(find "$TEST_SRC_DIR" -name "*.java" 2>/dev/null || true)
else
    # All tests
    ALL_JAVA_FILES=$(find "$TEST_SRC_DIR" -name "*.java" 2>/dev/null || true)
fi

if [ -z "$ALL_JAVA_FILES" ]; then
    echo "No Java files found in $TEST_SRC_DIR"
    exit 1
fi

echo "Found Java files:"
echo "$ALL_JAVA_FILES" | while read f; do echo "  - $(basename $f)"; done
echo ""

# Compile all Java files (tests + utilities)
"$JAVA_HOME/bin/javac" -d "$BUILD_DIR" \
    -cp "$CLASSPATH" \
    --release 17 \
    $ALL_JAVA_FILES

# OSGi component descriptors sit at the bundle root, not in target/classes; tests read them as resources
for classes_dir in "${CLASSES_DIRS[@]}"; do
    osgi_inf="${classes_dir%/target/classes}/OSGI-INF"
    if [ -d "$osgi_inf" ]; then
        mkdir -p "$BUILD_DIR/OSGI-INF"
        find "$osgi_inf" -maxdepth 1 -name '*.xml' -exec cp {} "$BUILD_DIR/OSGI-INF/" \;
    fi
done

echo "Compilation successful!"
echo ""

# Run tests using JUnit Platform Console
echo "Running tests..."
echo "================="

# Build JUnit command with optional verbose flag and tag filtering
JAVA_OPTS=""
DETAILS_MODE="tree"
TAG_OPTS=""

if [ "$VERBOSE" = true ]; then
    JAVA_OPTS="-Dtest.verbose=true -Dtest.debug=true"
    DETAILS_MODE="verbose"
fi

# Apply JUnit 5 tag filtering based on profile
if [ "$TEST_PROFILE" = "unit" ]; then
    TAG_OPTS="--include-tag unit --exclude-tag integration --exclude-tag e2e"
elif [ "$TEST_PROFILE" = "integration" ]; then
    TAG_OPTS="--include-tag integration"
fi

JUNIT_CMD="$JAVA_HOME/bin/java $JAVA_OPTS -jar $TEST_LIB_DIR/junit-platform-console-standalone-1.10.2.jar --classpath $BUILD_DIR:$CLASSPATH"

if [ -n "$TEST_FILTER" ]; then
    # Run specific test class or pattern
    echo "Filter: $TEST_FILTER"
    $JUNIT_CMD \
        --select-class "com.cloudempiere.ai.test.${TEST_FILTER}" \
        $TAG_OPTS \
        --details $DETAILS_MODE 2>/dev/null || \
    $JUNIT_CMD \
        --scan-class-path "$BUILD_DIR" \
        --include-classname ".*${TEST_FILTER}.*" \
        $TAG_OPTS \
        --details $DETAILS_MODE
else
    # Run all tests matching profile
    $JUNIT_CMD \
        --scan-class-path "$BUILD_DIR" \
        --include-classname ".*Test$" \
        $TAG_OPTS \
        --details $DETAILS_MODE
fi

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "=== All Tests Passed ==="
else
    echo "=== Some Tests Failed ==="
fi

exit $EXIT_CODE
