# ADR-048: Comprehensive Security Strategy for AI Chatbot Panel

## Status

Accepted

## Date

2025-12-19

## Deciders

Cloudempiere AI Team

## Related ADRs

- **ADR-007**: Database Security Model (Dual-Identity, implemented v0.10.0)
- **ADR-014**: Guardrails and Safety (InputGuard, OutputGuard, ExecutionGuard, implemented v0.12.0)
- **ADR-029**: Multi-Tenant Access Integration (AIGAccessTier, implemented v0.12.0)

---

## Context and Problem Statement

This ADR consolidates and extends the security requirements for the AI chatbot panel, building on the foundations established in ADR-007 (database security), ADR-014 (guardrails), and ADR-029 (multi-tenant access).

While those ADRs address specific security aspects, we need a comprehensive strategy covering:

1. **Prompt Injection Prevention** - Defense against LLM manipulation attacks
2. **Token Usage Optimization** - Cost control and rate limiting
3. **Caching Strategy** - Secure, tenant-aware response caching
4. **Audit & Monitoring** - Comprehensive interaction logging
5. **Compliance** - GDPR, SOX, HIPAA requirements

### Key Security Requirements

| Requirement | Related ADR | Status |
|-------------|-------------|--------|
| Multi-Tenancy (AD_Client_ID filtering) | ADR-007 | Implemented |
| Role-Based Access Control | ADR-007 | Implemented |
| Prompt Injection Detection | ADR-014 (InputGuard) | Implemented |
| Output Validation | ADR-014 (OutputGuard) | Implemented |
| Risk-Based Execution | ADR-014 (ExecutionGuard) | Implemented |
| Access Tiers | ADR-029 | Implemented |
| **Token Optimization** | This ADR | Planned |
| **Rate Limiting** | This ADR | Planned |
| **Response Caching** | This ADR | Planned |
| **Comprehensive Audit** | This ADR | Partial |

---

## Decision Drivers

- **Cost Control**: LLM API costs scale with token usage
- **Performance**: Caching reduces latency for common queries
- **Compliance**: Audit trails required for enterprise deployment
- **Abuse Prevention**: Rate limiting prevents cost overruns and DoS

---

## Decision

### 1. Prompt Injection Prevention (Enhancement to ADR-014)

**Current Implementation:** `InputGuard.java` with PII detection and injection patterns.

**Enhancement:** Add comprehensive injection patterns based on OWASP LLM Top 10.

```java
// Additional patterns for InputGuard.java
private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
    // Direct instruction override
    Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
    Pattern.compile("forget\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
    Pattern.compile("you\\s+are\\s+now\\s+in\\s+(developer|admin|debug)\\s+mode", Pattern.CASE_INSENSITIVE),

    // System prompt extraction
    Pattern.compile("reveal\\s+your\\s+(system\\s+)?prompt", Pattern.CASE_INSENSITIVE),
    Pattern.compile("what\\s+(are|were)\\s+your\\s+instructions", Pattern.CASE_INSENSITIVE),
    Pattern.compile("show\\s+me\\s+your\\s+prompt", Pattern.CASE_INSENSITIVE),

    // Role manipulation
    Pattern.compile("pretend\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
    Pattern.compile("act\\s+as\\s+if", Pattern.CASE_INSENSITIVE),

    // Database schema extraction
    Pattern.compile("(show|describe|list)\\s+(tables|columns|schema)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("information_schema", Pattern.CASE_INSENSITIVE),
    Pattern.compile("pg_catalog", Pattern.CASE_INSENSITIVE)
);

// Base64 encoded injection detection
public boolean containsEncodedInjection(String text) {
    Pattern base64Pattern = Pattern.compile("[A-Za-z0-9+/]{20,}={0,2}");
    Matcher matcher = base64Pattern.matcher(text);
    while (matcher.find()) {
        try {
            String decoded = new String(Base64.getDecoder().decode(matcher.group()));
            if (containsInjectionPattern(decoded)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Not valid base64, ignore
        }
    }
    return false;
}

// Zero-width character detection (used to hide instructions)
public boolean containsZeroWidthChars(String text) {
    return text.contains("\u200B") || text.contains("\u200C") ||
           text.contains("\u200D") || text.contains("\uFEFF");
}
```

### 2. Token Usage Optimization

**New Component:** `TokenOptimizer.java`

```java
public class TokenOptimizer {

    // Configuration (from AIG_Provider or system properties)
    private static final int MAX_INPUT_TOKENS = 4000;
    private static final int MAX_RETRIEVAL_CHUNKS = 5;
    private static final int MAX_HISTORY_MESSAGES = 20;

    /**
     * Optimize retrieval to stay within token budget.
     * Uses LangChain4j ContentRetriever with token-aware filtering.
     */
    public List<TextSegment> optimizeRetrieval(String query, int tokenBudget) {
        // Get more results than needed
        List<TextSegment> candidates = retriever.retrieve(query, MAX_RETRIEVAL_CHUNKS * 2);

        // Select top chunks that fit budget
        List<TextSegment> selected = new ArrayList<>();
        int currentTokens = 0;

        for (TextSegment segment : candidates) {
            int segmentTokens = countTokens(segment.text());
            if (currentTokens + segmentTokens <= tokenBudget * 0.7) { // 70% for context
                selected.add(segment);
                currentTokens += segmentTokens;
            }
            if (selected.size() >= MAX_RETRIEVAL_CHUNKS) break;
        }

        return selected;
    }

    /**
     * Manage conversation history to control token usage.
     * Already implemented in MessageWindowChatMemory(maxMessages=20).
     */
    public List<ChatMessage> trimHistory(List<ChatMessage> messages) {
        if (messages.size() <= MAX_HISTORY_MESSAGES + 1) { // +1 for system
            return messages;
        }

        // Keep system prompt + last N messages
        List<ChatMessage> result = new ArrayList<>();
        result.add(messages.get(0)); // System prompt
        result.addAll(messages.subList(messages.size() - MAX_HISTORY_MESSAGES, messages.size()));
        return result;
    }
}
```

### 3. Rate Limiting

**New Component:** `RateLimiter.java` (integrates with `CostGuard.java`)

```java
public class RateLimiter {

    // Limits per user
    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int REQUESTS_PER_HOUR = 100;
    private static final int REQUESTS_PER_DAY = 500;
    private static final int TOKENS_PER_DAY = 100_000;

    // Limits per tenant
    private static final int TENANT_REQUESTS_PER_HOUR = 1000;
    private static final int TENANT_REQUESTS_PER_DAY = 5000;
    private static final int TENANT_TOKENS_PER_DAY = 1_000_000;

    /**
     * Check if request is within rate limits.
     * Uses iDempiere's CCache for in-memory tracking.
     */
    public RateLimitResult checkLimit(int userId, int clientId) {
        // Per-user checks
        RateLimitCounter userCounter = getUserCounter(userId);

        if (userCounter.requestsPerMinute >= REQUESTS_PER_MINUTE) {
            return RateLimitResult.blocked("Rate limit: too many requests per minute");
        }
        if (userCounter.requestsPerHour >= REQUESTS_PER_HOUR) {
            return RateLimitResult.blocked("Rate limit: too many requests per hour");
        }
        if (userCounter.requestsPerDay >= REQUESTS_PER_DAY) {
            return RateLimitResult.blocked("Rate limit: daily request quota reached");
        }
        if (userCounter.tokensPerDay >= TOKENS_PER_DAY) {
            return RateLimitResult.blocked("Rate limit: daily token quota reached");
        }

        // Per-tenant checks
        RateLimitCounter tenantCounter = getTenantCounter(clientId);
        if (tenantCounter.requestsPerHour >= TENANT_REQUESTS_PER_HOUR) {
            return RateLimitResult.blocked("Tenant rate limit: too many requests");
        }

        return RateLimitResult.allowed();
    }

    /**
     * Record token usage after request completes.
     * Persists to AIG_UsageMetrics for billing.
     */
    public void recordUsage(int userId, int clientId, int inputTokens, int outputTokens) {
        // Update in-memory counters
        getUserCounter(userId).addTokens(inputTokens + outputTokens);
        getTenantCounter(clientId).addTokens(inputTokens + outputTokens);

        // Persist to database (async)
        MAIUsageMetrics.record(userId, clientId, inputTokens, outputTokens);
    }
}
```

### 4. Response Caching

**New Component:** `ResponseCacheManager.java`

```java
public class ResponseCacheManager {

    // Cache TTL in seconds
    private static final int CACHE_TTL = 3600; // 1 hour

    // Cache using iDempiere's CCache with tenant-aware keys
    private static CCache<String, CachedResponse> cache =
        new CCache<>("AIResponseCache", 1000, CACHE_TTL / 60);

    /**
     * Generate cache key including security context.
     * Prevents cross-tenant cache poisoning.
     */
    private String generateCacheKey(String query, int clientId, int roleId) {
        String keyComponents = query.toLowerCase().trim() + "|" + clientId + "|" + roleId;
        return DigestUtils.sha256Hex(keyComponents);
    }

    /**
     * Get cached response if available.
     */
    public Optional<String> getCached(String query, int clientId, int roleId) {
        String key = generateCacheKey(query, clientId, roleId);
        CachedResponse cached = cache.get(key);

        if (cached != null && !cached.isExpired()) {
            // Track cache hit for metrics
            MAIUsageMetrics.recordCacheHit(clientId);
            return Optional.of(cached.getResponse());
        }
        return Optional.empty();
    }

    /**
     * Cache a response.
     */
    public void cache(String query, String response, int clientId, int roleId) {
        String key = generateCacheKey(query, clientId, roleId);
        cache.put(key, new CachedResponse(response, System.currentTimeMillis()));
    }

    /**
     * Invalidate cache when data changes.
     * Called from model change triggers.
     */
    public void invalidateForClient(int clientId) {
        // CCache doesn't support pattern invalidation
        // Use a version counter approach instead
        incrementCacheVersion(clientId);
    }
}
```

### 5. Comprehensive Audit Logging

**Enhancement to existing AIG_UsageMetrics:**

```java
public class AIAuditLogger {

    /**
     * Log complete interaction for audit trail.
     */
    public void logInteraction(AIInteraction interaction) {
        // Create audit record
        MAIUsageMetrics metrics = new MAIUsageMetrics(Env.getCtx(), 0, null);

        metrics.setAD_User_ID(interaction.getUserId());
        metrics.setAD_Client_ID(interaction.getClientId());
        metrics.setAD_Role_ID(interaction.getRoleId());
        metrics.setSessionID(interaction.getSessionId());
        metrics.setQuery(sanitizeForLog(interaction.getQuery()));
        metrics.setResponse(sanitizeForLog(interaction.getResponse()));
        metrics.setInputTokens(interaction.getInputTokens());
        metrics.setOutputTokens(interaction.getOutputTokens());
        metrics.setLatencyMS(interaction.getLatencyMs());
        metrics.setCacheHit(interaction.isCacheHit() ? "Y" : "N");
        metrics.setTablesAccessed(toJson(interaction.getTablesAccessed()));

        // Calculate cost
        BigDecimal cost = calculateCost(interaction.getInputTokens(),
                                        interaction.getOutputTokens(),
                                        interaction.getModelName());
        metrics.setCostUSD(cost);

        metrics.saveEx();
    }

    /**
     * Log security violation with alerting.
     */
    public void logSecurityViolation(SecurityViolation violation) {
        // Store in security violations table
        X_AIG_SecurityViolation record = new X_AIG_SecurityViolation(Env.getCtx(), 0, null);
        record.setAD_User_ID(violation.getUserId());
        record.setAD_Client_ID(violation.getClientId());
        record.setViolationType(violation.getType().name());
        record.setSeverity(violation.getSeverity().name());
        record.setBlockedQuery(sanitizeForLog(violation.getQuery()));
        record.setDetails(violation.getDetails());
        record.saveEx();

        // Send alert for HIGH/CRITICAL severity
        if (violation.getSeverity().ordinal() >= Severity.HIGH.ordinal()) {
            sendSecurityAlert(violation);
        }
    }

    private String sanitizeForLog(String text) {
        if (text == null) return null;
        // Redact passwords
        text = text.replaceAll("(?i)password[\"\\s:=]+[^\\s,}\"]+", "password=REDACTED");
        // Redact tokens
        text = text.replaceAll("(?i)token[\"\\s:=]+[^\\s,}\"]+", "token=REDACTED");
        // Truncate if too long
        if (text.length() > 5000) {
            text = text.substring(0, 5000) + "... [TRUNCATED]";
        }
        return text;
    }
}
```

---

## Configuration

Add to `AIG_Provider` or system configuration:

```properties
# Token Optimization
ai.token.max_input=4000
ai.token.max_retrieval_chunks=5
ai.token.max_history_messages=20

# Rate Limiting - Per User
ai.ratelimit.user.requests_per_minute=10
ai.ratelimit.user.requests_per_hour=100
ai.ratelimit.user.requests_per_day=500
ai.ratelimit.user.tokens_per_day=100000

# Rate Limiting - Per Tenant
ai.ratelimit.tenant.requests_per_hour=1000
ai.ratelimit.tenant.requests_per_day=5000
ai.ratelimit.tenant.tokens_per_day=1000000

# Caching
ai.cache.enabled=true
ai.cache.ttl_seconds=3600

# Alerting
ai.alert.injection_attempts_per_hour=5
ai.alert.daily_cost_threshold_usd=100
ai.alert.error_rate_threshold_percent=5
```

---

## Database Schema

### New Table: AIG_SecurityViolation

```sql
-- Security violations table (migration script)
CREATE TABLE AIG_SecurityViolation (
    AIG_SecurityViolation_ID NUMERIC(10) NOT NULL,
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    AD_User_ID NUMERIC(10) NOT NULL,
    ViolationType VARCHAR(100) NOT NULL,
    Severity VARCHAR(20) NOT NULL,
    BlockedQuery TEXT,
    Details TEXT,
    IPAddress VARCHAR(45),
    UserAgent VARCHAR(500),
    Created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CreatedBy NUMERIC(10) NOT NULL,
    Updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedBy NUMERIC(10) NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y',
    CONSTRAINT AIG_SecurityViolation_Key PRIMARY KEY (AIG_SecurityViolation_ID)
);

CREATE INDEX idx_aig_secviol_client ON AIG_SecurityViolation(AD_Client_ID, Created);
CREATE INDEX idx_aig_secviol_user ON AIG_SecurityViolation(AD_User_ID, Created);
CREATE INDEX idx_aig_secviol_type ON AIG_SecurityViolation(ViolationType, Severity);
```

### Enhancement to AIG_UsageMetrics

```sql
-- Add columns for comprehensive audit
ALTER TABLE AIG_UsageMetrics ADD COLUMN SessionID VARCHAR(255);
ALTER TABLE AIG_UsageMetrics ADD COLUMN Query TEXT;
ALTER TABLE AIG_UsageMetrics ADD COLUMN Response TEXT;
ALTER TABLE AIG_UsageMetrics ADD COLUMN TablesAccessed TEXT; -- JSON array
ALTER TABLE AIG_UsageMetrics ADD COLUMN CacheHit CHAR(1) DEFAULT 'N';
ALTER TABLE AIG_UsageMetrics ADD COLUMN AD_Role_ID NUMERIC(10);
```

---

## Implementation Phases

### Phase 1: Enhanced Injection Prevention (v0.32.0)
- [ ] Add comprehensive injection patterns to InputGuard
- [ ] Add base64 decoding detection
- [ ] Add zero-width character detection
- [ ] Unit tests for new patterns

### Phase 2: Token Optimization (v0.32.0)
- [ ] Implement TokenOptimizer class
- [ ] Integrate with AIService
- [ ] Add configuration properties

### Phase 3: Rate Limiting (v0.33.0)
- [ ] Implement RateLimiter class
- [ ] Integrate with CostGuard
- [ ] Add per-user and per-tenant limits
- [ ] Migration script for configuration

### Phase 4: Response Caching (v0.33.0)
- [ ] Implement ResponseCacheManager
- [ ] Integrate with AIService
- [ ] Add cache invalidation hooks

### Phase 5: Comprehensive Audit (v0.34.0)
- [ ] Create AIG_SecurityViolation table
- [ ] Enhance AIG_UsageMetrics
- [ ] Implement AIAuditLogger
- [ ] Add alerting for security violations

---

## Consequences

### Positive
- Comprehensive defense-in-depth security model
- Cost control through rate limiting and caching
- Complete audit trail for compliance
- Proactive security violation alerting

### Negative
- Additional latency from security checks (~100-200ms)
- Memory usage for caching
- Implementation complexity

### Risks
- Sophisticated prompt injection may still succeed (inherent LLM limitation)
- False positives in injection detection (tune patterns)
- Cache invalidation complexity

---

## Compliance

### GDPR
- All interactions logged with consent
- Users can request chat history deletion
- Cross-tenant isolation prevents data leakage

### SOX
- Complete audit trail of AI actions
- Role-based access enforcement
- Security violation tracking

### Data Retention
- Chat history: 90 days (configurable)
- Audit logs: 7 years
- Security violations: Indefinite

---

## References

- [OWASP LLM Top 10](https://owasp.org/www-project-top-10-for-large-language-model-applications/)
- [ADR-007: Database Security Model](007-database-security-model.md)
- [ADR-014: Guardrails and Safety](014-guardrails-and-safety.md)
- [ADR-029: Multi-Tenant Access Integration](docs/adr/)
- [LangChain Multi-Tenant RAG Best Practices](https://python.langchain.com/docs/how_to/qa_per_user/)
