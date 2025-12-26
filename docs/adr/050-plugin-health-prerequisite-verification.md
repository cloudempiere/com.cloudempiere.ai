# ADR-050: Plugin Health and Prerequisite Verification

## Status

Proposed

## Date

2025-12-25

## Deciders

- Cloudempiere AI Team
- iDempiere Plugin Architecture Team

## Scope

**Hybrid Approach**: Implemented in `com.cloudempiere.ai` but designed with reusable interfaces that can be extracted to a shared library (`com.cloudempiere.plugin.health`) when other plugins need similar functionality.

## Context and Problem Statement

The AI plugin (`com.cloudempiere.ai`) depends on multiple prerequisites to function correctly:
- Database tables (AIG_* tables) that may not be deployed
- PostgreSQL extensions (pgvector) for vector embeddings
- External LLM services (Anthropic, Ollama, AWS Bedrock)
- OSGi services from other bundles
- Valid configuration (API keys, provider settings)

Currently, if prerequisites are missing, the plugin may:
1. Fail silently with cryptic errors deep in the call stack
2. Crash on startup, preventing iDempiere from loading
3. Work partially, causing confusion about what features are available

We need a systematic approach to:
- Check all prerequisites before enabling AI features
- Provide clear feedback to administrators about missing prerequisites
- Allow graceful degradation when possible
- Support different modes for development vs production environments

## Decision Drivers

- **Reliability**: Plugin should not crash iDempiere if prerequisites are missing
- **Visibility**: Administrators should clearly see what's missing and how to fix it
- **Developer Experience**: Development environments should work with minimal setup
- **Production Safety**: Production environments should fail fast on critical issues
- **Extensibility**: Easy to add new prerequisite checks as features grow
- **Performance**: Health checks should not slow down normal operations
- **Monitoring**: Health status should be queryable for external monitoring systems

## Considered Options

1. **Fail-Fast on Bundle Start** - Check prerequisites in Activator.start()
2. **Lazy Initialization with Graceful Degradation** - Check on first use, fall back when possible
3. **Centralized Health Service with Tiered Checks** - Dedicated service with categorized prerequisites
4. **Feature Flags with Runtime Detection** - Individual feature toggles based on prerequisites

## Decision Outcome

**Chosen option:** "Centralized Health Service with Tiered Checks" (Option 3), because it provides:
- Clear separation of concerns (health checking vs business logic)
- Tiered prerequisite categories (critical, required, optional, external)
- Configurable behavior (STRICT vs GRACEFUL mode)
- Admin notification via AD_Issue
- Monitoring endpoint for external systems
- Lazy initialization to handle startup ordering issues

### Confirmation

The implementation is confirmed successful when:
1. Plugin starts without errors even when AIG_* tables are missing
2. AD_Issue is created with clear resolution steps when prerequisites fail
3. `AIPluginHealthService.getInstance().isHealthy()` returns accurate status
4. Health endpoint returns JSON status for monitoring systems
5. STRICT mode throws exception when critical prerequisites missing
6. GRACEFUL mode logs warnings and continues with degraded functionality

## Pros and Cons of the Options

### Option 1: Fail-Fast on Bundle Start

Check all prerequisites in `Activator.start()` and throw exception if missing.

- Good, because issues are detected immediately on server start
- Good, because simple implementation
- Bad, because OSGi bundle activation order is not guaranteed
- Bad, because database may not be ready during bundle activation
- Bad, because one missing prerequisite blocks entire plugin
- Bad, because no graceful degradation possible

### Option 2: Lazy Initialization with Graceful Degradation

Each service checks its own prerequisites on first use.

- Good, because handles OSGi startup ordering naturally
- Good, because allows partial functionality
- Neutral, because each service manages its own health
- Bad, because no centralized view of plugin health
- Bad, because duplicate prerequisite checking code
- Bad, because difficult to query overall health status

### Option 3: Centralized Health Service with Tiered Checks

Dedicated `AIPluginHealthService` that categorizes and checks all prerequisites.

- Good, because single source of truth for plugin health
- Good, because tiered categories allow nuanced responses
- Good, because supports both STRICT and GRACEFUL modes
- Good, because provides monitoring endpoint
- Good, because lazy initialization handles startup ordering
- Good, because extensible for new prerequisites
- Neutral, because adds another service to manage
- Bad, because requires coordination between services

### Option 4: Feature Flags with Runtime Detection

Individual feature toggles that activate based on prerequisite availability.

- Good, because fine-grained control per feature
- Good, because UI can show which features are available
- Bad, because complex configuration management
- Bad, because no clear "plugin is healthy" status
- Bad, because scattered prerequisite logic

## More Information

### Prerequisite Categories

The health service checks prerequisites in four tiers:

| Tier | Category | Failure Behavior | Examples |
|------|----------|------------------|----------|
| 1 | **Critical** | Always fail (both modes) | Database connection |
| 2 | **Required** | Fail in STRICT, warn in GRACEFUL | AIG_Provider table, core tables |
| 3 | **Optional** | Always warn, feature disabled | pgvector, aig_embedding table |
| 4 | **External** | Always warn, provider unavailable | Ollama server, Anthropic API |

### Prerequisite Checklist

#### Tier 1: Critical (Plugin Cannot Function)

| Prerequisite | Check Method | Resolution |
|--------------|--------------|------------|
| Database Connection | `DB.getConnectionRO()` | Fix database configuration |
| iDempiere Context | `Env.getCtx() != null` | Wait for iDempiere initialization |

#### Tier 2: Required (Core Functionality)

| Prerequisite | Check Method | Resolution |
|--------------|--------------|------------|
| `aig_provider` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| `aig_budget` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| `aig_usagemetrics` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| `aig_chatownership` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| `aig_queryaudit` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| `aig_prompt_config` table | `information_schema.tables` | Deploy 2Pack or run migrations |
| Active AIG_Provider | Query AIG_Provider | Configure at least one provider |

#### Tier 3: Optional (Enhanced Features)

| Prerequisite | Check Method | Affected Feature |
|--------------|--------------|------------------|
| pgvector extension | `pg_extension` | Vector embeddings, RAG |
| `aig_embedding` table | `information_schema.tables` | Persistent embeddings |
| `aig_knowledge_entry` table | `information_schema.tables` | Knowledge base |

#### Tier 4: External Services

| Prerequisite | Check Method | Affected Feature |
|--------------|--------------|------------------|
| Anthropic API | HTTP health check | Claude models |
| Ollama Server | HTTP health check to `/api/tags` | Local LLM, embeddings |
| AWS Bedrock | STS GetCallerIdentity | Bedrock models |
| OpenAI API | HTTP health check | OpenAI/GPT models |

### Initialization Modes

```
┌─────────────────────────────────────────────────────────────────┐
│                    Initialization Mode                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  GRACEFUL (Default - Development/Testing)                      │
│  ─────────────────────────────────────────                      │
│  • Log WARNING for missing prerequisites                        │
│  • Create AD_Issue with resolution steps                        │
│  • Continue with degraded functionality                         │
│  • Use fallbacks where possible (in-memory store)               │
│                                                                 │
│  STRICT (Production)                                            │
│  ────────────────────                                           │
│  • Log SEVERE for Tier 1-2 failures                             │
│  • Create AD_Issue with resolution steps                        │
│  • Throw IllegalStateException for critical failures            │
│  • Tier 3-4 still degrade gracefully                            │
│                                                                 │
│  Configuration:                                                 │
│  • System property: -Dai.plugin.mode=strict                     │
│  • Or programmatic: healthService.setInitMode(STRICT)           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Health Status Data Model

```java
public class PluginHealthStatus {
    // Overall status
    private HealthState overallState;  // HEALTHY, DEGRADED, UNHEALTHY
    private long lastCheckTimestamp;

    // Tier 1: Critical
    private boolean databaseAvailable;
    private boolean contextInitialized;

    // Tier 2: Required
    private List<TableStatus> requiredTables;
    private boolean hasActiveProvider;

    // Tier 3: Optional
    private boolean pgVectorAvailable;
    private List<TableStatus> optionalTables;

    // Tier 4: External
    private Map<String, ExternalServiceStatus> externalServices;

    // Metadata
    private String summary;
    private String resolutionSteps;
    private InitMode currentMode;
}

public enum HealthState {
    HEALTHY,    // All Tier 1-2 prerequisites met
    DEGRADED,   // Tier 1-2 met, some Tier 3-4 missing
    UNHEALTHY   // Tier 1 or 2 prerequisites missing
}
```

### Lazy Initialization Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                    Initialization Flow                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OSGi Bundle Start                                              │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────────┐                                        │
│  │ Activator.start()   │  ← Minimal work only                   │
│  │ - Register singleton│                                        │
│  │ - Read config       │                                        │
│  │ - NO DB access      │                                        │
│  └─────────────────────┘                                        │
│       │                                                         │
│       ▼                                                         │
│  Service Registered (but not initialized)                       │
│       │                                                         │
│       ▼                                                         │
│  First Usage (e.g., user opens AI Chat)                         │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────────┐                                        │
│  │ ensureInitialized() │  ← Thread-safe, one-time               │
│  │ - Check Tier 1      │                                        │
│  │ - Check Tier 2      │                                        │
│  │ - Check Tier 3      │                                        │
│  │ - Check Tier 4      │                                        │
│  │ - Create AD_Issue   │                                        │
│  └─────────────────────┘                                        │
│       │                                                         │
│       ▼                                                         │
│  Status Cached (refresh on demand)                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### AD_Issue Notification Format

When prerequisites fail, an AD_Issue is created with:

```
Summary: AI Plugin: Prerequisites Not Met

Description:
The AI Plugin cannot start properly due to missing prerequisites.

Mode: GRACEFUL
Status: UNHEALTHY

═══════════════════════════════════════════════════════════════
TIER 1 - CRITICAL
═══════════════════════════════════════════════════════════════
✓ Database Connection: AVAILABLE
✓ iDempiere Context: INITIALIZED

═══════════════════════════════════════════════════════════════
TIER 2 - REQUIRED (Core Functionality)
═══════════════════════════════════════════════════════════════
✗ aig_provider: NOT FOUND
✗ aig_budget: NOT FOUND
✗ aig_usagemetrics: NOT FOUND
✗ aig_chatownership: NOT FOUND
✗ aig_queryaudit: NOT FOUND
✗ aig_prompt_config: NOT FOUND

═══════════════════════════════════════════════════════════════
TIER 3 - OPTIONAL (Enhanced Features)
═══════════════════════════════════════════════════════════════
✗ pgvector extension: NOT INSTALLED (RAG disabled)
✗ aig_embedding table: NOT FOUND (persistent embeddings disabled)

═══════════════════════════════════════════════════════════════
TIER 4 - EXTERNAL SERVICES
═══════════════════════════════════════════════════════════════
⚠ Cannot check - no providers configured

═══════════════════════════════════════════════════════════════
RESOLUTION STEPS
═══════════════════════════════════════════════════════════════

Option 1: Deploy via 2Pack (Recommended)
----------------------------------------
1. Navigate to: System Admin → Pack In
2. Import: com.cloudempiere.ai-2pack.zip
3. Restart iDempiere

Option 2: Run Migration Scripts
-------------------------------
1. Execute SQL migrations from:
   com.cloudempiere.ai/migration/postgresql/

2. For pgvector support (optional):
   CREATE EXTENSION IF NOT EXISTS vector;

3. Restart iDempiere or call:
   AIPluginHealthService.getInstance().refresh()

═══════════════════════════════════════════════════════════════
```

### Monitoring Endpoint

The health service provides a JSON endpoint for external monitoring:

```java
// Get health status as JSON (for REST API or monitoring)
Map<String, Object> status = AIPluginHealthService.getInstance().getHealthAsJson();
```

Returns:
```json
{
  "status": "DEGRADED",
  "timestamp": "2025-12-25T10:30:00Z",
  "mode": "GRACEFUL",
  "tiers": {
    "critical": {
      "status": "HEALTHY",
      "checks": {
        "database": true,
        "context": true
      }
    },
    "required": {
      "status": "UNHEALTHY",
      "checks": {
        "aig_provider": false,
        "aig_budget": false,
        "hasActiveProvider": false
      },
      "missing": ["aig_provider", "aig_budget", "aig_usagemetrics"]
    },
    "optional": {
      "status": "DEGRADED",
      "checks": {
        "pgvector": false,
        "aig_embedding": false
      },
      "disabledFeatures": ["RAG", "persistent_embeddings"]
    },
    "external": {
      "status": "UNKNOWN",
      "services": {}
    }
  },
  "summary": "Core tables missing - deploy 2Pack",
  "resolutionUrl": "/webui/ai-plugin-setup"
}
```

### Usage Pattern

```java
// Service classes should check health before operations
public class AIChatService {

    public AIResponse processChat(AIRequest request) {
        // Quick health check
        AIPluginHealthService health = AIPluginHealthService.getInstance();

        if (health == null) {
            throw new AIPluginNotReadyException(
                "AI Plugin service not initialized. Please try again later.");
        }

        PluginHealthStatus status = health.getStatus();

        // Check based on required features
        if (!status.isHealthy()) {
            if (status.getOverallState() == HealthState.UNHEALTHY) {
                throw new AIPluginNotReadyException(
                    "AI Plugin prerequisites not met: " + status.getSummary());
            }
            // DEGRADED - some features may not work
            log.warning("AI Plugin running in degraded mode: " + status.getSummary());
        }

        // Check specific feature requirements
        if (request.requiresRag() && !status.isRagAvailable()) {
            throw new FeatureNotAvailableException(
                "RAG not available: " + status.getRagStatus());
        }

        // Proceed with operation
        return doProcessChat(request);
    }
}
```

### Reusable Interface Design (Hybrid Approach)

The implementation uses generic interfaces that can be extracted to a shared library later:

```java
// ═══════════════════════════════════════════════════════════════
// REUSABLE INTERFACES (can be moved to com.cloudempiere.plugin.health)
// ═══════════════════════════════════════════════════════════════

/**
 * Generic prerequisite check interface.
 * Implement this for each type of prerequisite.
 */
public interface IPrerequisiteCheck {
    /** Unique identifier for this check */
    String getId();

    /** Human-readable name */
    String getName();

    /** Which tier this check belongs to */
    PrerequisiteTier getTier();

    /** Perform the check */
    CheckResult check();

    /** Get resolution steps if check fails */
    String getResolutionSteps();
}

/**
 * Result of a prerequisite check.
 */
public class CheckResult {
    private final boolean passed;
    private final String message;
    private final Exception exception;  // Optional

    public static CheckResult pass() { ... }
    public static CheckResult pass(String message) { ... }
    public static CheckResult fail(String message) { ... }
    public static CheckResult fail(String message, Exception e) { ... }
}

/**
 * Prerequisite tiers for categorized handling.
 */
public enum PrerequisiteTier {
    CRITICAL(1, "Critical - plugin cannot function"),
    REQUIRED(2, "Required - core functionality"),
    OPTIONAL(3, "Optional - enhanced features"),
    EXTERNAL(4, "External - third-party services");

    private final int level;
    private final String description;
}

/**
 * Plugin health service interface.
 * Each plugin implements this with its specific checks.
 */
public interface IPluginHealthService {
    /** Plugin identifier */
    String getPluginId();

    /** Current health state */
    HealthState getHealthState();

    /** Detailed status */
    PluginHealthStatus getStatus();

    /** Force refresh of status */
    PluginHealthStatus refresh();

    /** Get all registered checks */
    List<IPrerequisiteCheck> getChecks();

    /** Register a new check */
    void registerCheck(IPrerequisiteCheck check);

    /** Get health as JSON for monitoring */
    Map<String, Object> getHealthAsJson();
}

// ═══════════════════════════════════════════════════════════════
// AI PLUGIN SPECIFIC IMPLEMENTATIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Check if a database table exists.
 */
public class TableExistsCheck implements IPrerequisiteCheck {
    private final String tableName;
    private final PrerequisiteTier tier;

    public TableExistsCheck(String tableName, PrerequisiteTier tier) {
        this.tableName = tableName;
        this.tier = tier;
    }

    @Override
    public CheckResult check() {
        String sql = "SELECT 1 FROM information_schema.tables WHERE LOWER(table_name) = LOWER(?)";
        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return CheckResult.pass();
                }
                return CheckResult.fail("Table '" + tableName + "' not found");
            }
        } catch (SQLException e) {
            return CheckResult.fail("Cannot check table: " + e.getMessage(), e);
        }
    }

    @Override
    public String getResolutionSteps() {
        return "Deploy 2Pack or run migration script to create table: " + tableName;
    }
}

/**
 * Check if a PostgreSQL extension is installed.
 */
public class PostgresExtensionCheck implements IPrerequisiteCheck {
    private final String extensionName;

    @Override
    public CheckResult check() {
        String sql = "SELECT 1 FROM pg_extension WHERE extname = ?";
        // ... implementation
    }
}

/**
 * Check if an external HTTP service is reachable.
 */
public class HttpServiceCheck implements IPrerequisiteCheck {
    private final String serviceName;
    private final String healthUrl;
    private final int timeoutMs;

    @Override
    public CheckResult check() {
        // Async HTTP check with timeout
    }
}

/**
 * Check if an OSGi service is available.
 */
public class OsgiServiceCheck implements IPrerequisiteCheck {
    private final Class<?> serviceInterface;
    private final BundleContext bundleContext;

    @Override
    public CheckResult check() {
        ServiceReference<?> ref = bundleContext.getServiceReference(serviceInterface);
        if (ref != null) {
            return CheckResult.pass();
        }
        return CheckResult.fail("OSGi service not available: " + serviceInterface.getName());
    }
}
```

#### Future Extraction Path

When other plugins need health checks:

```
Phase 1 (Current):
  com.cloudempiere.ai
    └── health/
        ├── IPrerequisiteCheck.java      ← Generic interface
        ├── IPluginHealthService.java    ← Generic interface
        ├── CheckResult.java             ← Generic
        ├── PrerequisiteTier.java        ← Generic
        ├── PluginHealthStatus.java      ← Generic
        ├── TableExistsCheck.java        ← Reusable
        ├── PostgresExtensionCheck.java  ← Reusable
        ├── HttpServiceCheck.java        ← Reusable
        └── AIPluginHealthService.java   ← AI-specific

Phase 2 (Extraction):
  com.cloudempiere.plugin.health (NEW shared bundle)
    └── health/
        ├── IPrerequisiteCheck.java      ← Extracted
        ├── IPluginHealthService.java    ← Extracted
        ├── CheckResult.java             ← Extracted
        ├── PrerequisiteTier.java        ← Extracted
        ├── PluginHealthStatus.java      ← Extracted
        ├── checks/
        │   ├── TableExistsCheck.java    ← Extracted
        │   ├── PostgresExtensionCheck.java
        │   ├── HttpServiceCheck.java
        │   └── OsgiServiceCheck.java
        └── PluginHealthRegistry.java    ← NEW: aggregates all plugins

  com.cloudempiere.ai (depends on shared bundle)
    └── health/
        └── AIPluginHealthService.java   ← Uses shared interfaces

  com.cloudempiere.dms (depends on shared bundle)
    └── health/
        └── DMSPluginHealthService.java  ← New plugin uses same pattern
```

### Implementation Notes

#### Service Component Declaration

```xml
<!-- OSGI-INF/com.cloudempiere.ai.health.AIPluginHealthService.xml -->
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
               activate="activate"
               deactivate="deactivate"
               immediate="true"
               name="com.cloudempiere.ai.health.AIPluginHealthService">
   <property name="service.ranking" type="Integer" value="100"/>
   <service>
      <provide interface="com.cloudempiere.ai.health.IAIPluginHealthService"/>
   </service>
   <implementation class="com.cloudempiere.ai.health.AIPluginHealthService"/>
</scr:component>
```

#### External Service Health Checks

External services are checked asynchronously to avoid blocking:

```java
// Check Ollama availability
private CompletableFuture<ExternalServiceStatus> checkOllamaAsync(String baseUrl) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            URL url = new URL(baseUrl + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            return new ExternalServiceStatus("ollama", responseCode == 200);
        } catch (Exception e) {
            return new ExternalServiceStatus("ollama", false, e.getMessage());
        }
    });
}
```

### Integration with iDempiere Core Patterns

**Important**: iDempiere does NOT have a built-in plugin health check framework. This ADR introduces a new capability that integrates with existing iDempiere patterns.

#### Existing iDempiere Patterns

| Pattern | Class/Interface | Purpose |
|---------|-----------------|---------|
| Server state events | `ServerStateChangeEvent` | `SERVER_START`, `SERVER_STOP` events |
| Server ready check | `Adempiere.isStarted()` | Returns true when database is ready |
| 2Pack deployment | `Incremental2PackActivator` | Waits for server start, deploys tables |
| Framework started | `AbstractActivator.frameworkStarted()` | Hook called when OSGi framework ready |

#### How AI Plugin Health Integrates

```
┌─────────────────────────────────────────────────────────────────┐
│                iDempiere Startup Sequence                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. OSGi Framework Starts                                       │
│       │                                                         │
│       ▼                                                         │
│  2. Bundle Activation (Activator.start())                       │
│       │ ← Don't access database here!                           │
│       │                                                         │
│       ▼                                                         │
│  3. FrameworkEvent.STARTLEVEL_CHANGED                           │
│       │ ← AbstractActivator.frameworkStarted()                  │
│       │                                                         │
│       ▼                                                         │
│  4. Wait for Adempiere.isStarted() or ServerStateChangeEvent    │
│       │                                                         │
│       ▼                                                         │
│  5. Incremental2PackActivator.installPackage()                  │
│       │ ← 2Pack creates AIG_* tables                            │
│       │                                                         │
│       ▼                                                         │
│  6. AIPluginHealthService.ensureInitialized()                   │
│       │ ← NEW: Check prerequisites on first AI feature use      │
│       │                                                         │
│       ▼                                                         │
│  7. AI Features Available                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Key Integration Points

```java
// AI plugin follows iDempiere pattern: wait for server ready
public class AIPluginHealthService {

    private void ensureInitialized() {
        // Follow iDempiere pattern: check server is started
        if (!Adempiere.isStarted()) {
            // Database not ready - allow retry later
            log.fine("Adempiere not started - deferring health check");
            return;
        }

        synchronized (initLock) {
            if (!initialized.get()) {
                checkPrerequisites();
                initialized.set(true);
            }
        }
    }
}

// Integration with Activator - NO DB access during start()
public class Activator extends Incremental2PackActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // Standard model/process scanning - safe
        Core.getMappedModelFactory().scan(context, "com.cloudempiere.ai.model");
        Core.getMappedProcessFactory().scan(context, "com.cloudempiere.ai.process");

        // 2Pack installation - waits for Adempiere.isStarted()
        super.start(context);

        // Health service registered but NOT initialized yet
        // Will initialize on first use (lazy)
    }
}
```

### Related ADRs

- [ADR-001](001-initial-architecture.md) - Initial architecture defining plugin structure
- [ADR-006](006-data-model-architecture.md) - Data model defining AIG_* tables
- [ADR-026](026-vector-database-strategy.md) - pgvector strategy for embeddings
- [ADR-038](038-user-friendly-error-handling.md) - Error handling patterns
- [ADR-042](042-satellite-ai-provider-integration.md) - AI Hub provider integration
- [ADR-051](051-zk-ui-defensive-programming.md) - UI resilience using this health service

### References

- [OSGi Declarative Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html)
- [iDempiere AD_Issue](https://wiki.idempiere.org/en/AD_Issue_(Table))
- [Health Check Patterns](https://microservices.io/patterns/observability/health-check-api.html)
- [Kubernetes Liveness/Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)

---

## Appendix: Migration Path

### Phase 1: Core Health Service (This ADR)
- Implement `AIPluginHealthService` with Tier 1-2 checks
- AD_Issue notification
- STRICT/GRACEFUL modes

### Phase 2: Optional Feature Checks
- Add Tier 3 checks (pgvector, optional tables)
- Feature availability API

### Phase 3: External Service Monitoring
- Add Tier 4 async checks
- Provider connectivity status
- Cache results with TTL

### Phase 4: Monitoring Integration
- REST endpoint for health status
- Kubernetes probe support
- Prometheus metrics export
