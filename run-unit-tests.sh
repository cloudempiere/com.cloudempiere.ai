#!/bin/bash
# Run AI plugin unit tests from CLI
# Tests are located in org.idempiere.test plugin
# Requires iDempiere classes for CLogger, etc.
#
# Usage:
#   ./run-unit-tests.sh                    # Run all tests
#   ./run-unit-tests.sh APICreditsTest     # Run specific test class
#   ./run-unit-tests.sh InputGuard         # Run tests matching pattern
#   ./run-unit-tests.sh -v                 # Run all tests with verbose logging
#   ./run-unit-tests.sh -v APICreditsTest  # Run specific test with verbose logging

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Parse arguments
VERBOSE=false
TEST_FILTER=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            TEST_FILTER="$1"
            shift
            ;;
    esac
done

# Java home
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home}"

echo "=== AI Plugin Unit Test Runner ==="
echo "Java: $JAVA_HOME"
if [ "$VERBOSE" = true ]; then
    echo "Mode: VERBOSE (detailed logging enabled)"
fi
echo ""

# Directories
IDEMPIERE_DIR="$SCRIPT_DIR/../iDempiereCLDE"
TEST_DIR="$IDEMPIERE_DIR/org.idempiere.test/src/com/cloudempiere/ai"
LIB_DIR="$SCRIPT_DIR/lib"
BUILD_DIR="$SCRIPT_DIR/target/test-classes"
CLASSES_DIR="$SCRIPT_DIR/target/classes"

# Check test directory exists
if [ ! -d "$TEST_DIR" ]; then
    echo "ERROR: Test directory not found: $TEST_DIR"
    echo "       Make sure org.idempiere.test is checked out."
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
        echo "WARNING: iDempiere base classes not found. Tests may fail."
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

# Find all Java files in test dir (including utilities like TestLogger)
# Exclude tests that require iDempiere context (AbstractTestCase) - they need full OSGi setup
ALL_JAVA_FILES=$(find "$TEST_DIR" -name "*.java" 2>/dev/null | grep -v "AIProviderAuthorizationTest.java" || true)

if [ -z "$ALL_JAVA_FILES" ]; then
    echo "No Java files found in $TEST_DIR"
    exit 1
fi

echo "Found Java files (standalone tests only):"
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

# Build JUnit command with optional verbose flag
JAVA_OPTS=""
DETAILS_MODE="tree"

if [ "$VERBOSE" = true ]; then
    # Enable verbose logging via system property
    JAVA_OPTS="-Dtest.verbose=true -Dtest.debug=true"
    DETAILS_MODE="verbose"
fi

JUNIT_CMD="$JAVA_HOME/bin/java $JAVA_OPTS -jar $TEST_LIB_DIR/junit-platform-console-standalone-1.10.2.jar --classpath $BUILD_DIR:$CLASSPATH"

if [ -n "$TEST_FILTER" ]; then
    # Run specific test class or pattern
    echo "Filter: $TEST_FILTER"
    $JUNIT_CMD \
        --select-class "com.cloudempiere.ai.${TEST_FILTER}" \
        --details $DETAILS_MODE 2>/dev/null || \
    $JUNIT_CMD \
        --scan-class-path "$BUILD_DIR" \
        --include-classname ".*${TEST_FILTER}.*" \
        --details $DETAILS_MODE
else
    # Run all tests
    $JUNIT_CMD \
        --scan-class-path "$BUILD_DIR" \
        --include-classname ".*Test$" \
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
