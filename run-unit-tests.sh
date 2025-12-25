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

# Java home
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home}"

echo "=== AI Plugin Unit Test Runner ==="
echo "Java: $JAVA_HOME"
echo "Profile: $TEST_PROFILE"
if [ "$VERBOSE" = true ]; then
    echo "Mode: VERBOSE (detailed logging enabled)"
fi
echo ""

# Directories
IDEMPIERE_DIR="$SCRIPT_DIR/../iDempiereCLDE"
TEST_BUNDLE_DIR="$SCRIPT_DIR/com.cloudempiere.ai.test"
TEST_SRC_DIR="$TEST_BUNDLE_DIR/src"
LIB_DIR="$SCRIPT_DIR/lib"
BUILD_DIR="$SCRIPT_DIR/target/test-classes"
CLASSES_DIR="$SCRIPT_DIR/target/classes"

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

# Build classpath - main classes first
CLASSPATH="$CLASSES_DIR"

# Add AI plugin lib jars
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
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

# Check if main classes are compiled
if [ ! -d "$CLASSES_DIR" ] || [ -z "$(ls -A $CLASSES_DIR 2>/dev/null)" ]; then
    echo "ERROR: Main classes not compiled. Run 'mvn compile' first or open in Eclipse."
    echo "       Looking for: $CLASSES_DIR"
    exit 1
fi

echo "Compiling tests..."

# Find Java files based on test profile
if [ "$TEST_PROFILE" = "unit" ]; then
    # Unit tests only (fast, no external deps)
    ALL_JAVA_FILES=$(find "$TEST_SRC_DIR" -name "*.java" 2>/dev/null | grep -v "/integration/" | grep -v "/e2e/" || true)
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
    -source 11 -target 11 \
    $ALL_JAVA_FILES

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
