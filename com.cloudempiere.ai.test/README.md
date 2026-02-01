# CloudEmpiere AI - Test Plugin

**Bundle ID:** `com.cloudempiere.ai.test`
**Version:** 10.0.2-SNAPSHOT
**Type:** OSGi Test Fragment
**Test Framework:** JUnit 5

## Purpose

Comprehensive test suite for CloudEmpiere AI plugins using JUnit 5 with iDempiere test infrastructure.

## Test Structure

```
com.cloudempiere.ai.test/
├── src/
│   └── com/cloudempiere/ai/test/
│       ├── categories/
│       │   ├── UnitTest.java
│       │   ├── IntegrationTest.java
│       │   └── E2ETest.java
│       ├── provider/
│       │   ├── LangChain4jProviderFactoryTest.java
│       │   ├── AnthropicProviderTest.java
│       │   └── BedrockProviderTest.java
│       ├── database/
│       │   └── SecureDatabaseQueryExecutorTest.java
│       ├── context/
│       │   └── AIContextProviderRegistryTest.java
│       └── boundary/
│           └── DomainBoundaryTest.java
├── run-unit-tests.sh
└── run-integration-tests.sh
```

## Test Categories

### @UnitTest
**Purpose:** Fast, isolated tests with no external dependencies.

```java
@UnitTest
class DomainBoundaryTest {
    @Test
    void testTableValidation() {
        DomainBoundary boundary = new SalesDomainBoundary();

        // Should allow
        assertDoesNotThrow(() -> boundary.validateReadTable("C_Order"));

        // Should reject
        assertThrows(SecurityException.class,
            () -> boundary.validateReadTable("M_Warehouse"));
    }
}
```

**Characteristics:**
- No database access
- No API calls
- No file I/O
- Fast execution (<100ms per test)

### @IntegrationTest
**Purpose:** Tests with database and iDempiere context.

```java
@IntegrationTest
class SecureDatabaseQueryExecutorTest {
    private Properties ctx;
    private String trxName;

    @BeforeEach
    void setUp() {
        ctx = Env.getCtx();
        trxName = Trx.createTrxName("Test");
    }

    @Test
    void testSecureQuery() {
        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql("SELECT C_Order_ID FROM C_Order WHERE DocStatus='CO'")
            .ctx(ctx)
            .trxName(trxName)
            .build();

        SecureQueryResult result =
            SecureDatabaseQueryExecutor.executeSecureQuery(request);

        assertNotNull(result);
        assertTrue(result.getRowCount() >= 0);
    }

    @AfterEach
    void tearDown() {
        Trx.get(trxName, false).rollback();
    }
}
```

**Characteristics:**
- Requires iDempiere database
- Uses test transactions (rolled back)
- Slower execution (1-10s per test)

### @E2ETest
**Purpose:** End-to-end tests with real AI providers.

```java
@E2ETest
class AnthropicProviderE2ETest {
    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void testRealAPICall() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");

        AIRequest request = AIRequest.builder()
            .message("What is 2+2?")
            .model("claude-3-5-sonnet-20241022")
            .maxTokens(100)
            .build();

        AnthropicProvider provider = new AnthropicProvider(apiKey);
        AIResponse response = provider.generateText(request);

        assertTrue(response.getContent().contains("4"));
        assertTrue(response.getTokenUsage().getTotalTokens() > 0);
    }
}
```

**Characteristics:**
- Requires API keys (env variables)
- Makes real API calls (costs money)
- Slowest execution (5-30s per test)
- Only run manually or in nightly builds

## Running Tests

### Unit Tests Only (Fast)
```bash
./run-unit-tests.sh
```

**Equivalent Maven:**
```bash
mvn test -Dgroups="unit"
```

**Expected Output:**
```
[INFO] Running unit tests...
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 8.342 s
```

### Integration Tests
```bash
./run-unit-tests.sh --integration
```

**Equivalent Maven:**
```bash
mvn test -Dgroups="integration"
```

**Requirements:**
- iDempiere database running
- Test data loaded
- Correct database credentials in `IDEMPIERE_HOME`

### E2E Tests (Manual)
```bash
# Set API keys
export ANTHROPIC_API_KEY="sk-ant-..."
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="..."

# Run E2E tests
mvn test -Dgroups="e2e"
```

### All Tests
```bash
mvn test
```

**Categories run:**
- Unit tests (45 tests, ~8s)
- Integration tests (12 tests, ~45s)
- E2E tests (skipped if no API keys)

## Test Configuration

### pom.xml
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
```

### Test Categories (Annotations)
```java
// src/com/cloudempiere/ai/test/categories/UnitTest.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("unit")
public @interface UnitTest {
}

// IntegrationTest.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
public @interface IntegrationTest {
}

// E2ETest.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("e2e")
public @interface E2ETest {
}
```

## Test Scripts

### run-unit-tests.sh
```bash
#!/bin/bash
# Run unit tests (fast, no DB required)

if [ "$1" = "--integration" ]; then
    echo "Running integration tests..."
    mvn test -Dgroups="integration"
else
    echo "Running unit tests..."
    mvn test -Dgroups="unit"
fi
```

**Usage:**
```bash
chmod +x run-unit-tests.sh
./run-unit-tests.sh              # Unit tests only
./run-unit-tests.sh --integration  # Integration tests
```

## Writing New Tests

### 1. Create Test Class

```java
package com.cloudempiere.ai.test.provider;

import com.cloudempiere.ai.test.categories.UnitTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@UnitTest
class MyProviderTest {

    private MyProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MyProvider();
    }

    @Test
    @DisplayName("Should return valid response")
    void testValidResponse() {
        AIResponse response = provider.generateText(request);
        assertNotNull(response);
        assertNotNull(response.getContent());
    }

    @AfterEach
    void tearDown() {
        provider = null;
    }
}
```

### 2. Choose Category

```java
@UnitTest           // Fast, isolated
@IntegrationTest    // Database required
@E2ETest            // API calls required
```

### 3. Follow Naming Conventions

**Test Class:**
- `<ClassName>Test.java` - Unit tests
- `<ClassName>IntegrationTest.java` - Integration tests
- `<ClassName>E2ETest.java` - E2E tests

**Test Methods:**
- `testMethodName()` - Simple test
- `testMethodName_condition_expectedResult()` - BDD style

### 4. Use Assertions

```java
// JUnit 5 assertions
assertEquals(expected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(value);
assertNotNull(value);
assertThrows(Exception.class, () -> code());
assertDoesNotThrow(() -> code());
assertAll(
    () -> assertEquals(expected1, actual1),
    () -> assertEquals(expected2, actual2)
);
```

## Test Coverage

### Current Coverage (Phase 2 Week 4)

| Component | Unit Tests | Integration Tests | E2E Tests | Coverage |
|-----------|------------|-------------------|-----------|----------|
| **Provider Factory** | ✅ 8 tests | ✅ 4 tests | ✅ 2 tests | 85% |
| **Database Security** | ✅ 6 tests | ✅ 3 tests | - | 78% |
| **Context Providers** | ✅ 12 tests | ✅ 2 tests | - | 92% |
| **Domain Boundaries** | ✅ 10 tests | ✅ 1 test | - | 95% |
| **Data Models** | ✅ 9 tests | ✅ 2 tests | - | 80% |

**Total:** 45 unit, 12 integration, 2 E2E tests

### Target Coverage (Phase 3)
- Unit test coverage: >90%
- Integration test coverage: >75%
- Critical paths: 100%

## Mock Objects

### Mockito for Provider Mocking

```java
import static org.mockito.Mockito.*;

@UnitTest
class AIServiceTest {

    @Test
    void testChatWithMockProvider() {
        // Mock provider
        IAIProvider mockProvider = mock(IAIProvider.class);
        AIResponse mockResponse = new AIResponse("Test response", ...);

        when(mockProvider.generateText(any(AIRequest.class)))
            .thenReturn(mockResponse);

        // Test service with mock
        AIService service = new AIService(mockProvider);
        String result = service.chat("Hello");

        assertEquals("Test response", result);
        verify(mockProvider, times(1)).generateText(any());
    }
}
```

### Test Data Builders

```java
public class AIRequestBuilder {
    public static AIRequest.Builder defaultRequest() {
        return AIRequest.builder()
            .message("Test message")
            .model("test-model")
            .temperature(0.7)
            .maxTokens(100);
    }
}

// Usage in tests
AIRequest request = AIRequestBuilder.defaultRequest()
    .message("Custom message")
    .build();
```

## Continuous Integration

### GitHub Actions

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run Unit Tests
        run: ./run-unit-tests.sh

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
    steps:
      - uses: actions/checkout@v2
      - name: Set up iDempiere DB
        run: ./setup-test-db.sh
      - name: Run Integration Tests
        run: ./run-unit-tests.sh --integration
```

## Debugging Tests

### Eclipse IDE

1. Right-click test class → **Debug As** → **JUnit Test**
2. Set breakpoints in test or source code
3. Step through execution

### Command Line with Debug

```bash
mvn test -Dmaven.surefire.debug
# Connect debugger to port 5005
```

### Verbose Logging

```bash
# Enable DEBUG logging
mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Troubleshooting

### Tests Not Found
**Symptom:** `No tests found`

**Solution:**
1. Verify `@Test` annotation is from JUnit 5 (`org.junit.jupiter.api.Test`)
2. Check test class is public
3. Verify test method is public and void

### Database Connection Failed
**Symptom:** Integration tests fail with DB errors

**Solution:**
1. Check `IDEMPIERE_HOME` is set
2. Verify database is running
3. Check credentials in `idempiere.properties`

### API Key Tests Skipped
**Symptom:** E2E tests show `@DisabledIfEnvironmentVariable`

**Solution:**
- This is expected if API keys not set
- E2E tests are optional
- Set environment variables to enable:
  ```bash
  export ANTHROPIC_API_KEY="sk-ant-..."
  ```

## Best Practices

1. **Write tests first** (TDD approach)
2. **One assertion per test** (or use assertAll)
3. **Clear test names** (describe what is tested)
4. **Independent tests** (no test depends on another)
5. **Clean up resources** (use @AfterEach)
6. **Mock external dependencies** (databases, APIs)
7. **Test edge cases** (null, empty, invalid input)

## References

- **JUnit 5 Documentation:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation:** https://site.mockito.org/
- **iDempiere Testing Guide:** https://wiki.idempiere.org/en/Testing

## Maintainers

CloudEmpiere AI Team

**Last Updated:** 2026-01-30
