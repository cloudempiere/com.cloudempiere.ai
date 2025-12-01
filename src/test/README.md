# Test Framework

This directory contains JUnit 5 tests for the `com.cloudempiere.ai` plugin.

## Test Classes

| Class | Description |
|-------|-------------|
| `LangChain4jProviderFactoryTest` | Tests for provider creation and caching |
| `ERPToolsTest` | Tests for @Tool annotated ERP operations |
| `IDempiereAIServiceTest` | Tests for the main AI service facade |

## Running Tests

### Option 1: Eclipse IDE (Recommended)

1. Import the project into Eclipse with iDempiere
2. Right-click on test class → Run As → JUnit Test
3. Or run all tests: Right-click on `src/test/java` → Run As → JUnit Test

### Option 2: iDempiere Test Framework

Tests require the full iDempiere environment due to OSGi/Tycho dependencies.

```bash
# From iDempiere workspace root
cd iDempiereCLDE
mvn verify -pl ../com.cloudempiere.ai
```

### Option 3: Environment Variables for Integration Tests

Some tests require API keys to run integration tests:

```bash
# Anthropic integration tests
export ANTHROPIC_API_KEY=sk-ant-xxx

# OpenAI integration tests
export OPENAI_API_KEY=sk-xxx

# AWS Bedrock tests (uses IAM)
export AWS_ACCESS_KEY_ID=xxx
export AWS_SECRET_ACCESS_KEY=xxx

# Local Ollama tests
export OLLAMA_ENABLED=true
```

## Test Categories

### Unit Tests (No API Key Required)
- Provider type detection
- Error handling
- Cache management
- Input validation
- SQL injection prevention

### Integration Tests (API Key Required)
Tests marked with `@EnabledIfEnvironmentVariable` run only when the appropriate API keys are set:
- `ANTHROPIC_API_KEY` - Anthropic Claude integration tests
- `OPENAI_API_KEY` - OpenAI integration tests
- `AWS_ACCESS_KEY_ID` - AWS Bedrock integration tests
- `OLLAMA_ENABLED=true` - Local Ollama tests

## Dependencies

Test dependencies are defined in `pom.xml`:
- JUnit Jupiter 5.10.2
- Mockito 5.11.0
- AssertJ 3.25.3
