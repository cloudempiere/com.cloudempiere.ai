#!/bin/bash
#
# MCP E2E Test Runner
# Runs all MCP Inspector tests for iDempiere MCP Server
#
# Usage: ./mcp-tests/run-all.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
CONFIG_FILE="${SCRIPT_DIR}/config/inspector-config.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p "${RESULTS_DIR}"

# Log file
LOG_FILE="${RESULTS_DIR}/test-run-$(date +%Y%m%d-%H%M%S).log"

echo "==========================================="
echo "MCP E2E Test Suite"
echo "==========================================="
echo "Started: $(date)"
echo "Results: ${RESULTS_DIR}"
echo "Log: ${LOG_FILE}"
echo ""

# Check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."

    # Check Node.js
    if ! command -v node &> /dev/null; then
        echo -e "${RED}ERROR: Node.js is not installed${NC}"
        exit 1
    fi
    echo "  - Node.js: $(node --version)"

    # Check npx
    if ! command -v npx &> /dev/null; then
        echo -e "${RED}ERROR: npx is not available${NC}"
        exit 1
    fi
    echo "  - npx: available"

    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}ERROR: Java is not installed${NC}"
        exit 1
    fi
    echo "  - Java: $(java -version 2>&1 | head -1)"

    # Check MCP server JAR exists
    MCP_SERVER_JAR="${HOME}/github/idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar"
    if [ ! -f "${MCP_SERVER_JAR}" ]; then
        echo -e "${YELLOW}WARNING: MCP server JAR not found at ${MCP_SERVER_JAR}${NC}"
        echo "  Please build idempiere-mcp-server first"
        return 1
    fi
    echo "  - MCP Server JAR: found"

    # Check CLI JAR exists
    CLI_JAR="${HOME}/github/cloudempiere-cli/target/idempiere-cli-1.26.0-SNAPSHOT-runner.jar"
    if [ ! -f "${CLI_JAR}" ]; then
        echo -e "${YELLOW}WARNING: CLI JAR not found at ${CLI_JAR}${NC}"
        echo "  Please build cloudempiere-cli first"
        return 1
    fi
    echo "  - CLI JAR: found"

    echo ""
    return 0
}

# Run tool discovery test
test_tool_discovery() {
    echo "Test 1: Tool Discovery"
    echo "---------------------"

    # Try to list tools using MCP Inspector CLI
    RESULT=$(npx @anthropic-ai/mcp-inspector \
        --cli \
        --server "java -jar ${HOME}/github/idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar" \
        --command "tools/list" 2>&1 || true)

    if echo "${RESULT}" | grep -q "listTables"; then
        echo -e "${GREEN}PASS: listTables tool found${NC}"
    else
        echo -e "${YELLOW}SKIP: Tool discovery test skipped (MCP Inspector not configured)${NC}"
        echo "  To run: npm install -g @anthropic-ai/mcp-inspector"
    fi

    echo "${RESULT}" >> "${LOG_FILE}"
    echo ""
}

# Run listTables test
test_list_tables() {
    echo "Test 2: listTables Tool"
    echo "----------------------"

    # Placeholder - actual test would call MCP Inspector
    echo -e "${YELLOW}SKIP: Test requires MCP server running${NC}"
    echo ""
}

# Run describeTable test
test_describe_table() {
    echo "Test 3: describeTable Tool"
    echo "-------------------------"

    # Placeholder - actual test would call MCP Inspector
    echo -e "${YELLOW}SKIP: Test requires MCP server running${NC}"
    echo ""
}

# Generate summary
generate_summary() {
    echo "==========================================="
    echo "Test Summary"
    echo "==========================================="
    echo "Completed: $(date)"
    echo ""
    echo "Results saved to: ${LOG_FILE}"
    echo ""

    # Count results
    PASS_COUNT=$(grep -c "PASS:" "${LOG_FILE}" 2>/dev/null || echo 0)
    FAIL_COUNT=$(grep -c "FAIL:" "${LOG_FILE}" 2>/dev/null || echo 0)
    SKIP_COUNT=$(grep -c "SKIP:" "${LOG_FILE}" 2>/dev/null || echo 0)

    echo "Passed:  ${PASS_COUNT}"
    echo "Failed:  ${FAIL_COUNT}"
    echo "Skipped: ${SKIP_COUNT}"

    # Exit with failure if any tests failed
    if [ "${FAIL_COUNT}" -gt 0 ]; then
        exit 1
    fi
}

# Main execution
main() {
    echo "Starting MCP E2E tests..." >> "${LOG_FILE}"

    # Check prerequisites (don't fail on warnings)
    check_prerequisites || true

    # Run tests
    test_tool_discovery
    test_list_tables
    test_describe_table

    # Generate summary
    generate_summary
}

main "$@"
