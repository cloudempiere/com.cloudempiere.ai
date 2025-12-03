# ADR-032: Testing Strategy and Framework

## Status

Accepted

## Date

2025-12-03

## Deciders

CloudEmpiere AI Team

## Context and Problem Statement

The com.cloudempiere.ai plugin requires a comprehensive, automated testing strategy to ensure reliability, security, and maintainability. Currently, we have basic JUnit 5 tests but lack:

1. Complete test coverage for critical components
2. Automated CI/CD pipeline
3. End-to-end testing for MCP integration
4. Integration tests with real AI providers
5. Performance and security testing

## Decision Drivers

- **Security**: AI database queries must be tested for SQL injection and access control
- **Reliability**: LangChain4j agent integrations must work consistently
- **Maintainability**: Tests must be easy to run and maintain
- **Cost**: Minimize API costs during testing
- **CI/CD**: Enable automated testing on every commit

## Considered Options

1. **JUnit 5 + Mockito (Unit) + Testcontainers (Integration)**
2. **JUnit 5 + Mockito (Unit) + Manual Integration Tests**
3. **JUnit 5 + Mockito + MCP Inspector for E2E**
4. **Complete TestNG Migration**

## Decision Outcome

**Chosen option:** "Option 3: JUnit 5 + Mockito + MCP Inspector for E2E", because it:

- Leverages existing test infrastructure (JUnit 5, Mockito, AssertJ)
- Uses MCP Inspector CLI for automated MCP server testing
- Supports conditional integration tests based on environment
- Enables cost-effective CI/CD with mocked providers

### Confirmation

- All tests pass with `mvn test`
- GitHub Actions workflow succeeds on PR
- MCP Inspector CLI validates MCP tools
- Code coverage > 70% for critical packages

## Testing Architecture

### Test Layers

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 4: End-to-End (E2E) Tests                                 │
│   - MCP Inspector CLI validation                                │
│   - Full stack: Agent → MCP → CLI → Database                    │
│   - Run: On-demand / Release                                    │
├─────────────────────────────────────────────────────────────────┤
│ Layer 3: Integration Tests                                      │
│   - Real AI provider calls (Anthropic, Ollama)                  │
│   - Requires environment variables                              │
│   - Run: Manual / Nightly CI                                    │
├─────────────────────────────────────────────────────────────────┤
│ Layer 2: Component Tests                                        │
│   - Database mocked, business logic tested                      │
│   - SecureDatabaseQueryExecutor with mock DB                    │
│   - Run: Every commit                                           │
├─────────────────────────────────────────────────────────────────┤
│ Layer 1: Unit Tests                                             │
│   - Pure logic, no dependencies                                 │
│   - DTOs, utilities, validators                                 │
│   - Run: Every commit                                           │
└─────────────────────────────────────────────────────────────────┘
```

### Test Categories and Execution

| Category | Location | Run Command | CI Trigger |
|----------|----------|-------------|------------|
| Unit Tests | `src/test/java/**/*Test.java` | `mvn test` | Every commit |
| Integration Tests | `src/test/java/**/*IT.java` | `mvn verify -Pintegration` | Nightly/Manual |
| E2E Tests | `mcp-tests/*.sh` | `./mcp-tests/run-all.sh` | Release |

### Directory Structure

```
src/
├── test/
│   └── java/
│       └── com/cloudempiere/ai/
│           ├── database/
│           │   ├── SecureDatabaseQueryExecutorTest.java
│           │   └── SecureQueryRequestTest.java
│           ├── context/
│           │   ├── AIContextProviderRegistryTest.java
│           │   └── ContextParametersTest.java
│           └── provider/
│               └── langchain4j/
│                   ├── ERPToolsTest.java          (existing)
│                   ├── IDempiereAIServiceTest.java (existing)
│                   └── LangChain4jProviderFactoryTest.java (existing)
mcp-tests/
├── README.md
├── run-all.sh
├── test-mcp-tools.sh
├── fixtures/
│   ├── test-queries.json
│   └── expected-responses.json
└── config/
    └── inspector-config.json
```

## Implementation

### Test Dependencies (pom.xml)

```xml
<!-- Already configured -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.25.3</version>
    <scope>test</scope>
</dependency>
```

### GitHub Actions CI/CD Pipeline

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [master, develop, langchain]
  pull_request:
    branches: [master, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'corretto'
      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      - name: Run unit tests
        run: mvn test -DskipITs
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/
```

### MCP Inspector Integration

The [MCP Inspector](https://github.com/modelcontextprotocol/inspector) provides CLI mode for automated testing:

```bash
# Install
npm install -g @anthropic-ai/mcp-inspector

# Test MCP tools
npx @anthropic-ai/mcp-inspector \
  --cli \
  --server "java -jar ../idempiere-mcp-server/target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar" \
  --command "tools/list"

# Test specific tool
npx @anthropic-ai/mcp-inspector \
  --cli \
  --server "java -jar ..." \
  --command "tools/call" \
  --params '{"name": "listTables", "arguments": {"pattern": "C_%"}}'
```

### Test Patterns

#### 1. Unit Test Pattern (No Dependencies)

```java
@DisplayName("SecureQueryRequest Tests")
class SecureQueryRequestTest {

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        SecureQueryRequest request = new SecureQueryRequest();
        assertThatThrownBy(() -> request.validate())
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

#### 2. Component Test Pattern (Mocked Dependencies)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SecureDatabaseQueryExecutor Tests")
class SecureDatabaseQueryExecutorTest {

    @Mock
    private Properties mockCtx;

    @Test
    @DisplayName("Should reject DML statements")
    void shouldRejectDmlStatements() {
        // Test SQL validation without real database
    }
}
```

#### 3. Integration Test Pattern (Real Providers)

```java
@DisplayName("Anthropic Integration Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AnthropicIntegrationIT {

    @Test
    @DisplayName("Should complete chat with real API")
    void shouldChatWithRealApi() {
        // Real API call - only runs when API key is set
    }
}
```

## Pros and Cons of the Options

### Option 1: JUnit 5 + Mockito + Testcontainers

- Good, because provides isolated database testing
- Bad, because adds complexity (Docker required)
- Bad, because iDempiere setup in container is complex

### Option 2: JUnit 5 + Mockito + Manual Integration

- Good, because simple setup
- Bad, because manual integration testing is error-prone
- Bad, because no automated E2E validation

### Option 3: JUnit 5 + Mockito + MCP Inspector (Chosen)

- Good, because leverages existing infrastructure
- Good, because MCP Inspector provides automated E2E testing
- Good, because conditional tests minimize CI costs
- Good, because CLI mode enables scripted validation
- Neutral, because requires MCP server setup for E2E

### Option 4: TestNG Migration

- Good, because more powerful grouping
- Bad, because migration effort from existing JUnit 5 tests
- Bad, because team familiar with JUnit

## Test Coverage Goals

| Package | Target Coverage | Priority |
|---------|----------------|----------|
| `database/` | > 80% | Critical (security) |
| `provider/langchain4j/` | > 70% | High |
| `context/` | > 70% | High |
| `model/` | > 60% | Medium |
| `process/` | > 50% | Medium |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Unit test pass rate | 100% | CI pipeline |
| Code coverage | > 70% | JaCoCo report |
| CI build time | < 5 min | GitHub Actions |
| Integration test success | > 95% | Nightly run |
| MCP E2E validation | 100% | Release gate |

## More Information

### Related ADRs

- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md) - Agent architecture
- [ADR-003: MCP Server Integration](003-mcp-server-integration.md) - MCP testing scope
- [ADR-007: Database Security Model](007-database-security-model.md) - Security test requirements

### References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://site.mockito.org/)
- [MCP Inspector](https://github.com/modelcontextprotocol/inspector)
- [GitHub Actions for Java](https://docs.github.com/en/actions/use-cases-and-examples/building-and-testing/building-and-testing-java-with-maven)

---

*ADR-032 | Version 1.0 | 2025-12-03*
*Status: **Accepted***
