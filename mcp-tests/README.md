# MCP E2E Tests

End-to-end tests for iDempiere MCP Server integration using [MCP Inspector](https://github.com/modelcontextprotocol/inspector).

## Overview

These tests validate the full stack integration:

```
Claude/External Agent → MCP Server → cloudempiere-cli → iDempiere Database
```

## Prerequisites

1. **Node.js 20+** - Required for MCP Inspector
2. **idempiere-mcp-server** - Built and available at `~/github/idempiere-mcp-server`
3. **cloudempiere-cli** - Built and available at `~/github/cloudempiere-cli`
4. **iDempiere Database** - Running PostgreSQL with iDempiere schema

## Installation

```bash
# Install MCP Inspector globally
npm install -g @anthropic-ai/mcp-inspector

# Or use npx (no installation required)
npx @anthropic-ai/mcp-inspector --help
```

## Directory Structure

```
mcp-tests/
├── README.md              # This file
├── run-all.sh             # Main test runner script
├── test-mcp-tools.sh      # Tool-specific tests
├── config/
│   └── inspector-config.json  # MCP Inspector configuration
├── fixtures/
│   ├── test-queries.json      # Test query inputs
│   └── expected-responses.json # Expected tool outputs
└── results/               # Test results (generated)
```

## Running Tests

### Run All Tests

```bash
./mcp-tests/run-all.sh
```

### Run Specific Tool Tests

```bash
./mcp-tests/test-mcp-tools.sh listTables
./mcp-tests/test-mcp-tools.sh describeTable
./mcp-tests/test-mcp-tools.sh chat
```

### Using MCP Inspector CLI Directly

```bash
# List available tools
npx @anthropic-ai/mcp-inspector \
  --cli \
  --server "java -jar ~/github/idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar" \
  --command "tools/list"

# Call a specific tool
npx @anthropic-ai/mcp-inspector \
  --cli \
  --server "java -jar ~/github/idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar" \
  --command "tools/call" \
  --params '{"name": "listTables", "arguments": {"pattern": "C_%"}}'
```

## Test Categories

### 1. Tool Discovery Tests

Verify all expected MCP tools are available:

- `listTables` - List Application Dictionary tables
- `describeTable` - Get table schema/columns
- `addTable` - Add new table definition
- `addColumn` - Add column to table
- `syncTable` - Synchronize table with database

### 2. Tool Execution Tests

Verify tools return expected results:

```json
// fixtures/test-queries.json
{
  "listTables": {
    "input": {"pattern": "C_Order%"},
    "expectedTables": ["C_Order", "C_OrderLine", "C_OrderTax"]
  }
}
```

### 3. Error Handling Tests

Verify proper error responses:

- Invalid table names
- Permission denied scenarios
- Malformed inputs

### 4. Integration Tests (Future)

Once AI tools are added to MCP server:

- `chat` - AI conversation with iDempiere context
- `query` - Secure database queries via AI
- `analyze` - Data analysis tasks

## Configuration

### inspector-config.json

```json
{
  "server": {
    "command": "java",
    "args": [
      "-jar",
      "../idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar"
    ],
    "env": {
      "IDEMPIERE_BACKEND": "cli",
      "IDEMPIERE_CLI_PATH": "../cloudempiere-cli/target/idempiere-cli-1.26.0-SNAPSHOT-runner.jar"
    }
  },
  "tests": {
    "timeout": 30000,
    "retries": 2
  }
}
```

## CI/CD Integration

MCP E2E tests run:

1. **On Release** - Full validation before deployment
2. **Manual Trigger** - Via GitHub Actions workflow dispatch
3. **Nightly** - Scheduled run for regression detection

### Environment Variables

Set in GitHub Secrets:

- `ANTHROPIC_API_KEY` - For AI tool tests
- `IDEMPIERE_DB_URL` - Database connection
- `IDEMPIERE_DB_USER` - Database user
- `IDEMPIERE_DB_PASSWORD` - Database password

## Troubleshooting

### MCP Server Not Starting

```bash
# Check Java version (requires 21+)
java -version

# Test server directly
java -jar ~/github/idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar
```

### CLI Backend Errors

```bash
# Test CLI directly
java -jar ~/github/cloudempiere-cli/target/idempiere-cli-1.26.0-SNAPSHOT-runner.jar tables --help
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
pg_isready -h localhost -p 5432

# Verify iDempiere schema
psql -h localhost -U adempiere -d idempiere -c "SELECT COUNT(*) FROM AD_Table"
```

## References

- [MCP Inspector GitHub](https://github.com/modelcontextprotocol/inspector)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [ADR-003: MCP Server Integration](../docs/adr/003-mcp-server-integration.md)
- [ADR-032: Testing Strategy](../docs/adr/032-testing-strategy.md)
