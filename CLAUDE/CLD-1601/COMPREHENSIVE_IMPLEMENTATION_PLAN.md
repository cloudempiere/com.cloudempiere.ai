# com.cloudempiere.ai - Comprehensive Implementation Plan

**Created**: 2025-11-17
**Author**: Claude Code Analysis
**Status**: Strategic Planning Document
**Security Priority**: CRITICAL

---

## Executive Summary

This document provides a comprehensive implementation plan for the **com.cloudempiere.ai** plugin, integrating two distinct architectural approaches:

1. **External API Approach** (Anthropic Claude - Already Implemented)
   - No database access
   - Works with data sent in requests
   - Cloud-based, fully managed
   - Already has working implementation

2. **Local LLM Approach** (Ollama/druiz POC - To Be Integrated)
   - Restricted database access through secure layer
   - On-premise, privacy-preserving
   - Proof-of-concept exists, needs productionization

**⚠️ SECURITY IS PARAMOUNT**: The local LLM approach with database access requires extensive security hardening before production use.

---

## Current State Analysis

### What We Have

#### 1. Core Infrastructure (com.cloudempiere.ai) ✅

**Location**: `/Users/developer/GitHub/com.cloudempiere.ai`

**Completed Components**:
- ✅ **IAIProvider Interface** - Generic provider abstraction
- ✅ **DTO Layer** (10 classes):
  - AIRequest, AIResponse, AIMessage
  - AITokenUsage, AIModelCapabilities, AIHealthStatus
  - AIRateLimitStatus, AIFunction, AIFunctionCall
  - AIStreamCallback
- ✅ **AnthropicProvider** - Full working implementation with:
  - Text generation (sync and streaming)
  - Function calling support
  - Cost estimation
  - Health monitoring
  - Uses official Anthropic Java SDK v2.10.0
- ✅ **Provider Factory Pattern** with OSGi service registration
- ✅ **Exception Handling** - AIProviderException with error codes
- ✅ **Test Classes** - AnthropicProviderTest, TestAIProvider

**Package Structure**:
```
com.cloudempiere.ai/
├── src/com/cloudempiere/ai/
│   ├── provider/
│   │   ├── IAIProvider.java
│   │   ├── AIProviderException.java
│   │   ├── dto/          (10 DTO classes)
│   │   ├── impl/
│   │   │   └── AnthropicProvider.java
│   │   ├── factory/
│   │   │   ├── IAIProviderFactory.java
│   │   │   └── AIProviderFactory.java
│   │   └── test/
│   ├── model/
│   │   ├── I_AIG_Provider.java
│   │   ├── X_AIG_Provider.java
│   │   └── MAIProvider.java
│   ├── process/
│   │   └── TestAIProvider.java
│   └── Activator.java
└── OSGI-INF/
    └── com.cloudempiere.ai.provider.factory.AIProviderFactory.xml
```

**Total Files**: 21 Java files

#### 2. Comprehensive Data Model Architecture ✅

**Location**: `/Users/developer/GitHub/com.cloudempiere.ai/CLAUDE/CLD-1601/AI_DATA_MODEL_ARCHITECTURE.md`

**Includes**:
- ✅ 10 core database tables with full ERD
- ✅ Materialized views for analytics
- ✅ Security considerations and role-based access
- ✅ 11 detailed use cases:
  1. Chart Analysis ("Explain What I'm Seeing")
  2. Sales Opportunity Summarization
  3. Customer Request Classification
  4. Business Partner Relationship Summary
  5. Auto-Labeling for Records
  6. Writing Improvement for Support Responses
  7. Inbound Email Cleaning
  8. Product Catalog Enhancement
  9. Multi-Language Translation
  10. Import Loader Data Normalization
  11. Invoice OCR Processing
- ✅ Integration with CM_Chat module
- ✅ Cost optimization strategies
- ✅ Scalability patterns

#### 3. druiz POC (de.bxservice.chatbotpoc) ⚠️

**Location**: External repository - https://github.com/d-ruiz/de.bxservice.chatbotpoc

**Status**: Experimental POC, NOT production-ready

**Key Components**:
- **Java Layer**:
  - ChatBotForm.java (ZK UI)
  - ChatbotUtils.java (Java-Python bridge)
  - PythonInvoker.java (iDempiere process wrapper)
  - MSQLAIConfigurator.java (configuration model)
  - OSGi service factories

- **Python Layer**:
  - pocSQLAgent.py (LangGraph-based multi-step workflow)
  - pocSQLLangChain.py (simpler chain-based approach)
  - LangChain/LangGraph orchestration
  - Ollama with Llama 3.1 (8B model)
  - PostgreSQL database access

**Workflow**:
```
User Question → ChatBotForm → ChatbotUtils → Python Process
  → LangChain/LangGraph Agent → SQL Generation → Execution
  → HTML Response → Chat UI
```

**Capabilities**:
- Natural language to SQL conversion
- Database querying through predefined views:
  - v_product_info
  - v_order_info
  - v_orderline_info
  - v_bpartner_info
- Multi-step validation workflow
- Read-only query enforcement

**Critical Limitations**:
- ⚠️ Explicitly marked as experimental
- ⚠️ Hardcoded configuration (UUID-based)
- ⚠️ Subprocess execution with potential command injection risk
- ⚠️ Database credentials in Python code
- ⚠️ No user permission checks
- ⚠️ Synchronous blocking execution
- ⚠️ No conversation memory/context
- ⚠️ Limited to 4 predefined views

---

## Architecture Comparison: Two Approaches

### Approach 1: External API (Anthropic Claude) ✅ IMPLEMENTED

**How It Works**:
```
User Request → iDempiere Process → AIRequest (DTO)
  → AnthropicProvider → Anthropic API (HTTPS)
  → Cloud LLM Processing → AIResponse (DTO)
  → iDempiere Process → User
```

**Characteristics**:
- ✅ **No Database Access**: LLM works only with data sent in request
- ✅ **Fully Managed**: Anthropic handles infrastructure
- ✅ **Production Ready**: Official SDK, well-tested
- ✅ **Scalable**: Cloud infrastructure auto-scales
- ✅ **Simple**: No local LLM installation required
- ✅ **Secure**: HTTPS encryption, API key authentication
- ✅ **Cost**: Pay-per-use, token-based pricing

**Security Model**:
- API key stored encrypted in database
- No system access beyond API calls
- Data sent to external service (privacy consideration)
- Rate limiting enforced by provider
- No SQL injection risk

**Use Cases**:
- Text generation (summaries, descriptions)
- Document analysis
- Translation
- Content improvement
- Sentiment analysis
- Classification

**Limitations**:
- Cannot query database directly
- Requires sending all context in request
- External dependency (internet required)
- Data leaves premises (compliance concern)
- Usage costs scale with volume

### Approach 2: Local LLM with Restricted DB Access ⚠️ POC ONLY

**How It Works (druiz POC)**:
```
User Question → ChatBotForm → ChatbotUtils → Python Subprocess
  → LangChain Agent → LLM (Ollama) → SQL Generation
  → Validation → Execute on DB Views → Format Response
  → Return to Java → Display in UI
```

**Characteristics**:
- ⚠️ **Restricted Database Access**: Through secure view layer
- ✅ **On-Premise**: All processing local, no data leaves network
- ⚠️ **Complex Setup**: Requires Python + Ollama + LLM model
- ⚠️ **Resource Intensive**: Local GPU/CPU for inference
- ⚠️ **Experimental**: POC status, needs hardening
- ⚠️ **Security Risk**: Direct database access requires extensive safeguards
- ✅ **No Usage Costs**: After initial setup

**Security Model (Current POC - INADEQUATE)**:
- ⚠️ Read-only queries enforced in Python
- ⚠️ Limited to 4 predefined views
- ⚠️ 5-row result limit
- ⚠️ No user permission integration
- ⚠️ SQL injection risk from LLM-generated queries
- ⚠️ Command injection risk in Python invocation
- ⚠️ Database credentials in Python code

**Security Model (REQUIRED for Production)**:
- ✅ Row-level security based on iDempiere roles
- ✅ Dynamic view access per user/role
- ✅ Query validation through whitelist
- ✅ SQL injection prevention
- ✅ Audit logging with user tracking
- ✅ Resource limits (rows, execution time)
- ✅ Credentials from secure vault
- ✅ Sandboxed execution environment

**Use Cases**:
- Natural language data queries
- Business intelligence questions
- Report generation
- Data exploration
- Cross-table analysis

**Limitations**:
- Requires local infrastructure
- LLM accuracy varies
- Query generation errors possible
- High security risk if not properly locked down
- Maintenance overhead

---

## Implementation Strategy: Unified Hybrid Approach

### Vision: Best of Both Worlds

Integrate both approaches into a unified `com.cloudempiere.ai` plugin that allows administrators to choose the appropriate provider based on use case:

**Decision Matrix**:

| Use Case | Recommended Approach | Rationale |
|----------|---------------------|-----------|
| Content generation | External API | No DB access needed |
| Translation | External API | Language models excel here |
| Document OCR | External API | Specialized services better |
| Data queries | Local LLM | Database access required |
| BI questions | Local LLM | Complex joins, privacy |
| High-volume | Local LLM | Cost optimization |
| Low-volume | External API | Simpler, no infrastructure |
| Compliance-sensitive | Local LLM | Data stays on-premise |

---

## Phase 1: Security Foundation for Local LLM (CRITICAL)

**Duration**: 4-6 weeks
**Priority**: HIGHEST - Must complete before any production use

### Security Requirements

#### 1. Database Access Security Layer

**Component**: `SecureDatabaseQueryExecutor.java`

**Purpose**: Secure wrapper for LLM-generated SQL execution

**Features**:
```java
public class SecureDatabaseQueryExecutor {

    /**
     * Execute query with comprehensive security checks
     *
     * Security Controls:
     * 1. User authentication and role validation
     * 2. Row-level security enforcement
     * 3. SQL injection prevention
     * 4. Query whitelisting
     * 5. Resource limits
     * 6. Audit logging
     */
    public SecureQueryResult executeQuery(
        Properties ctx,
        String sql,
        int userId,
        String trxName
    ) throws SecurityException {

        // 1. Validate user has AI query permission
        if (!hasAIQueryPermission(ctx, userId)) {
            auditLog(userId, sql, "PERMISSION_DENIED");
            throw new SecurityException("User not authorized for AI queries");
        }

        // 2. Parse and validate SQL
        ParsedQuery parsed = parseSQLSafe(sql);
        if (!parsed.isValid()) {
            auditLog(userId, sql, "INVALID_SQL");
            throw new SecurityException("Invalid SQL structure");
        }

        // 3. Check against whitelist
        if (!isWhitelistedQuery(parsed)) {
            auditLog(userId, sql, "NOT_WHITELISTED");
            throw new SecurityException("Query not in whitelist");
        }

        // 4. Enforce read-only
        if (!parsed.isReadOnly()) {
            auditLog(userId, sql, "DML_ATTEMPTED");
            throw new SecurityException("Only SELECT queries allowed");
        }

        // 5. Apply row-level security
        String securedSQL = applyRowLevelSecurity(ctx, parsed, userId);

        // 6. Apply resource limits
        securedSQL = applyLimits(securedSQL, MAX_ROWS, MAX_EXECUTION_TIME);

        // 7. Execute with timeout
        SecureQueryResult result = executeWithTimeout(
            ctx,
            securedSQL,
            MAX_EXECUTION_TIME,
            trxName
        );

        // 8. Audit log
        auditLog(userId, sql, "SUCCESS", result.getRowCount());

        return result;
    }
}
```

**Implementation Details**:

1. **SQL Parser Integration**:
   - Use JSQLParser or similar for safe parsing
   - Detect DML operations (INSERT, UPDATE, DELETE, DROP, etc.)
   - Extract table names and verify access
   - Prevent SQL injection patterns

2. **Row-Level Security**:
   ```java
   private String applyRowLevelSecurity(
       Properties ctx,
       ParsedQuery query,
       int userId
   ) {
       // Get user's client/org context
       int clientId = Env.getAD_Client_ID(ctx);
       int orgId = Env.getAD_Org_ID(ctx);

       // Add WHERE clauses for multi-tenancy
       String rls = String.format(
           "AD_Client_ID = %d AND (AD_Org_ID = %d OR AD_Org_ID = 0)",
           clientId, orgId
       );

       return query.addWhereClause(rls);
   }
   ```

3. **Query Whitelisting**:
   ```java
   /**
    * Define approved table/view patterns
    */
   private static final Set<String> WHITELISTED_TABLES = Set.of(
       "V_PRODUCT_INFO",
       "V_ORDER_INFO",
       "V_ORDERLINE_INFO",
       "V_BPARTNER_INFO",
       "V_INVOICE_INFO"
       // Add more as needed with approval
   );

   private boolean isWhitelistedQuery(ParsedQuery query) {
       for (String table : query.getTables()) {
           if (!WHITELISTED_TABLES.contains(table.toUpperCase())) {
               return false;
           }
       }
       return true;
   }
   ```

4. **Resource Limits**:
   ```java
   private static final int MAX_ROWS = 100;
   private static final int MAX_EXECUTION_TIME_MS = 5000; // 5 seconds

   private String applyLimits(String sql, int maxRows, int timeoutMs) {
       // Add LIMIT clause if not present
       if (!sql.toUpperCase().contains("LIMIT")) {
           sql += " LIMIT " + maxRows;
       }
       return sql;
   }
   ```

5. **Audit Logging**:
   ```java
   /**
    * Log all AI query attempts
    */
   private void auditLog(
       int userId,
       String sql,
       String status,
       int rowCount
   ) {
       AIG_QueryAudit audit = new AIG_QueryAudit(ctx, 0, trxName);
       audit.setAD_User_ID(userId);
       audit.setQuerySQL(sql);
       audit.setStatus(status);
       audit.setRowCount(rowCount);
       audit.setExecutionTime(System.currentTimeMillis() - startTime);
       audit.saveEx();
   }
   ```

#### 2. Secure Database Views

**Purpose**: Additional security layer - LLM only accesses views, not direct tables

**Implementation**:

```sql
-- Example: Product view with security filters
CREATE OR REPLACE VIEW v_ai_product_info AS
SELECT
    p.M_Product_ID,
    p.Value AS ProductCode,
    p.Name AS ProductName,
    p.Description,
    pc.Name AS Category,
    p.IsActive,
    -- NEVER expose sensitive fields like cost
    -- p.CostStandard (EXCLUDED)
    AD_Client_ID,
    AD_Org_ID
FROM M_Product p
LEFT JOIN M_Product_Category pc ON p.M_Product_Category_ID = pc.M_Product_Category_ID
WHERE p.IsActive = 'Y';

-- Grant access only through role
GRANT SELECT ON v_ai_product_info TO idempiere_ai_query_role;
REVOKE ALL ON M_Product FROM idempiere_ai_query_role;
```

**View Design Principles**:
- ✅ Only expose non-sensitive columns
- ✅ Join commonly needed related data
- ✅ Apply default filters (e.g., IsActive = 'Y')
- ✅ Denormalize for easier LLM comprehension
- ✅ Include AD_Client_ID, AD_Org_ID for RLS
- ❌ NEVER expose: passwords, API keys, costs, salaries, credit cards

#### 3. Secure Python Execution Environment

**Problem**: Current POC uses subprocess with potential command injection

**Solution**: Sandboxed execution with strict validation

```java
public class SecurePythonExecutor {

    /**
     * Execute Python script with security controls
     */
    public String executePython(
        String scriptPath,
        Map<String, String> parameters
    ) throws SecurityException {

        // 1. Validate script path (no path traversal)
        if (!isValidScriptPath(scriptPath)) {
            throw new SecurityException("Invalid script path");
        }

        // 2. Sanitize all parameters
        Map<String, String> sanitized = sanitizeParameters(parameters);

        // 3. Build command with whitelist validation
        List<String> command = buildSecureCommand(scriptPath, sanitized);

        // 4. Execute with timeout and resource limits
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        // Set resource limits (if supported by OS)
        // - Max memory
        // - Max CPU time
        // - No network access

        Process process = pb.start();

        // 5. Read output with timeout
        String output = readWithTimeout(process, 30000); // 30 sec

        // 6. Verify process completed successfully
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new AIProviderException("Python execution failed");
        }

        return output;
    }

    /**
     * Sanitize parameters to prevent injection
     */
    private Map<String, String> sanitizeParameters(
        Map<String, String> params
    ) {
        Map<String, String> sanitized = new HashMap<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Remove dangerous characters
            value = value.replaceAll("[;&|`$(){}\\[\\]<>]", "");

            // Validate key is alphanumeric
            if (!key.matches("[a-zA-Z0-9_]+")) {
                throw new SecurityException("Invalid parameter key: " + key);
            }

            sanitized.put(key, value);
        }

        return sanitized;
    }
}
```

#### 4. Database Credentials Security

**Problem**: POC has credentials in Python code

**Solution**: Secure credential management

**Options**:

1. **Use iDempiere's Connection Pool** (RECOMMENDED):
   ```python
   # Python accesses DB through iDempiere-managed connection
   # Credentials never exposed to Python code
   # Java passes connection string with token

   # Java side:
   String connectionToken = generateSecureToken();
   storeConnectionInPool(connectionToken, ctx, trxName);

   # Python side:
   connection = get_connection_from_idempiere(token)
   ```

2. **Environment Variables**:
   ```java
   // Set environment variables for Python process
   ProcessBuilder pb = new ProcessBuilder();
   Map<String, String> env = pb.environment();
   env.put("DB_HOST", "localhost");
   env.put("DB_PORT", "5432");
   env.put("DB_NAME", "idempiere");
   env.put("DB_USER", "readonly_ai_user");
   env.put("DB_PASSWORD", getSecurePassword()); // From vault
   ```

3. **Secure Vault Integration**:
   ```java
   // Store credentials in HashiCorp Vault, AWS Secrets Manager, etc.
   String password = vaultClient.getSecret("idempiere/ai/db_password");
   ```

#### 5. User Permission Integration

**Purpose**: Ensure LLM queries respect iDempiere user roles

**Implementation**:

```sql
-- New table: AI Query Permissions
CREATE TABLE AIG_User_Permission (
    AIG_User_Permission_ID INTEGER PRIMARY KEY,
    AD_User_ID INTEGER NOT NULL,
    AD_Role_ID INTEGER NOT NULL,
    AllowAIQueries CHAR(1) DEFAULT 'N',
    AllowedViews TEXT, -- JSON array of view names
    MaxQueriesPerHour INTEGER DEFAULT 10,
    MaxRowsPerQuery INTEGER DEFAULT 100,
    CONSTRAINT AIG_User_Permission_User_FK
        FOREIGN KEY (AD_User_ID) REFERENCES AD_User(AD_User_ID),
    CONSTRAINT AIG_User_Permission_Role_FK
        FOREIGN KEY (AD_Role_ID) REFERENCES AD_Role(AD_Role_ID)
);
```

**Permission Check**:
```java
public boolean hasAIQueryPermission(Properties ctx, int userId) {
    // Check if user has AI query role
    String sql = "SELECT COUNT(*) FROM AIG_User_Permission " +
                 "WHERE AD_User_ID = ? AND AllowAIQueries = 'Y'";

    int count = DB.getSQLValue(null, sql, userId);
    return count > 0;
}

public Set<String> getAllowedViews(Properties ctx, int userId) {
    String sql = "SELECT AllowedViews FROM AIG_User_Permission " +
                 "WHERE AD_User_ID = ?";

    String json = DB.getSQLValueString(null, sql, userId);
    return parseJSONArray(json);
}
```

#### 6. Rate Limiting and Abuse Prevention

**Purpose**: Prevent resource exhaustion from excessive queries

**Implementation**:

```java
public class AIQueryRateLimiter {

    // Cache recent queries per user
    private static final Map<Integer, Queue<Long>> userQueryHistory =
        new ConcurrentHashMap<>();

    /**
     * Check if user has exceeded query rate limit
     */
    public boolean isRateLimited(int userId, int maxPerHour) {
        Queue<Long> queries = userQueryHistory.computeIfAbsent(
            userId,
            k -> new ConcurrentLinkedQueue<>()
        );

        // Remove queries older than 1 hour
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        queries.removeIf(time -> time < oneHourAgo);

        // Check limit
        if (queries.size() >= maxPerHour) {
            return true;
        }

        // Add current query
        queries.add(System.currentTimeMillis());
        return false;
    }
}
```

---

## Phase 2: Ollama Provider Implementation (Local LLM)

**Duration**: 3-4 weeks
**Depends On**: Phase 1 completion

### Component: OllamaProvider.java

**Purpose**: Local LLM provider using Ollama for on-premise inference

**Location**: `com.cloudempiere.ai.provider.impl.OllamaProvider.java`

**Architecture**:
```
iDempiere Process → AIRequest → OllamaProvider
  → HTTP API (localhost:11434) → Ollama Service
  → Local LLM Model → Response → AIResponse
```

**Implementation**:

```java
package com.cloudempiere.ai.provider.impl;

import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.model.MAIProvider;
import org.compiere.util.CLogger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

/**
 * Ollama Local LLM Provider
 *
 * Supports running LLMs locally for privacy and cost optimization.
 *
 * Ollama API: http://localhost:11434
 * Models: llama3.1, llama2, mistral, codellama, etc.
 *
 * Security: This provider can be configured with database access
 * through SecureDatabaseQueryExecutor.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class OllamaProvider implements IAIProvider {

    private static final CLogger log = CLogger.getCLogger(OllamaProvider.class);

    /** Default Ollama API endpoint */
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434";

    /** Provider configuration */
    private MAIProvider providerConfig;

    /** HTTP client for Ollama API */
    private HttpClient httpClient;

    /** Ollama API endpoint */
    private String apiEndpoint;

    /** Ready flag */
    private boolean ready = false;

    /** Secure query executor (for database access) */
    private SecureDatabaseQueryExecutor queryExecutor;

    @Override
    public void initialize(MAIProvider provider) throws AIProviderException {
        this.providerConfig = provider;

        // Get API endpoint from config or use default
        this.apiEndpoint = provider.getAPIEndpoint();
        if (apiEndpoint == null || apiEndpoint.isEmpty()) {
            this.apiEndpoint = DEFAULT_ENDPOINT;
        }

        // Initialize HTTP client
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

        // Initialize secure query executor if DB access enabled
        if (provider.isEnableDBAccess()) {
            this.queryExecutor = new SecureDatabaseQueryExecutor();
        }

        // Test connection
        if (!testConnection()) {
            throw new AIProviderException(
                "Failed to connect to Ollama at: " + apiEndpoint
            );
        }

        this.ready = true;
        log.info("Ollama provider initialized: " + apiEndpoint);
    }

    @Override
    public AIResponse generateText(AIRequest request) throws AIProviderException {
        if (!ready) {
            throw new AIProviderException("Provider not initialized");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Build Ollama API request
            String model = request.getModel() != null ?
                request.getModel() : "llama3.1:8b";

            // Check if this is a database query request
            boolean isDBQuery = request.getProviderParameters() != null &&
                request.getProviderParameters().containsKey("enable_db_query");

            AIResponse response;
            if (isDBQuery && queryExecutor != null) {
                // Use LangChain/LangGraph workflow for DB queries
                response = handleDatabaseQuery(request);
            } else {
                // Standard text generation
                response = handleTextGeneration(request, model);
            }

            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;

        } catch (Exception e) {
            log.severe("Ollama text generation failed: " + e.getMessage());
            throw new AIProviderException("Generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle standard text generation (no DB access)
     */
    private AIResponse handleTextGeneration(
        AIRequest request,
        String model
    ) throws Exception {

        // Build prompt
        StringBuilder prompt = new StringBuilder();
        if (request.getSystemPrompt() != null) {
            prompt.append(request.getSystemPrompt()).append("\n\n");
        }

        for (AIMessage msg : request.getMessages()) {
            prompt.append(msg.getRole()).append(": ")
                  .append(msg.getContent()).append("\n");
        }

        // Call Ollama API
        String requestBody = String.format(
            "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
            model,
            escapeJSON(prompt.toString())
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(apiEndpoint + "/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> httpResponse = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // Parse response
        return parseOllamaResponse(httpResponse.body());
    }

    /**
     * Handle database query request with security controls
     *
     * Security Flow:
     * 1. User question received
     * 2. LangChain generates SQL
     * 3. SecureDatabaseQueryExecutor validates and executes
     * 4. Results formatted and returned
     */
    private AIResponse handleDatabaseQuery(AIRequest request)
        throws Exception {

        if (queryExecutor == null) {
            throw new AIProviderException(
                "Database access not enabled for this provider"
            );
        }

        // Get user context
        Properties ctx = request.getContext();
        int userId = Env.getAD_User_ID(ctx);
        String trxName = request.getTransactionName();

        // Extract question
        String question = extractUserQuestion(request);

        // Call Python LangChain agent to generate SQL
        String generatedSQL = callLangChainAgent(question, ctx);

        // Execute through secure executor
        SecureQueryResult result = queryExecutor.executeQuery(
            ctx,
            generatedSQL,
            userId,
            trxName
        );

        // Format results into natural language response
        String formattedResponse = formatQueryResults(result, question);

        // Build AIResponse
        AIResponse response = new AIResponse();
        response.setContent(formattedResponse);
        response.setSuccess(true);

        // Add metadata
        response.addMetadata("sql_query", generatedSQL);
        response.addMetadata("row_count", result.getRowCount());

        return response;
    }

    /**
     * Call Python LangChain agent for SQL generation
     *
     * Integrates with druiz POC approach
     */
    private String callLangChainAgent(
        String question,
        Properties ctx
    ) throws Exception {

        // Build Python command
        String pythonScript = providerConfig.getPythonScriptPath();

        // Sanitize inputs
        Map<String, String> params = new HashMap<>();
        params.put("question", question);
        params.put("client_id", String.valueOf(Env.getAD_Client_ID(ctx)));

        // Execute Python securely
        SecurePythonExecutor executor = new SecurePythonExecutor();
        String output = executor.executePython(pythonScript, params);

        // Parse JSON response
        JSONObject response = new JSONObject(output);
        return response.getString("query");
    }

    @Override
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint + "/api/tags"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;

        } catch (Exception e) {
            log.warning("Ollama connection test failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public AIHealthStatus checkHealth() {
        AIHealthStatus status = new AIHealthStatus();

        try {
            long startTime = System.currentTimeMillis();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint + "/api/tags"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                status.setHealthy(true);
                status.setStatus("Healthy");
                status.setMessage("Ollama is running");
                status.setResponseTimeMs(responseTime);
            } else {
                status.setHealthy(false);
                status.setStatus("Unhealthy");
                status.setMessage("Ollama returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            status.setHealthy(false);
            status.setStatus("Unhealthy");
            status.setMessage("Health check failed: " + e.getMessage());
        }

        return status;
    }

    @Override
    public String getProviderType() {
        return MAIProvider.AIGPROVIDERTYPE_Ollama;
    }

    @Override
    public String getProviderName() {
        return "Ollama (Local LLM)";
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
            "llama3.1:8b",
            "llama3.1:70b",
            "llama2",
            "mistral",
            "codellama",
            "phi"
        );
    }

    @Override
    public boolean supportsTextGeneration() {
        return true;
    }

    @Override
    public boolean supportsEmbeddings() {
        // Ollama supports embeddings through /api/embeddings
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return true; // Ollama supports streaming
    }

    @Override
    public boolean supportsFunctionCalling() {
        return false; // Not natively supported
    }

    @Override
    public boolean supportsVision() {
        // Some models like llava support vision
        return false; // Default false, can enable for specific models
    }

    @Override
    public boolean supportsAudio() {
        return false;
    }

    @Override
    public double estimateCost(AIRequest request) {
        // Local inference - no direct cost
        // Could estimate compute cost if needed
        return 0.0;
    }

    // Additional helper methods...
}
```

### Integration with druiz POC

**Approach**: Incorporate proven patterns from POC while adding security

**Reusable Components**:
1. ✅ LangChain/LangGraph workflow structure
2. ✅ Multi-step query validation
3. ✅ Database view approach
4. ✅ HTML response formatting

**Security Enhancements**:
1. ✅ Replace subprocess with secure execution
2. ✅ Add SecureDatabaseQueryExecutor layer
3. ✅ Integrate user permissions
4. ✅ Add audit logging
5. ✅ Implement rate limiting
6. ✅ Secure credential management

**Migration Path**:
```
druiz POC Component → com.cloudempiere.ai Integration
─────────────────────────────────────────────────────
ChatbotUtils.java   → OllamaProvider.java (secure execution)
pocSQLAgent.py      → langchain_agent.py (enhanced security)
MSQLAIConfigurator  → MAIProvider (unified config)
ChatBotForm         → AIQueryWindow (new ZK UI)
```

---

## Phase 3: Unified Provider Architecture

**Duration**: 2-3 weeks

### Goal: Seamless Provider Switching

**Implementation**:

```java
// Application code doesn't care which provider is used
IAIProviderFactory factory = ...; // OSGi service
IAIProvider provider = factory.getProvider(ctx, providerId, trxName);

// Same interface for all providers
AIRequest request = new AIRequest.Builder()
    .userMessage("Generate a summary")
    .maxTokens(500)
    .build();

AIResponse response = provider.generateText(request);
```

**Provider Selection Logic**:

```java
public class AIProviderSelector {

    /**
     * Select best provider for use case
     */
    public IAIProvider selectProvider(
        Properties ctx,
        AIRequestType requestType,
        boolean requiresDBAccess,
        int estimatedTokens
    ) {

        // If DB access required → must use local LLM
        if (requiresDBAccess) {
            return getLocalLLMProvider(ctx);
        }

        // If high volume → prefer local to save costs
        if (estimatedTokens > 50000) {
            IAIProvider local = getLocalLLMProvider(ctx);
            if (local != null && local.checkHealth().isHealthy()) {
                return local;
            }
        }

        // If compliance requires on-premise → local only
        if (requiresOnPremise(ctx)) {
            return getLocalLLMProvider(ctx);
        }

        // Otherwise, prefer external API for simplicity
        return getExternalAPIProvider(ctx);
    }
}
```

---

## Phase 4: Database Schema Implementation

**Duration**: 2 weeks
**Depends On**: Architecture finalized

### Implementation Priority

**High Priority Tables** (Implement First):
1. ✅ AIG_Provider (DONE - model classes exist)
2. ✅ AIG_Task_Type
3. ✅ AIG_Provider_Task
4. ✅ AIG_Request
5. ✅ AIG_Request_Attachment

**Security Tables** (Phase 1 requirement):
6. ✅ AIG_User_Permission (NEW - for security)
7. ✅ AIG_Query_Audit (NEW - for audit logging)

**Analytics Tables** (Lower priority):
8. ⏳ AIG_Usage_Stats
9. ⏳ AIG_Feedback
10. ⏳ AIG_Model_Training

**Integration Tables** (Future):
11. ⏳ AIG_Workflow_Step
12. ⏳ AIG_Business_Rule

### Migration Scripts

**PostgreSQL**: `/migration/postgresql/202511XX_AIG_Complete_Schema.sql`
**Oracle**: `/migration/oracle/202511XX_AIG_Complete_Schema.sql`

**Security Views**:
```sql
-- Create secure views for AI queries
-- See Phase 1, Section 2 for details
CREATE OR REPLACE VIEW v_ai_product_info AS ...;
CREATE OR REPLACE VIEW v_ai_order_info AS ...;
CREATE OR REPLACE VIEW v_ai_bpartner_info AS ...;
```

---

## Phase 5: Use Case Implementation

**Duration**: 8-12 weeks (parallel work streams)

### Use Case Prioritization

**Tier 1: Quick Wins (Weeks 1-4)**
1. **Content Generation** (External API)
   - Product description enhancement
   - Email writing improvement
   - Translation
   - **Estimated ROI**: High
   - **Risk**: Low

2. **Document Classification** (External API)
   - Request categorization
   - Auto-labeling
   - Sentiment analysis
   - **Estimated ROI**: Medium
   - **Risk**: Low

**Tier 2: High Value (Weeks 5-8)**
3. **Data Queries** (Local LLM + DB Access)
   - Natural language BI
   - Report generation
   - Cross-table analysis
   - **Estimated ROI**: Very High
   - **Risk**: High (requires Phase 1 security)

4. **Document Processing** (External API)
   - Invoice OCR (AWS Textract)
   - Import data normalization
   - Email cleaning
   - **Estimated ROI**: High
   - **Risk**: Medium

**Tier 3: Advanced (Weeks 9-12)**
5. **Workflow Integration** (Both)
   - AI-powered workflow steps
   - Business rule automation
   - Approval routing
   - **Estimated ROI**: Medium
   - **Risk**: Medium

6. **Analytics & Insights** (Both)
   - Chart explanation
   - Opportunity summarization
   - Relationship health scoring
   - **Estimated ROI**: Medium
   - **Risk**: Low

---

## Security Hardening Checklist

### Pre-Production Requirements

**Database Access Security**:
- [ ] SecureDatabaseQueryExecutor implemented and tested
- [ ] Secure database views created with proper RLS
- [ ] Query whitelisting configured
- [ ] SQL injection prevention validated
- [ ] Row-level security tested
- [ ] Resource limits enforced
- [ ] Audit logging complete

**User Permission Integration**:
- [ ] AIG_User_Permission table created
- [ ] Role-based access control implemented
- [ ] Permission checks integrated in all query paths
- [ ] Rate limiting configured
- [ ] Abuse prevention tested

**Credential Management**:
- [ ] Database credentials externalized
- [ ] API keys encrypted in database
- [ ] Secure vault integration (if applicable)
- [ ] No hardcoded secrets in code

**Execution Security**:
- [ ] Python subprocess replaced with secure executor
- [ ] Parameter sanitization validated
- [ ] Command injection testing passed
- [ ] Resource limits enforced (memory, CPU, timeout)
- [ ] Sandboxing implemented (if applicable)

**Audit & Monitoring**:
- [ ] All query attempts logged
- [ ] Failed access attempts tracked
- [ ] Performance monitoring in place
- [ ] Alert thresholds configured
- [ ] Security event notifications enabled

**Testing**:
- [ ] Penetration testing completed
- [ ] Security audit passed
- [ ] Load testing under realistic conditions
- [ ] Failure scenarios tested
- [ ] Recovery procedures documented

---

## Testing Strategy

### Unit Tests

**Provider Tests**:
- [ ] AnthropicProvider (already has test class)
- [ ] OllamaProvider
- [ ] SecureDatabaseQueryExecutor
- [ ] SecurePythonExecutor
- [ ] AIProviderFactory

**Security Tests**:
- [ ] SQL injection attempts
- [ ] Command injection attempts
- [ ] Permission bypass attempts
- [ ] Rate limit validation
- [ ] Resource exhaustion prevention

### Integration Tests

**End-to-End Workflows**:
- [ ] External API request flow
- [ ] Local LLM request flow
- [ ] Database query flow with security
- [ ] Provider failover
- [ ] Cost tracking accuracy

### Performance Tests

**Load Testing**:
- [ ] Concurrent requests handling
- [ ] Database query performance
- [ ] LLM inference latency
- [ ] Resource utilization under load

### Security Tests

**Penetration Testing**:
- [ ] SQL injection resistance
- [ ] Command injection resistance
- [ ] Authentication bypass attempts
- [ ] Authorization bypass attempts
- [ ] Data exfiltration attempts

---

## Deployment Plan

### Development Environment

**Prerequisites**:
1. PostgreSQL database (iDempiere)
2. Ollama installed (for local LLM testing)
3. Python 3.11+ with LangChain
4. Anthropic API key (for external API testing)

**Setup Steps**:
```bash
# 1. Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 2. Pull LLM model
ollama pull llama3.1:8b

# 3. Start Ollama service
ollama serve

# 4. Install Python dependencies
pip install langchain langchain-community psycopg2-binary

# 5. Run database migrations
psql -U idempiere -d idempiere -f migration/postgresql/202511XX_AIG_Schema.sql

# 6. Deploy com.cloudempiere.ai plugin to iDempiere
```

### Production Environment

**Infrastructure Requirements**:

**For External API Only**:
- iDempiere server
- Internet connectivity for API calls
- API key management

**For Local LLM**:
- Dedicated server with GPU (recommended)
  - NVIDIA GPU with 16GB+ VRAM for 8B models
  - 32GB+ system RAM
  - SSD storage
- Ollama service
- Python runtime environment
- Secure network isolation
- Monitoring infrastructure

**Deployment Checklist**:
- [ ] Database migrations applied
- [ ] Plugin deployed and activated
- [ ] Secure views created
- [ ] User permissions configured
- [ ] Provider configurations tested
- [ ] Health checks passing
- [ ] Monitoring enabled
- [ ] Backup procedures in place
- [ ] Rollback plan documented

---

## Cost Analysis

### External API (Anthropic Claude)

**Pricing (as of 2025)**:
- Claude 3.5 Sonnet: $3/1M input tokens, $15/1M output tokens
- Claude 3 Haiku: $0.25/1M input tokens, $1.25/1M output tokens

**Estimated Monthly Cost (Medium Volume)**:
- 10,000 requests/month
- Average 500 input tokens, 200 output tokens per request
- Using Claude 3.5 Sonnet:
  - Input: (10,000 × 500) / 1,000,000 × $3 = $15
  - Output: (10,000 × 200) / 1,000,000 × $15 = $30
  - **Total: $45/month**

### Local LLM (Ollama)

**Initial Setup Cost**:
- GPU Server: $2,000 - $5,000 (one-time)
- OR Cloud GPU instance: $0.50 - $2.00/hour

**Operating Cost**:
- Electricity: ~$50/month (24/7 operation)
- Maintenance: $100/month (labor)
- **Total: ~$150/month recurring**

**Break-even Analysis**:
- External API: $45/month + no infrastructure
- Local LLM: $150/month + $3,000 upfront
- Break-even: ~67 months (5.5 years) at current volume

**Conclusion**:
- Low volume (< 10K requests/month): External API cheaper
- High volume (> 100K requests/month): Local LLM cheaper
- **Hybrid approach**: Use both based on use case

---

## Risk Assessment

### High Risks

1. **SQL Injection from LLM-generated queries** ⚠️ CRITICAL
   - **Mitigation**: SecureDatabaseQueryExecutor with comprehensive validation
   - **Status**: Planned (Phase 1)

2. **Unauthorized data access through AI queries** ⚠️ HIGH
   - **Mitigation**: Row-level security + user permissions
   - **Status**: Planned (Phase 1)

3. **Command injection in Python subprocess** ⚠️ CRITICAL
   - **Mitigation**: SecurePythonExecutor with sanitization
   - **Status**: Planned (Phase 1)

4. **Resource exhaustion from malicious queries** ⚠️ MEDIUM
   - **Mitigation**: Rate limiting + resource caps
   - **Status**: Planned (Phase 1)

### Medium Risks

5. **LLM generating incorrect SQL** ⚠️ MEDIUM
   - **Mitigation**: Multi-step validation workflow
   - **Status**: Partially addressed in druiz POC

6. **External API dependency** ⚠️ LOW-MEDIUM
   - **Mitigation**: Provider failover + local fallback
   - **Status**: Planned (Phase 3)

7. **Data privacy with external APIs** ⚠️ MEDIUM
   - **Mitigation**: Configuration to disable for sensitive data
   - **Status**: Design decision (Phase 3)

### Low Risks

8. **Cost overruns** ⚠️ LOW
   - **Mitigation**: Budget alerts + cost tracking
   - **Status**: Planned (Phase 4)

---

## Success Metrics

### Technical Metrics

**Performance**:
- Average response time < 3 seconds (external API)
- Average response time < 5 seconds (local LLM)
- 99.9% uptime
- Query success rate > 95%

**Security**:
- Zero unauthorized data access incidents
- Zero SQL injection exploits
- 100% audit log coverage
- All security tests passed

**Quality**:
- LLM-generated SQL accuracy > 90%
- User satisfaction score > 4/5
- False positive rate < 5%

### Business Metrics

**Adoption**:
- 50+ active users within 3 months
- 1,000+ queries per month
- 80% user retention

**Value**:
- 50% reduction in manual data entry (OCR)
- 30% faster response to customer requests
- 20% improvement in data quality

**Cost**:
- Stay within budget ($500/month)
- ROI > 200% within 12 months

---

## Timeline Summary

| Phase | Duration | Start | End | Dependencies |
|-------|----------|-------|-----|--------------|
| Phase 1: Security Foundation | 4-6 weeks | Week 1 | Week 6 | None |
| Phase 2: Ollama Provider | 3-4 weeks | Week 5 | Week 9 | Phase 1 (80%) |
| Phase 3: Unified Architecture | 2-3 weeks | Week 7 | Week 10 | Phase 1, 2 |
| Phase 4: Database Schema | 2 weeks | Week 9 | Week 11 | Phase 3 |
| **Phase 6: Context Integration** | **3-4 weeks** | **Week 11** | **Week 15** | **Phase 3** |
| Phase 5: Use Cases (Tier 1) | 4 weeks | Week 15 | Week 19 | Phase 4, 6 |
| Phase 5: Use Cases (Tier 2) | 4 weeks | Week 19 | Week 23 | Phase 4, 6 |
| Phase 5: Use Cases (Tier 3) | 4 weeks | Week 23 | Week 27 | Phase 4, 6 |
| Testing & Hardening | 2 weeks | Week 27 | Week 29 | All phases |
| Documentation & Training | 1 week | Week 29 | Week 30 | All phases |
| **Total** | **30 weeks** | - | **7.5 months** | - |

**Critical Path**: Phase 1 → Phase 2 → Phase 3 → Phase 6 → Phase 5 (All Tiers)

**Note**: Phase 6 (Context Integration) can run in parallel with Phase 5 (Tier 1) if resources allow, potentially reducing total time to 26-28 weeks.

---

## Next Steps (Immediate Actions)

### Week 1-2: Planning & Design Review

1. **Review this implementation plan** with stakeholders
2. **Prioritize security requirements** - confirm Phase 1 scope
3. **Allocate resources** - assign developers
4. **Set up development environment**
5. **Create detailed security design document**

### Week 3-4: Phase 1 Kickoff

6. **Implement SecureDatabaseQueryExecutor**
7. **Create secure database views**
8. **Implement AIG_User_Permission table**
9. **Build SecurePythonExecutor**
10. **Write security unit tests**

### Ongoing

11. **Weekly security review meetings**
12. **Continuous threat modeling**
13. **Regular code reviews with security focus**
14. **Documentation as we go**

---

## Phase 6: iDempiere Context Integration (CRITICAL FOR USER EXPERIENCE)

**Duration**: 3-4 weeks
**Priority**: HIGH - Required for context-aware AI interactions

### Overview

For AI to be truly useful in iDempiere, it must understand the **full context** of what the user is doing:
- Which window/tab/form they're viewing
- What record they're working on
- Dashboard charts and their data
- User's permissions and organizational context
- Session state and preferences

This section describes a **generic, extensible solution** for capturing and passing iDempiere context to AI providers.

---

### Understanding iDempiere Context System

#### Context Hierarchy

iDempiere uses a hierarchical context system stored in `Properties ctx`:

```
Global Context (#Variables)
  └─ Window Context (WindowNo)
      └─ Tab Context (WindowNo|TabNo)
          └─ Field Context (WindowNo|TabNo|FieldName)
```

**Key Context Variables**:

| Variable | Scope | Example | Description |
|----------|-------|---------|-------------|
| `#AD_Client_ID` | Global | 1000000 | Current client (tenant) |
| `#AD_Org_ID` | Global | 1000001 | Current organization |
| `#AD_User_ID` | Global | 100 | Logged in user |
| `#AD_Role_ID` | Global | 1000000 | Active role |
| `#Date` | Global | 2025-11-17 | System date |
| `#Language` | Global | en_US | User's language |
| `WindowNo|WindowName` | Window | 143 | Window being viewed |
| `WindowNo|AD_Window_ID` | Window | 143 | Window ID |
| `WindowNo|TableName` | Window | C_Order | Main table |
| `WindowNo|Record_ID` | Window | 1000234 | Current record ID |
| `WindowNo|TabNo|FieldName` | Tab/Field | New Value | Field value |

**Context Access Patterns**:

```java
// Global context
int clientId = Env.getAD_Client_ID(ctx);
int userId = Env.getAD_User_ID(ctx);
String language = Env.getContext(ctx, "#Language");

// Window context (WindowNo = unique window instance)
int windowId = Env.getContextAsInt(ctx, windowNo, "AD_Window_ID");
String tableName = Env.getContext(ctx, windowNo, "TableName");
int recordId = Env.getContextAsInt(ctx, windowNo, "Record_ID");

// Tab context (WindowNo|TabNo)
String fieldValue = Env.getContext(ctx, windowNo, tabNo, "FieldName");
```

---

### Architecture: AI Context Provider System

#### Component: AIContextProvider

**Purpose**: Generic interface for extracting context from any iDempiere component

**Design Pattern**: Strategy pattern with component-specific extractors

```java
package com.cloudempiere.ai.context;

import java.util.Properties;
import org.json.JSONObject;

/**
 * Generic interface for extracting context from iDempiere components
 * for AI processing.
 *
 * Implementations exist for:
 * - Window/Tab context
 * - Dashboard/Chart context
 * - Process context
 * - Report context
 * - Form context
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIContextProvider {

    /**
     * Get the type of context this provider handles
     *
     * @return context type (e.g., "WINDOW", "CHART", "PROCESS")
     */
    String getContextType();

    /**
     * Extract context information and serialize to JSON
     *
     * This method captures all relevant information from the
     * iDempiere component needed for AI to understand the
     * user's current situation.
     *
     * @param ctx iDempiere context (Properties)
     * @param windowNo window number (if applicable)
     * @param parameters additional component-specific parameters
     * @return JSON object containing structured context
     */
    JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    );

    /**
     * Validate that extracted context is complete and valid
     *
     * @param context extracted context JSON
     * @return true if context is valid
     */
    boolean validateContext(JSONObject context);

    /**
     * Get security-sensitive fields that should be redacted
     *
     * @return list of field names to redact (e.g., "Password", "CreditCardNumber")
     */
    String[] getSensitiveFields();
}
```

#### Component: ContextParameters

**Purpose**: Flexible parameter passing for different component types

```java
package com.cloudempiere.ai.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for component-specific parameters when extracting context
 */
public class ContextParameters {

    private Map<String, Object> parameters = new HashMap<>();

    public ContextParameters put(String key, Object value) {
        parameters.put(key, value);
        return this; // Fluent API
    }

    public Object get(String key) {
        return parameters.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(parameters.get(key));
    }

    public boolean has(String key) {
        return parameters.containsKey(key);
    }

    // Builder methods for common parameters

    public static ContextParameters forWindow(int windowNo, int tabNo) {
        return new ContextParameters()
            .put("windowNo", windowNo)
            .put("tabNo", tabNo);
    }

    public static ContextParameters forChart(int chartId, String sql) {
        return new ContextParameters()
            .put("chartId", chartId)
            .put("sql", sql);
    }

    public static ContextParameters forProcess(int processId) {
        return new ContextParameters()
            .put("processId", processId);
    }
}
```

---

### Implementation: Chart Context Provider (Example)

**Use Case**: User clicks "Explain this chart" on a dashboard

**Context Needed**:
- Chart metadata (title, type, period)
- SQL query that generated the data
- Query results (actual data)
- Chart rendering configuration (JSON model)
- JavaScript chart definition
- User's filtering parameters
- Dashboard context (which dashboard, which panel)

**Implementation**:

```java
package com.cloudempiere.ai.context.impl;

import org.compiere.model.MChart;
import org.compiere.model.MChartDatasource;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.json.JSONArray;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.context.ContextParameters;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Properties;

/**
 * Context provider for Billboard.js dashboard charts
 *
 * Extracts comprehensive context about a chart including:
 * - Chart metadata
 * - SQL source query
 * - Query results (data)
 * - JSON chart model (rendering config)
 * - JavaScript chart definition
 * - User filter parameters
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ChartContextProvider implements IAIContextProvider {

    @Override
    public String getContextType() {
        return "CHART";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // 1. Extract basic user context
            addUserContext(context, ctx);

            // 2. Extract chart metadata
            int chartId = parameters.get("chartId", Integer.class);
            MChart chart = MChart.get(ctx, chartId);

            if (chart == null) {
                throw new IllegalArgumentException("Chart not found: " + chartId);
            }

            addChartMetadata(context, chart, ctx);

            // 3. Extract SQL query with context variable substitution
            String sql = parameters.get("sql", String.class);
            String parsedSQL = Env.parseContext(ctx, windowNo, sql, false);
            context.put("sql_query", parsedSQL);

            // 4. Execute query and capture results
            addQueryResults(context, ctx, parsedSQL, 100); // Limit to 100 rows

            // 5. Extract chart model (JSON configuration)
            String chartModel = parameters.get("chartModel", String.class);
            if (chartModel != null) {
                context.put("chart_model", new JSONObject(chartModel));
            }

            // 6. Extract JavaScript chart definition
            String chartJS = parameters.get("chartJS", String.class);
            if (chartJS != null) {
                context.put("chart_javascript", chartJS);
            }

            // 7. Extract chart parameters (user filters)
            addChartParameters(context, chart, ctx, windowNo);

            // 8. Extract datasource information
            addDatasourceInfo(context, chart);

            // 9. Add dashboard context if available
            if (parameters.has("dashboardId")) {
                addDashboardContext(
                    context,
                    parameters.get("dashboardId", Integer.class),
                    ctx
                );
            }

            // 10. Add rendering metadata
            addRenderingMetadata(context, parameters);

        } catch (Exception e) {
            context.put("error", "Failed to extract context: " + e.getMessage());
            context.put("success", false);
            return context;
        }

        context.put("success", true);
        return context;
    }

    /**
     * Add user context (who is viewing this chart)
     */
    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userContext = new JSONObject();
        userContext.put("user_id", Env.getAD_User_ID(ctx));
        userContext.put("user_name", Env.getContext(ctx, "#AD_User_Name"));
        userContext.put("client_id", Env.getAD_Client_ID(ctx));
        userContext.put("client_name", Env.getContext(ctx, "#AD_Client_Name"));
        userContext.put("org_id", Env.getAD_Org_ID(ctx));
        userContext.put("org_name", Env.getContext(ctx, "#AD_Org_Name"));
        userContext.put("role_id", Env.getAD_Role_ID(ctx));
        userContext.put("role_name", Env.getContext(ctx, "#AD_Role_Name"));
        userContext.put("language", Env.getContext(ctx, "#Language"));
        userContext.put("date", Env.getContext(ctx, "#Date"));

        context.put("user_context", userContext);
    }

    /**
     * Add chart metadata
     */
    private void addChartMetadata(
        JSONObject context,
        MChart chart,
        Properties ctx
    ) {
        JSONObject chartMeta = new JSONObject();
        chartMeta.put("chart_id", chart.getAD_Chart_ID());
        chartMeta.put("name", chart.getName());
        chartMeta.put("description", chart.getDescription());
        chartMeta.put("chart_type", chart.getChartType());
        chartMeta.put("time_unit", chart.getTimeUnit());
        chartMeta.put("time_scope", chart.getTimeScope());

        // Add translated name/description if available
        String language = Env.getContext(ctx, "#Language");
        if (language != null && !language.equals("en_US")) {
            // Get translation if exists
            String translatedName = chart.get_Translation("Name", language);
            if (translatedName != null) {
                chartMeta.put("name_translated", translatedName);
            }
        }

        context.put("chart_metadata", chartMeta);
    }

    /**
     * Execute query and capture results
     */
    private void addQueryResults(
        JSONObject context,
        Properties ctx,
        String sql,
        int maxRows
    ) {
        JSONArray results = new JSONArray();
        JSONArray columnNames = new JSONArray();
        JSONArray columnTypes = new JSONArray();

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();

            // Get column metadata
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.put(rsmd.getColumnName(i));
                columnTypes.put(rsmd.getColumnTypeName(i));
            }

            // Get data rows (limited)
            int rowCount = 0;
            while (rs.next() && rowCount < maxRows) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value != null ? value.toString() : null);
                }
                results.put(row);
                rowCount++;
            }

            JSONObject queryResults = new JSONObject();
            queryResults.put("columns", columnNames);
            queryResults.put("column_types", columnTypes);
            queryResults.put("rows", results);
            queryResults.put("row_count", rowCount);
            queryResults.put("truncated", rs.next()); // More rows available?

            context.put("query_results", queryResults);

        } catch (Exception e) {
            context.put("query_error", e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Add chart parameters (filters, date ranges, etc.)
     */
    private void addChartParameters(
        JSONObject context,
        MChart chart,
        Properties ctx,
        int windowNo
    ) {
        JSONArray parameters = new JSONArray();

        for (MChartPara para : chart.getParameters()) {
            JSONObject paramObj = new JSONObject();
            paramObj.put("name", para.getName());
            paramObj.put("column_name", para.getColumnName());
            paramObj.put("reference_id", para.getAD_Reference_ID());

            // Get actual value from context
            String value = Env.getContext(ctx, windowNo, para.getColumnName());
            paramObj.put("value", value);

            parameters.put(paramObj);
        }

        context.put("chart_parameters", parameters);
    }

    /**
     * Add datasource information
     */
    private void addDatasourceInfo(
        JSONObject context,
        MChart chart
    ) {
        JSONArray datasources = new JSONArray();

        for (ChartDatasource ds : chart.getDatasources()) {
            JSONObject dsObj = new JSONObject();
            dsObj.put("name", ds.getName());
            dsObj.put("description", ds.getDescription());
            dsObj.put("entity_type", ds.getEntityType());
            dsObj.put("from_clause", ds.getFromClause());
            dsObj.put("where_clause", ds.getWhereClause());
            dsObj.put("date_column", ds.getDateColumn());
            dsObj.put("value_column", ds.getValueColumn());
            dsObj.put("category_column", ds.getCategoryColumn());

            datasources.put(dsObj);
        }

        context.put("datasources", datasources);
    }

    /**
     * Add dashboard context (which dashboard panel)
     */
    private void addDashboardContext(
        JSONObject context,
        int dashboardId,
        Properties ctx
    ) {
        JSONObject dashboardContext = new JSONObject();
        dashboardContext.put("dashboard_id", dashboardId);

        // Could fetch dashboard name, layout, other panels, etc.
        // For now, just the ID

        context.put("dashboard_context", dashboardContext);
    }

    /**
     * Add rendering metadata
     */
    private void addRenderingMetadata(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject rendering = new JSONObject();

        if (parameters.has("chartWidth")) {
            rendering.put("width", parameters.get("chartWidth"));
        }
        if (parameters.has("chartHeight")) {
            rendering.put("height", parameters.get("chartHeight"));
        }
        if (parameters.has("colorScheme")) {
            rendering.put("color_scheme", parameters.get("colorScheme"));
        }

        context.put("rendering", rendering);
    }

    @Override
    public boolean validateContext(JSONObject context) {
        // Validate required fields
        return context.has("chart_metadata") &&
               context.has("sql_query") &&
               context.has("query_results") &&
               context.getBoolean("success");
    }

    @Override
    public String[] getSensitiveFields() {
        // Fields that should be redacted if present in data
        return new String[] {
            "Password",
            "CreditCardNumber",
            "CVV",
            "SSN",
            "TaxID",
            "BankAccount"
        };
    }
}
```

---

### Implementation: Window Context Provider

**Use Case**: User asks AI for help with a specific record/window

```java
package com.cloudempiere.ai.context.impl;

import org.compiere.model.GridTab;
import org.compiere.model.GridField;
import org.compiere.model.MWindow;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.json.JSONArray;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.context.ContextParameters;
import java.util.Properties;

/**
 * Context provider for iDempiere windows/tabs
 *
 * Extracts context about the current window the user is viewing,
 * including record data, field definitions, and window metadata.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class WindowContextProvider implements IAIContextProvider {

    @Override
    public String getContextType() {
        return "WINDOW";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // 1. User context
            addUserContext(context, ctx);

            // 2. Window metadata
            int windowId = Env.getContextAsInt(ctx, windowNo, "AD_Window_ID");
            MWindow window = MWindow.get(ctx, windowId);

            if (window != null) {
                addWindowMetadata(context, window);
            }

            // 3. Current tab and record info
            int tabNo = parameters.get("tabNo", Integer.class);
            addTabContext(context, ctx, windowNo, tabNo);

            // 4. Record data (current record values)
            addRecordData(context, ctx, windowNo, tabNo);

            // 5. Related tabs/records if requested
            if (parameters.has("includeChildTabs")) {
                addChildTabsData(context, ctx, windowNo);
            }

            context.put("success", true);

        } catch (Exception e) {
            context.put("error", e.getMessage());
            context.put("success", false);
        }

        return context;
    }

    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userCtx = new JSONObject();
        userCtx.put("user_id", Env.getAD_User_ID(ctx));
        userCtx.put("client_id", Env.getAD_Client_ID(ctx));
        userCtx.put("org_id", Env.getAD_Org_ID(ctx));
        userCtx.put("role_id", Env.getAD_Role_ID(ctx));
        context.put("user_context", userCtx);
    }

    private void addWindowMetadata(JSONObject context, MWindow window) {
        JSONObject windowMeta = new JSONObject();
        windowMeta.put("window_id", window.getAD_Window_ID());
        windowMeta.put("name", window.getName());
        windowMeta.put("description", window.getDescription());
        windowMeta.put("help", window.getHelp());
        windowMeta.put("window_type", window.getWindowType());

        context.put("window_metadata", windowMeta);
    }

    private void addTabContext(
        JSONObject context,
        Properties ctx,
        int windowNo,
        int tabNo
    ) {
        JSONObject tabCtx = new JSONObject();

        String tableName = Env.getContext(ctx, windowNo, tabNo, "TableName", false);
        int recordId = Env.getContextAsInt(ctx, windowNo, "Record_ID");

        tabCtx.put("table_name", tableName);
        tabCtx.put("record_id", recordId);
        tabCtx.put("tab_level", Env.getContextAsInt(ctx, windowNo, tabNo, "TabLevel", false));

        context.put("tab_context", tabCtx);
    }

    private void addRecordData(
        JSONObject context,
        Properties ctx,
        int windowNo,
        int tabNo
    ) {
        JSONObject recordData = new JSONObject();

        // Extract all context variables for this tab
        // Format: WindowNo|TabNo|FieldName
        String prefix = windowNo + "|" + tabNo + "|";

        for (Object key : ctx.keySet()) {
            String keyStr = key.toString();
            if (keyStr.startsWith(prefix)) {
                String fieldName = keyStr.substring(prefix.length());
                String value = ctx.getProperty(keyStr);
                recordData.put(fieldName, value);
            }
        }

        context.put("record_data", recordData);
    }

    private void addChildTabsData(
        JSONObject context,
        Properties ctx,
        int windowNo
    ) {
        // Implementation would iterate through child tabs
        // and extract their data as well
        JSONArray childTabs = new JSONArray();
        // ... implementation details
        context.put("child_tabs", childTabs);
    }

    @Override
    public boolean validateContext(JSONObject context) {
        return context.has("window_metadata") &&
               context.has("tab_context") &&
               context.getBoolean("success");
    }

    @Override
    public String[] getSensitiveFields() {
        return new String[] {
            "Password",
            "UserPIN",
            "CreditCardNumber",
            "CVV",
            "SSN",
            "BankAccount",
            "APIKey"
        };
    }
}
```

---

### Context Provider Registry

**Purpose**: Central registry for managing context providers

```java
package com.cloudempiere.ai.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.compiere.util.CLogger;

/**
 * Registry for AI context providers
 *
 * Manages different context provider implementations
 * and allows dynamic registration of new providers.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIContextProviderRegistry {

    private static final CLogger log = CLogger.getCLogger(AIContextProviderRegistry.class);

    private static AIContextProviderRegistry instance;

    private Map<String, IAIContextProvider> providers = new ConcurrentHashMap<>();

    private AIContextProviderRegistry() {
        // Register default providers
        registerDefaultProviders();
    }

    public static synchronized AIContextProviderRegistry getInstance() {
        if (instance == null) {
            instance = new AIContextProviderRegistry();
        }
        return instance;
    }

    private void registerDefaultProviders() {
        register(new ChartContextProvider());
        register(new WindowContextProvider());
        register(new ProcessContextProvider());
        register(new ReportContextProvider());
        register(new FormContextProvider());

        log.info("Registered " + providers.size() + " context providers");
    }

    /**
     * Register a context provider
     */
    public void register(IAIContextProvider provider) {
        String type = provider.getContextType();
        providers.put(type, provider);
        log.fine("Registered context provider: " + type);
    }

    /**
     * Get context provider by type
     */
    public IAIContextProvider getProvider(String contextType) {
        return providers.get(contextType);
    }

    /**
     * Check if provider exists for type
     */
    public boolean hasProvider(String contextType) {
        return providers.containsKey(contextType);
    }
}
```

---

### Integration with AIRequest

**Updated AIRequest to support context**:

```java
package com.cloudempiere.ai.provider.dto;

import org.json.JSONObject;
import java.util.Properties;

/**
 * AI Request with iDempiere context support
 */
public class AIRequest {

    // Existing fields...
    private String model;
    private List<AIMessage> messages;
    private Integer maxTokens;
    private Double temperature;

    // NEW: Context support
    private JSONObject idempiereContext;
    private Properties ctx;
    private int windowNo;
    private String trxName;

    // Builder pattern
    public static class Builder {

        private AIRequest request = new AIRequest();

        // Existing methods...
        public Builder model(String model) {
            request.model = model;
            return this;
        }

        // NEW: Context methods
        public Builder withContext(Properties ctx, int windowNo) {
            request.ctx = ctx;
            request.windowNo = windowNo;
            return this;
        }

        public Builder withTransactionName(String trxName) {
            request.trxName = trxName;
            return this;
        }

        public Builder withExtractedContext(
            String contextType,
            ContextParameters parameters
        ) {
            // Automatically extract context
            AIContextProviderRegistry registry = AIContextProviderRegistry.getInstance();
            IAIContextProvider provider = registry.getProvider(contextType);

            if (provider != null) {
                JSONObject context = provider.extractContext(
                    request.ctx,
                    request.windowNo,
                    parameters
                );
                request.idempiereContext = context;
            }

            return this;
        }

        public Builder withCustomContext(JSONObject context) {
            request.idempiereContext = context;
            return this;
        }

        public AIRequest build() {
            return request;
        }
    }

    // Getters
    public JSONObject getIdempiereContext() {
        return idempiereContext;
    }

    public Properties getContext() {
        return ctx;
    }

    public int getWindowNo() {
        return windowNo;
    }

    public String getTransactionName() {
        return trxName;
    }
}
```

---

### Usage Examples

#### Example 1: Explain Chart

```java
// User clicks "Explain this chart" button on dashboard

// 1. Build context parameters
ContextParameters params = ContextParameters.forChart(chartId, sqlQuery)
    .put("chartModel", chartModelJSON)
    .put("chartJS", chartJavaScript)
    .put("dashboardId", dashboardId);

// 2. Create AI request with auto-extracted context
AIRequest request = new AIRequest.Builder()
    .withContext(ctx, windowNo)
    .withExtractedContext("CHART", params)
    .model("claude-3-5-sonnet-20240620")
    .userMessage("Explain what this chart shows and provide key insights.")
    .maxTokens(1000)
    .build();

// 3. Send to AI provider
IAIProvider provider = factory.getProvider(ctx, providerId, trxName);
AIResponse response = provider.generateText(request);

// 4. Display explanation to user
showChartExplanation(response.getContent());
```

#### Example 2: Help with Current Record

```java
// User clicks "AI Help" button on a window

// 1. Extract window/tab context
ContextParameters params = ContextParameters.forWindow(windowNo, tabNo)
    .put("includeChildTabs", true);

// 2. Create AI request
AIRequest request = new AIRequest.Builder()
    .withContext(ctx, windowNo)
    .withExtractedContext("WINDOW", params)
    .model("gpt-4")
    .systemPrompt("You are an ERP assistant helping users understand their data.")
    .userMessage("What does this record mean? Are there any issues I should know about?")
    .maxTokens(500)
    .build();

// 3. Get AI response
AIResponse response = provider.generateText(request);

// 4. Show in help panel
showContextHelp(response.getContent());
```

#### Example 3: Database Query with Context

```java
// User asks a question about their data

// 1. Build request with user context
AIRequest request = new AIRequest.Builder()
    .withContext(ctx, windowNo)
    .model("llama3.1:8b")
    .userMessage("Show me all open orders from last month")
    .addProviderParameter("enable_db_query", true)
    .maxTokens(500)
    .build();

// 2. Provider will:
//    - Extract user's client/org from context
//    - Apply row-level security
//    - Generate SQL with proper WHERE clauses
//    - Execute securely

AIResponse response = ollamaProvider.generateText(request);

// 3. Display results
showQueryResults(response.getContent());
```

---

### Security Considerations for Context

#### 1. Sensitive Field Redaction

```java
/**
 * Redact sensitive fields before sending to AI
 */
public class ContextSanitizer {

    public static JSONObject sanitize(
        JSONObject context,
        IAIContextProvider provider
    ) {
        String[] sensitiveFields = provider.getSensitiveFields();

        JSONObject sanitized = new JSONObject(context.toString());

        // Recursively redact sensitive fields
        redactFields(sanitized, sensitiveFields);

        return sanitized;
    }

    private static void redactFields(
        JSONObject obj,
        String[] sensitiveFields
    ) {
        for (String field : sensitiveFields) {
            if (obj.has(field)) {
                obj.put(field, "[REDACTED]");
            }
        }

        // Recurse into nested objects
        for (String key : obj.keySet()) {
            Object value = obj.get(key);
            if (value instanceof JSONObject) {
                redactFields((JSONObject) value, sensitiveFields);
            } else if (value instanceof JSONArray) {
                redactArray((JSONArray) value, sensitiveFields);
            }
        }
    }

    private static void redactArray(
        JSONArray arr,
        String[] sensitiveFields
    ) {
        for (int i = 0; i < arr.length(); i++) {
            Object value = arr.get(i);
            if (value instanceof JSONObject) {
                redactFields((JSONObject) value, sensitiveFields);
            }
        }
    }
}
```

#### 2. Context Size Limits

```java
/**
 * Enforce maximum context size to prevent token overflow
 */
public class ContextSizeLimiter {

    private static final int MAX_CONTEXT_TOKENS = 10000; // Configurable

    public static JSONObject limitContextSize(
        JSONObject context,
        int maxTokens
    ) {
        // Estimate token count (rough: 4 chars per token)
        int estimatedTokens = context.toString().length() / 4;

        if (estimatedTokens <= maxTokens) {
            return context; // Within limit
        }

        // Truncate less important parts
        JSONObject limited = new JSONObject(context.toString());

        // Keep essential fields
        // Truncate: query_results, child_tabs, etc.
        if (limited.has("query_results")) {
            JSONObject results = limited.getJSONObject("query_results");
            JSONArray rows = results.getJSONArray("rows");

            // Limit to first 50 rows
            if (rows.length() > 50) {
                JSONArray limited_rows = new JSONArray();
                for (int i = 0; i < 50; i++) {
                    limited_rows.put(rows.get(i));
                }
                results.put("rows", limited_rows);
                results.put("truncated", true);
            }
        }

        return limited;
    }
}
```

#### 3. Permission Validation

```java
/**
 * Validate user has permission to share this context with AI
 */
public class ContextPermissionValidator {

    public static boolean canShareContext(
        Properties ctx,
        JSONObject context,
        String contextType
    ) {
        int userId = Env.getAD_User_ID(ctx);

        // Check AI query permission
        String sql = "SELECT AllowAIQueries FROM AIG_User_Permission " +
                     "WHERE AD_User_ID = ? AND IsActive = 'Y'";

        String allowed = DB.getSQLValueString(null, sql, userId);

        if (!"Y".equals(allowed)) {
            return false;
        }

        // Check specific context type permissions
        // (e.g., user might be allowed charts but not financial windows)

        return true;
    }
}
```

---

### Implementation Checklist

**Context Provider System**:
- [ ] Create IAIContextProvider interface
- [ ] Create ContextParameters class
- [ ] Implement ChartContextProvider
- [ ] Implement WindowContextProvider
- [ ] Implement ProcessContextProvider
- [ ] Implement ReportContextProvider
- [ ] Implement FormContextProvider
- [ ] Create AIContextProviderRegistry
- [ ] Update AIRequest to support context

**Security**:
- [ ] Implement ContextSanitizer for sensitive fields
- [ ] Implement ContextSizeLimiter for token limits
- [ ] Implement ContextPermissionValidator
- [ ] Add context audit logging

**Testing**:
- [ ] Unit tests for each context provider
- [ ] Test context extraction accuracy
- [ ] Test sensitive field redaction
- [ ] Test context size limiting
- [ ] Integration tests with AI providers

**Documentation**:
- [ ] Developer guide for creating new context providers
- [ ] User guide for context-aware AI features
- [ ] Security guide for context handling

---

### Extension: Custom Interface Context Providers

**Key Design Principle**: The `IAIContextProvider` interface is **intentionally generic** to support ANY custom interface in iDempiere, including:
- Custom ZK components
- Billboard.js dashboards with custom widgets
- Custom forms and dialogs
- Third-party integrations
- Any client-side JavaScript UI

#### Example: Custom Billboard Dashboard Widget

**Scenario**: You have a custom dashboard widget with:
- Custom SQL queries
- Custom JavaScript rendering logic
- Custom user interactions (filters, drill-downs)
- Custom data transformations
- Client-side state management

**Solution**: Create a custom context provider for your widget type

```java
package com.cloudempiere.ai.context.impl;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.json.JSONArray;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.context.ContextParameters;
import java.util.Properties;

/**
 * Context provider for custom Billboard.js dashboard widgets
 *
 * Handles ANY custom widget type by accepting flexible parameters
 * and extracting all relevant context for AI processing.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class CustomDashboardWidgetContextProvider implements IAIContextProvider {

    @Override
    public String getContextType() {
        return "CUSTOM_WIDGET";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // 1. User context (always included)
            addUserContext(context, ctx);

            // 2. Widget metadata (from parameters)
            addWidgetMetadata(context, parameters);

            // 3. SQL queries (can have multiple)
            if (parameters.has("sqlQueries")) {
                addMultipleSQLQueries(context, ctx, windowNo, parameters);
            }

            // 4. Chart/visualization models (JSON configs)
            if (parameters.has("chartModels")) {
                addChartModels(context, parameters);
            }

            // 5. JavaScript code (client-side logic)
            if (parameters.has("jsCode")) {
                addJavaScriptCode(context, parameters);
            }

            // 6. User interactions/filters (current state)
            if (parameters.has("userFilters")) {
                addUserFilters(context, ctx, windowNo, parameters);
            }

            // 7. Custom data transformations
            if (parameters.has("transformations")) {
                addDataTransformations(context, parameters);
            }

            // 8. Client-side state (from JavaScript)
            if (parameters.has("clientState")) {
                addClientSideState(context, parameters);
            }

            // 9. Related widgets (if part of a dashboard)
            if (parameters.has("relatedWidgets")) {
                addRelatedWidgets(context, parameters);
            }

            // 10. Custom metadata (anything else)
            if (parameters.has("customMetadata")) {
                addCustomMetadata(context, parameters);
            }

            context.put("success", true);

        } catch (Exception e) {
            context.put("error", "Failed to extract context: " + e.getMessage());
            context.put("success", false);
        }

        return context;
    }

    /**
     * Add widget metadata (type, name, description, etc.)
     */
    private void addWidgetMetadata(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject widgetMeta = new JSONObject();

        widgetMeta.put("widget_id", parameters.get("widgetId"));
        widgetMeta.put("widget_type", parameters.get("widgetType"));
        widgetMeta.put("widget_name", parameters.get("widgetName"));
        widgetMeta.put("widget_description", parameters.get("widgetDescription"));

        // Dashboard location
        if (parameters.has("dashboardId")) {
            widgetMeta.put("dashboard_id", parameters.get("dashboardId"));
        }
        if (parameters.has("panelId")) {
            widgetMeta.put("panel_id", parameters.get("panelId"));
        }
        if (parameters.has("position")) {
            widgetMeta.put("position", parameters.get("position"));
        }

        context.put("widget_metadata", widgetMeta);
    }

    /**
     * Add multiple SQL queries (for widgets with multiple data sources)
     */
    private void addMultipleSQLQueries(
        JSONObject context,
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONArray queries = new JSONArray();

        // Get array of SQL queries
        Object sqlQueriesObj = parameters.get("sqlQueries");
        if (sqlQueriesObj instanceof String[]) {
            String[] sqlQueries = (String[]) sqlQueriesObj;

            for (int i = 0; i < sqlQueries.length; i++) {
                JSONObject queryObj = new JSONObject();

                // Parse context variables in SQL
                String parsedSQL = Env.parseContext(ctx, windowNo, sqlQueries[i], false);
                queryObj.put("sql", parsedSQL);
                queryObj.put("index", i);

                // Execute and get results
                JSONObject results = executeQueryWithResults(ctx, parsedSQL, 100);
                queryObj.put("results", results);

                queries.put(queryObj);
            }
        }

        context.put("sql_queries", queries);
    }

    /**
     * Add chart/visualization models (JSON configurations)
     */
    private void addChartModels(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONArray chartModels = new JSONArray();

        Object modelsObj = parameters.get("chartModels");
        if (modelsObj instanceof String[]) {
            String[] models = (String[]) modelsObj;

            for (String modelJSON : models) {
                try {
                    JSONObject model = new JSONObject(modelJSON);
                    chartModels.put(model);
                } catch (Exception e) {
                    // If not valid JSON, store as string
                    chartModels.put(modelJSON);
                }
            }
        } else if (modelsObj instanceof String) {
            // Single model
            try {
                JSONObject model = new JSONObject((String) modelsObj);
                chartModels.put(model);
            } catch (Exception e) {
                chartModels.put(modelsObj);
            }
        }

        context.put("chart_models", chartModels);
    }

    /**
     * Add JavaScript code (rendering logic, event handlers, etc.)
     */
    private void addJavaScriptCode(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject jsCode = new JSONObject();

        // Main rendering code
        if (parameters.has("renderingJS")) {
            jsCode.put("rendering", parameters.get("renderingJS"));
        }

        // Event handlers
        if (parameters.has("eventHandlersJS")) {
            jsCode.put("event_handlers", parameters.get("eventHandlersJS"));
        }

        // Data transformation code
        if (parameters.has("transformJS")) {
            jsCode.put("transformations", parameters.get("transformJS"));
        }

        // Initialization code
        if (parameters.has("initJS")) {
            jsCode.put("initialization", parameters.get("initJS"));
        }

        context.put("javascript_code", jsCode);
    }

    /**
     * Add user filters and interactions
     */
    private void addUserFilters(
        JSONObject context,
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject filters = new JSONObject();

        // Get filter object (could be JSON or Map)
        Object filtersObj = parameters.get("userFilters");

        if (filtersObj instanceof String) {
            // If it's a JSON string
            try {
                filters = new JSONObject((String) filtersObj);
            } catch (Exception e) {
                filters.put("raw", filtersObj);
            }
        } else if (filtersObj instanceof java.util.Map) {
            // If it's a Map
            java.util.Map<?, ?> filterMap = (java.util.Map<?, ?>) filtersObj;
            for (java.util.Map.Entry<?, ?> entry : filterMap.entrySet()) {
                filters.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // Add resolved values from context
        JSONObject resolvedFilters = new JSONObject();
        for (String key : filters.keySet()) {
            Object value = filters.get(key);

            // If value is a context variable reference, resolve it
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("@") && strValue.endsWith("@")) {
                    // Context variable reference
                    String varName = strValue.substring(1, strValue.length() - 1);
                    String resolved = Env.getContext(ctx, windowNo, varName);
                    resolvedFilters.put(key, resolved);
                } else {
                    resolvedFilters.put(key, value);
                }
            } else {
                resolvedFilters.put(key, value);
            }
        }

        context.put("user_filters", filters);
        context.put("resolved_filters", resolvedFilters);
    }

    /**
     * Add data transformations (how data is processed before display)
     */
    private void addDataTransformations(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject transformations = new JSONObject();

        Object transformObj = parameters.get("transformations");
        if (transformObj instanceof String) {
            try {
                transformations = new JSONObject((String) transformObj);
            } catch (Exception e) {
                transformations.put("description", transformObj);
            }
        }

        context.put("data_transformations", transformations);
    }

    /**
     * Add client-side state (from JavaScript variables/localStorage)
     */
    private void addClientSideState(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject clientState = new JSONObject();

        Object stateObj = parameters.get("clientState");
        if (stateObj instanceof String) {
            try {
                clientState = new JSONObject((String) stateObj);
            } catch (Exception e) {
                clientState.put("raw", stateObj);
            }
        }

        context.put("client_state", clientState);
    }

    /**
     * Add related widgets (for context about the whole dashboard)
     */
    private void addRelatedWidgets(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONArray relatedWidgets = new JSONArray();

        Object widgetsObj = parameters.get("relatedWidgets");
        if (widgetsObj instanceof Object[]) {
            Object[] widgets = (Object[]) widgetsObj;
            for (Object widget : widgets) {
                if (widget instanceof String) {
                    try {
                        relatedWidgets.put(new JSONObject((String) widget));
                    } catch (Exception e) {
                        relatedWidgets.put(widget);
                    }
                } else {
                    relatedWidgets.put(widget);
                }
            }
        }

        context.put("related_widgets", relatedWidgets);
    }

    /**
     * Add custom metadata (anything else specific to the widget)
     */
    private void addCustomMetadata(
        JSONObject context,
        ContextParameters parameters
    ) {
        Object metadataObj = parameters.get("customMetadata");

        if (metadataObj instanceof String) {
            try {
                context.put("custom_metadata", new JSONObject((String) metadataObj));
            } catch (Exception e) {
                context.put("custom_metadata", metadataObj);
            }
        } else if (metadataObj instanceof JSONObject) {
            context.put("custom_metadata", metadataObj);
        }
    }

    /**
     * Execute query and return results as JSON
     */
    private JSONObject executeQueryWithResults(
        Properties ctx,
        String sql,
        int maxRows
    ) {
        JSONObject results = new JSONObject();
        JSONArray rows = new JSONArray();
        JSONArray columns = new JSONArray();

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            // Column names
            for (int i = 1; i <= columnCount; i++) {
                columns.put(rsmd.getColumnName(i));
            }

            // Data rows
            int rowCount = 0;
            while (rs.next() && rowCount < maxRows) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(rsmd.getColumnName(i), value != null ? value.toString() : null);
                }
                rows.put(row);
                rowCount++;
            }

            results.put("columns", columns);
            results.put("rows", rows);
            results.put("row_count", rowCount);
            results.put("truncated", rs.next());

        } catch (Exception e) {
            results.put("error", e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return results;
    }

    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userContext = new JSONObject();
        userContext.put("user_id", Env.getAD_User_ID(ctx));
        userContext.put("user_name", Env.getContext(ctx, "#AD_User_Name"));
        userContext.put("client_id", Env.getAD_Client_ID(ctx));
        userContext.put("org_id", Env.getAD_Org_ID(ctx));
        userContext.put("role_id", Env.getAD_Role_ID(ctx));
        userContext.put("language", Env.getContext(ctx, "#Language"));
        context.put("user_context", userContext);
    }

    @Override
    public boolean validateContext(JSONObject context) {
        return context.has("widget_metadata") && context.getBoolean("success");
    }

    @Override
    public String[] getSensitiveFields() {
        return new String[] {
            "Password", "CreditCardNumber", "CVV", "SSN",
            "TaxID", "BankAccount", "APIKey", "Token"
        };
    }
}
```

#### Usage Example: Custom Dashboard Widget

```java
// In your custom ZK dashboard component

/**
 * User clicks "Ask AI about this widget" button
 */
public void onAIHelp(Event event) {

    // 1. Gather ALL context about your custom widget
    ContextParameters params = new ContextParameters()
        .put("widgetId", this.widgetId)
        .put("widgetType", "CUSTOM_BILLBOARD_CHART")
        .put("widgetName", this.getTitle())
        .put("widgetDescription", this.getDescription())
        .put("dashboardId", this.getDashboardId())
        .put("panelId", this.getPanelId())

        // Multiple SQL queries (if your widget has multiple data sources)
        .put("sqlQueries", new String[] {
            this.mainDataQuery,
            this.trendDataQuery,
            this.comparisonQuery
        })

        // Chart models (JSON configs)
        .put("chartModels", new String[] {
            this.billboardChartConfig,  // Billboard.js config
            this.legendConfig,
            this.tooltipConfig
        })

        // JavaScript code
        .put("renderingJS", this.getRenderingJavaScript())
        .put("eventHandlersJS", this.getEventHandlers())
        .put("transformJS", this.getDataTransformationCode())

        // User interactions
        .put("userFilters", this.getCurrentFilters())  // Map or JSON

        // Data transformations
        .put("transformations", this.getTransformationDescription())

        // Client-side state (from JavaScript)
        .put("clientState", this.getClientSideState())  // JSON from JS

        // Related widgets
        .put("relatedWidgets", this.getRelatedWidgetConfigs())

        // Any custom metadata
        .put("customMetadata", this.getCustomMetadata());

    // 2. Create AI request with extracted context
    AIRequest request = new AIRequest.Builder()
        .withContext(Env.getCtx(), this.getWindowNo())
        .withExtractedContext("CUSTOM_WIDGET", params)
        .model("claude-3-5-sonnet-20240620")
        .userMessage("Explain what this chart shows, identify any trends or anomalies, " +
                    "and suggest insights based on the data and filters I've applied.")
        .maxTokens(1500)
        .build();

    // 3. Send to AI provider
    IAIProviderFactory factory = getAIProviderFactory();  // OSGi service
    IAIProvider provider = factory.getProvider(Env.getCtx(), providerId, null);
    AIResponse response = provider.generateText(request);

    // 4. Display AI insights in a dialog or panel
    showAIInsightsDialog(response.getContent());
}

/**
 * Get current client-side state from JavaScript
 */
private String getClientSideState() {
    // Call JavaScript to get state
    String js = "JSON.stringify({" +
                "  selectedDataPoints: window.myWidget.selectedPoints," +
                "  zoomLevel: window.myWidget.zoomLevel," +
                "  currentView: window.myWidget.currentView," +
                "  drillDownPath: window.myWidget.drillDownPath" +
                "})";

    return (String) Clients.evalJavaScript(js);
}

/**
 * Get rendering JavaScript
 */
private String getRenderingJavaScript() {
    // Return the actual JavaScript code used to render the chart
    return this.chartRenderingCode;
}
```

#### Key Benefits of This Approach

1. **Completely Generic**
   - Works with ANY custom interface
   - No hardcoded assumptions about widget types
   - Fully extensible through ContextParameters

2. **Captures Everything**
   - SQL queries (multiple sources)
   - JSON configurations
   - JavaScript code
   - User interactions
   - Client-side state
   - Related components

3. **Easy to Use**
   - Just create ContextParameters with your data
   - Call `withExtractedContext("CUSTOM_WIDGET", params)`
   - Context provider handles the rest

4. **Flexible Data Passing**
   - Strings, arrays, Maps, JSON objects
   - Auto-conversion and parsing
   - Context variable resolution

5. **Security Built-In**
   - Sensitive field redaction
   - User permission validation
   - Audit logging

#### Creating Your Own Context Provider

For specialized widget types, you can create dedicated providers:

```java
// Example: Sales Dashboard Widget Provider
public class SalesDashboardWidgetProvider implements IAIContextProvider {

    @Override
    public String getContextType() {
        return "SALES_DASHBOARD_WIDGET";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        // Your custom extraction logic here
        // Tailored specifically to sales dashboard widgets
        // Can include business logic, KPI calculations, etc.
    }
}

// Register it
AIContextProviderRegistry.getInstance().register(
    new SalesDashboardWidgetProvider()
);

// Use it
AIRequest request = new AIRequest.Builder()
    .withContext(ctx, windowNo)
    .withExtractedContext("SALES_DASHBOARD_WIDGET", params)
    .build();
```

---

## Conclusion

The com.cloudempiere.ai plugin has a solid foundation with the **external API approach already implemented** (AnthropicProvider). The **local LLM approach** from the druiz POC shows technical feasibility but requires **significant security hardening** before production use.

**Key Success Factors**:
1. ✅ **Security First**: Complete Phase 1 before any production DB access
2. ✅ **Context-Aware AI**: Implement Phase 6 for meaningful user interactions
3. ✅ **Hybrid Approach**: Leverage both external API and local LLM based on use case
4. ✅ **Incremental Rollout**: Start with low-risk use cases (content generation)
5. ✅ **Continuous Monitoring**: Track security, performance, and costs
6. ✅ **User Training**: Educate users on AI capabilities and limitations

**Recommended Approval Gates**:
- ✅ **Gate 1**: Security design review (before Phase 1 implementation)
- ✅ **Gate 2**: Context architecture review (before Phase 6 implementation)
- ✅ **Gate 3**: Security testing passed (before local LLM production use)
- ✅ **Gate 4**: Use case validation (before scaling beyond pilot)

**Risk Level by Approach**:
- External API only: **LOW** ✅ (recommended for initial rollout)
- External API with context: **LOW-MEDIUM** ⚠️ (ensure sensitive data redaction)
- Local LLM with security + context: **MEDIUM** ⚠️ (after Phase 1 & 6 completion)
- Local LLM without security: **CRITICAL** ❌ (NEVER deploy)

---

**Document Version**: 2.0
**Last Updated**: 2025-11-17
**Status**: Ready for Review
**Approvals Required**: Security Team, Architecture Team, Product Owner

**Changes in v2.0**:
- Added Phase 6: iDempiere Context Integration
- Comprehensive context extraction system for all iDempiere components
- Generic, extensible architecture using Strategy pattern
- Security considerations for context sharing with AI
- Example implementations for Chart and Window contexts
- Updated timeline to reflect new phase
