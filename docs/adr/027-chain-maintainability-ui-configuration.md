# ADR-027: Chain Maintainability and UI Configuration Strategy

## Status

Proposed

## Date

2025-12-03

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

As the `com.cloudempiere.ai` plugin evolves with multiple AI chains (agents, tools, prompts), we need a sustainable strategy for:

1. **Maintainability**: How to structure chains for easy updates without breaking existing functionality
2. **Update Frequency**: How to manage rapid AI model/prompt improvements vs iDempiere release cycles
3. **UI Configuration**: Whether business users can define/modify chains without code changes
4. **Deployment Independence**: Leveraging OSGi plugin architecture to update AI capabilities without full iDempiere server updates

**Key Insight**: Since `com.cloudempiere.ai` is an OSGi plugin, it can be hot-deployed independently of the iDempiere core system. This creates an opportunity for faster iteration cycles on AI capabilities.

### Current State

```
iDempiere Core (iDempiereCLDE branch)
    └── com.cloudempiere.ai (OSGi Plugin)
            ├── LangChain4jProviderFactory (Provider abstraction)
            ├── ERPTools (@Tool annotated methods)
            ├── IDempiereAgent (AiServices interface)
            ├── MAIPromptConfig (Database-stored prompts)
            └── Various use-case specific logic
```

### Challenges

1. **Model Evolution**: Claude 3.5 → Claude 4 → Claude 5 requires prompt adjustments
2. **Prompt Tuning**: Business-specific prompts need iteration without developer involvement
3. **Tool Additions**: New ERP operations require code changes
4. **Chain Composition**: Complex workflows need flexible orchestration
5. **Version Control**: Track what worked vs what broke

## Decision Drivers

- **Rapid Iteration**: AI capabilities evolve faster than traditional ERP release cycles
- **Business Agility**: Non-developers need to tune AI behavior for their context
- **Stability**: Production systems need predictable, versioned behavior
- **Auditability**: Track all changes for compliance
- **OSGi Advantage**: Plugin can be updated independently (no full server restart)
- **Cost Control**: Prevent runaway API costs from poorly configured chains

## Considered Options

1. **Code-Only Chains** - All chain logic in Java, deployed via OSGi updates
2. **Database-Configured Chains** - Chain definitions stored in Application Dictionary
3. **Hybrid: Code Structure + Database Configuration** - Framework in code, parameters in DB
4. **External Chain Repository** - Chains loaded from external service/file system
5. **External Quarkus Microservice** - AI chains in separate Quarkus project, communicating via REST/gRPC

## Decision Outcome

**Chosen option:** "Hybrid: Code Structure + Database Configuration" (Option 3)

This approach provides:
- **Structural stability** via versioned Java code (OSGi plugin)
- **Runtime flexibility** via database-stored configurations
- **Business user empowerment** via iDempiere UI for parameter tuning
- **Version control** via both git (code) and AD changelog (config)

### Confirmation

The decision is confirmed when:
- [ ] Chain configurations can be modified in iDempiere UI without code deployment
- [ ] New chain versions can be A/B tested in production
- [ ] Audit trail captures all configuration changes
- [ ] Plugin hot-reload updates chain logic without server restart

## Pros and Cons of the Options

### Option 1: Code-Only Chains

All chain definitions, prompts, and orchestration logic in Java code.

```java
public class SalesOpportunityChain {
    private static final String SYSTEM_PROMPT = """
        You are a sales analyst for an ERP system...
        """;

    @Tool("Analyze opportunity health")
    public String analyzeOpportunity(...) { }
}
```

- Good, because fully version-controlled in git
- Good, because compile-time type safety
- Good, because easy to test with unit tests
- Bad, because requires developer for any change
- Bad, because OSGi plugin update for prompt tweaks
- Bad, because no business user self-service

### Option 2: Database-Configured Chains

All chain definitions stored in Application Dictionary tables.

```sql
-- AIG_Chain: Chain definition
-- AIG_Chain_Step: Ordered steps in chain
-- AIG_Chain_Tool: Tools available to chain
-- AIG_Prompt_Template: Parameterized prompts
```

- Good, because business users can modify via UI
- Good, because no code deployment for changes
- Good, because tenant-specific configurations
- Bad, because complex validation logic needed
- Bad, because risk of invalid configurations breaking production
- Bad, because difficult to test before deployment
- Bad, because prompt injection risks if not sanitized

### Option 3: Hybrid Approach (Recommended)

**Code provides structure and safety; database provides flexibility.**

```
┌─────────────────────────────────────────────────────────────┐
│                    Java Code (OSGi Plugin)                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Chain Framework                                      │    │
│  │  - ChainExecutor (orchestration logic)              │    │
│  │  - ChainValidator (safety checks)                   │    │
│  │  - ToolRegistry (available tools)                   │    │
│  │  - GuardrailEnforcer (limits)                       │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Chain Implementations (versioned)                    │    │
│  │  - SalesOpportunityChain v1.0                       │    │
│  │  - ChartAnalysisChain v1.0                          │    │
│  │  - TicketClassificationChain v1.0                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Database Configuration (AD Tables)            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ AIG_Chain_Config                                     │    │
│  │  - Chain name (reference to code)                   │    │
│  │  - Active version                                   │    │
│  │  - Provider override                                │    │
│  │  - Max tokens, temperature                          │    │
│  │  - Cost budget per execution                        │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ AIG_Prompt_Config (existing table)                   │    │
│  │  - System prompt template                           │    │
│  │  - User prompt template                             │    │
│  │  - Example few-shots                                │    │
│  │  - Version history                                  │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ AIG_Chain_Parameter                                  │    │
│  │  - Business-specific parameters                     │    │
│  │  - Tenant overrides                                 │    │
│  │  - Feature flags                                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

- Good, because structure is version-controlled and tested
- Good, because parameters tunable without deployment
- Good, because guardrails enforced in code (not bypassable)
- Good, because supports A/B testing via version flags
- Good, because audit trail for all configuration changes
- Neutral, because requires initial code framework investment
- Bad, because more complex than pure code approach

### Option 4: External Chain Repository

Chains loaded from external service (e.g., S3, GitHub).

- Good, because chains can be updated without any iDempiere change
- Bad, because external dependency for core functionality
- Bad, because network latency on chain load
- Bad, because security concerns (external code execution)
- Bad, because versioning complexity

### Option 5: External Quarkus Microservice (Alternative Worth Considering)

AI chains deployed as a separate Quarkus application, communicating with iDempiere via REST API.

```
┌─────────────────────────────────────────────────────────────┐
│              iDempiere (Existing)                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ com.cloudempiere.ai (Thin OSGi Plugin)              │    │
│  │  - REST client to Quarkus service                   │    │
│  │  - Context extraction (Window, Chart)               │    │
│  │  - Security enforcement (AD_User, AD_Role)          │    │
│  │  - UI components (AIChatWidget)                     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │ REST/gRPC
                              ▼
┌─────────────────────────────────────────────────────────────┐
│           cloudempiere-ai-service (Quarkus)                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Chain Implementations                                │    │
│  │  - SalesOpportunityChain                            │    │
│  │  - ChartAnalysisChain                               │    │
│  │  - TicketClassificationChain                        │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ LangChain4j Integration                              │    │
│  │  - Native Quarkus extension (quarkus-langchain4j)   │    │
│  │  - Dev Services for local LLMs                      │    │
│  │  - Built-in observability (Micrometer)              │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Deployment Options                                   │    │
│  │  - Docker container (easy updates)                  │    │
│  │  - Native executable (GraalVM)                      │    │
│  │  - AWS Lambda / ECS / Kubernetes                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- Good, because **completely independent deployment** - no OSGi complexity
- Good, because **modern tooling** - Quarkus DevMode for live reload during development
- Good, because **native LangChain4j support** via `quarkus-langchain4j` extension
- Good, because **container-native** - easy Docker/Kubernetes deployment
- Good, because **horizontal scaling** - multiple instances behind load balancer
- Good, because **faster startup** - Quarkus optimized for cloud-native
- Good, because **separate team ownership** - AI team can iterate independently
- Good, because **technology freedom** - not constrained by iDempiere's OSGi environment
- Good, because **dev services** - automatic local Ollama for development
- Good, because **better testing** - Quarkus test framework without iDempiere bootstrap

**Cons:**
- Bad, because **network latency** - REST call overhead for each AI operation
- Bad, because **distributed system complexity** - service discovery, health checks, retries
- Bad, because **security boundary** - must secure API between iDempiere and Quarkus
- Bad, because **data access complexity** - Quarkus cannot use iDempiere's `Env.getCtx()` or `MRole`
- Bad, because **two deployment targets** - must coordinate iDempiere + Quarkus versions
- Bad, because **infrastructure cost** - separate container/service to run and monitor
- Bad, because **context passing** - iDempiere context (AD_Client_ID, AD_Org_ID, AD_User_ID) must be serialized
- Bad, because **transaction boundaries** - cannot participate in iDempiere database transactions
- Bad, because **Application Dictionary access** - must replicate or query AD metadata

**When to Choose Quarkus Instead:**

| Scenario | Recommendation |
|----------|----------------|
| Heavy AI workload (>100 requests/min) | Consider Quarkus (horizontal scaling) |
| Separate AI team | Consider Quarkus (independent deployment) |
| Multiple ERP integrations | Consider Quarkus (shared AI service) |
| Tight iDempiere integration needed | Stay with OSGi plugin |
| Small team / simple use cases | Stay with OSGi plugin |
| Strong AD/security requirements | Stay with OSGi plugin |

**Recommended Migration Path: Start Internal, Scale with Quarkus**

The architecture is designed to **start with Option 3 (OSGi hybrid)** and **migrate to Quarkus when scaling demands**, reusing the same chain implementations:

```
Phase 1: Internal OSGi Plugin (Current)
┌─────────────────────────────────────────────────────────────┐
│                    iDempiere Server                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ com.cloudempiere.ai (Full Plugin)                    │    │
│  │  - Chain Framework + Implementations                │    │
│  │  - LangChain4j integration                          │    │
│  │  - Database configuration                           │    │
│  │  - Direct iDempiere API access                      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘

Phase 2: Extract to Quarkus (When Scaling Required)
┌─────────────────────────────────────────────────────────────┐
│                    iDempiere Server                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ com.cloudempiere.ai (Thin Client Plugin)             │    │
│  │  - REST client to Quarkus                           │    │
│  │  - Context extraction & security                    │    │
│  │  - UI components (AIChatWidget)                     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │ REST/gRPC
                              ▼
┌─────────────────────────────────────────────────────────────┐
│           cloudempiere-ai-service (Quarkus)                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ REUSED: Chain Implementations (same Java classes)    │    │
│  │  - SalesOpportunityChain.java                       │    │
│  │  - ChartAnalysisChain.java                          │    │
│  │  - IChain interface (unchanged)                     │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ NEW: Quarkus-specific adapters                       │    │
│  │  - REST endpoints exposing chains                   │    │
│  │  - Quarkus LangChain4j configuration               │    │
│  │  - Health checks, metrics                           │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Key Reuse Strategy:**

```java
// 1. Chain interface remains IDENTICAL
public interface IChain {
    String getName();
    ChainResult execute(ChainInput input);
    List<ChainParameter> getConfigurableParameters();
}

// 2. Chain implementations are REUSED without modification
@ChainMetadata(name = "SalesOpportunityAnalysis", version = "1.0")
public class SalesOpportunityChain implements IChain {
    // Same code works in OSGi AND Quarkus
}

// 3. Only the ADAPTER changes
// OSGi: Direct invocation
chainRegistry.get("SalesOpportunityAnalysis").execute(input);

// Quarkus: REST endpoint wrapper
@Path("/chains")
public class ChainResource {
    @Inject IChain salesChain;

    @POST @Path("/sales-opportunity")
    public ChainResult execute(ChainInput input) {
        return salesChain.execute(input);  // Same chain, different deployment
    }
}
```

**Migration Triggers (When to Move to Quarkus):**

| Trigger | Threshold | Action |
|---------|-----------|--------|
| Request volume | >100 requests/min sustained | Consider Quarkus |
| Response latency | >5s average due to contention | Consider Quarkus |
| Team growth | Separate AI team formed | Consider Quarkus |
| Multi-ERP | Second ERP system needs AI | Consider Quarkus |
| Scaling needs | Need horizontal pod autoscaling | Move to Quarkus |

**What Gets Reused (Zero Rewrite):**
- `IChain` interface
- All chain implementations (`*Chain.java`)
- `ChainParameter` definitions
- Business logic and prompts
- Tool implementations (`@Tool` methods)

**What Changes (New Code):**
- REST/gRPC endpoint layer in Quarkus
- Quarkus-specific configuration (`application.properties`)
- iDempiere plugin becomes thin REST client
- Context serialization DTOs

## More Information

### Implementation Architecture

#### Layer 1: Chain Framework (Java/OSGi)

```java
/**
 * Core chain execution framework - provides structure and safety.
 * Deployed via OSGi plugin update.
 */
public class ChainExecutor {
    private final LangChain4jProviderFactory providerFactory;
    private final ChainConfigRepository configRepo;
    private final GuardrailEnforcer guardrails;
    private final ToolRegistry toolRegistry;

    /**
     * Execute a chain with database-loaded configuration
     */
    public ChainResult execute(String chainName, ChainInput input) {
        // 1. Load configuration from database
        ChainConfig config = configRepo.getActiveConfig(chainName);

        // 2. Validate against guardrails (code-enforced, not bypassable)
        guardrails.validateInput(input, config);
        guardrails.validateBudget(input.getContext(), config);

        // 3. Build chain with configured parameters
        Chain chain = buildChain(chainName, config);

        // 4. Execute with observability
        try (var span = tracer.startSpan("chain.execute")) {
            ChainResult result = chain.execute(input);

            // 5. Validate output
            guardrails.validateOutput(result, config);

            return result;
        }
    }

    private Chain buildChain(String chainName, ChainConfig config) {
        // Get the Java chain implementation
        IChain chainImpl = chainRegistry.get(chainName);

        // Apply database configuration
        return chainImpl.configure(config);
    }
}
```

#### Layer 2: Chain Interface (Code Contract)

```java
/**
 * Interface for chain implementations.
 * Each chain is a Java class providing structure.
 */
public interface IChain {
    /**
     * Chain identifier (matches AIG_Chain_Config.Value)
     */
    String getName();

    /**
     * Chain version (for A/B testing support)
     */
    String getVersion();

    /**
     * Configure chain with database parameters
     */
    IChain configure(ChainConfig config);

    /**
     * Execute the chain
     */
    ChainResult execute(ChainInput input);

    /**
     * Declare required tools (validated at startup)
     */
    List<String> getRequiredTools();

    /**
     * Declare configurable parameters (for UI generation)
     */
    List<ChainParameter> getConfigurableParameters();
}
```

#### Layer 3: Example Chain Implementation

```java
/**
 * Sales Opportunity Analysis Chain
 * Structure in code, parameters from database.
 */
@Component
@ChainMetadata(
    name = "SalesOpportunityAnalysis",
    version = "1.0",
    description = "Analyze sales opportunity health and suggest actions"
)
public class SalesOpportunityChain implements IChain {

    // Configurable parameters (values loaded from DB)
    private String systemPromptTemplate;
    private int maxContextRecords;
    private double confidenceThreshold;
    private List<String> enabledAnalysisTypes;

    @Override
    public List<ChainParameter> getConfigurableParameters() {
        return List.of(
            ChainParameter.builder()
                .name("systemPromptTemplate")
                .type(ParameterType.PROMPT)
                .description("System prompt for opportunity analysis")
                .defaultValue(DEFAULT_SYSTEM_PROMPT)
                .build(),
            ChainParameter.builder()
                .name("maxContextRecords")
                .type(ParameterType.INTEGER)
                .description("Max related records to include")
                .defaultValue(50)
                .min(10).max(200)
                .build(),
            ChainParameter.builder()
                .name("confidenceThreshold")
                .type(ParameterType.DECIMAL)
                .description("Minimum confidence for recommendations")
                .defaultValue(0.7)
                .min(0.5).max(0.95)
                .build(),
            ChainParameter.builder()
                .name("enabledAnalysisTypes")
                .type(ParameterType.LIST)
                .description("Analysis types to perform")
                .options("HEALTH_SCORE", "RISK_FACTORS", "NEXT_ACTIONS", "COMPETITOR_ANALYSIS")
                .defaultValue(List.of("HEALTH_SCORE", "RISK_FACTORS", "NEXT_ACTIONS"))
                .build()
        );
    }

    @Override
    public IChain configure(ChainConfig config) {
        this.systemPromptTemplate = config.getPrompt("systemPromptTemplate");
        this.maxContextRecords = config.getInt("maxContextRecords");
        this.confidenceThreshold = config.getDouble("confidenceThreshold");
        this.enabledAnalysisTypes = config.getList("enabledAnalysisTypes");
        return this;
    }

    @Override
    public ChainResult execute(ChainInput input) {
        // Build context with configured limits
        var context = buildContext(input, maxContextRecords);

        // Render prompt with template
        var prompt = renderPrompt(systemPromptTemplate, context);

        // Execute via LangChain4j
        var response = aiService.analyze(prompt);

        // Filter by confidence
        return filterByConfidence(response, confidenceThreshold);
    }
}
```

#### Layer 4: Database Schema

```sql
-- Chain configuration (per-tenant overrides supported)
CREATE TABLE AIG_Chain_Config (
    AIG_Chain_Config_ID     NUMERIC(10,0) NOT NULL,
    AD_Client_ID            NUMERIC(10,0) NOT NULL,
    AD_Org_ID               NUMERIC(10,0) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' NOT NULL,
    Created                 TIMESTAMP NOT NULL,
    CreatedBy               NUMERIC(10,0) NOT NULL,
    Updated                 TIMESTAMP NOT NULL,
    UpdatedBy               NUMERIC(10,0) NOT NULL,

    -- Chain identification
    Value                   VARCHAR(100) NOT NULL,  -- Matches IChain.getName()
    Name                    VARCHAR(255) NOT NULL,
    Description             VARCHAR(2000),

    -- Version control
    Version                 VARCHAR(20) NOT NULL,   -- e.g., "1.0", "1.1-beta"
    IsDefault               CHAR(1) DEFAULT 'Y',    -- Default version for this chain

    -- Provider configuration
    AIG_Provider_ID         NUMERIC(10,0),          -- Override default provider
    ModelName               VARCHAR(100),           -- Override model

    -- Execution parameters
    MaxTokens               NUMERIC(10,0),
    Temperature             NUMERIC(5,2),
    TopP                    NUMERIC(5,2),

    -- Budget guardrails
    MaxCostPerExecution     NUMERIC(10,4),          -- Max $ per single execution
    MaxCostPerDay           NUMERIC(10,4),          -- Daily budget cap
    MaxExecutionsPerHour    NUMERIC(10,0),          -- Rate limit

    -- Feature flags
    IsABTestEnabled         CHAR(1) DEFAULT 'N',
    ABTestPercentage        NUMERIC(5,2),           -- % traffic to this version

    CONSTRAINT AIG_Chain_Config_Key PRIMARY KEY (AIG_Chain_Config_ID)
);

-- Chain parameters (key-value for flexibility)
CREATE TABLE AIG_Chain_Parameter (
    AIG_Chain_Parameter_ID  NUMERIC(10,0) NOT NULL,
    AIG_Chain_Config_ID     NUMERIC(10,0) NOT NULL,
    AD_Client_ID            NUMERIC(10,0) NOT NULL,
    AD_Org_ID               NUMERIC(10,0) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' NOT NULL,

    -- Parameter definition
    ParameterName           VARCHAR(100) NOT NULL,
    ParameterValue          TEXT,                   -- JSON for complex values
    ParameterType           VARCHAR(20) NOT NULL,   -- STRING, INTEGER, DECIMAL, LIST, PROMPT

    -- Validation (code enforces, DB stores)
    MinValue                VARCHAR(100),
    MaxValue                VARCHAR(100),

    CONSTRAINT AIG_Chain_Parameter_Key PRIMARY KEY (AIG_Chain_Parameter_ID),
    CONSTRAINT AIG_ChainParam_Config FOREIGN KEY (AIG_Chain_Config_ID)
        REFERENCES AIG_Chain_Config(AIG_Chain_Config_ID)
);

-- Prompt templates (extends existing AIG_Prompt_Config)
-- Already exists, add version support
ALTER TABLE AIG_Prompt_Config ADD COLUMN Version VARCHAR(20);
ALTER TABLE AIG_Prompt_Config ADD COLUMN ParentConfig_ID NUMERIC(10,0);
ALTER TABLE AIG_Prompt_Config ADD COLUMN IsDefault CHAR(1) DEFAULT 'Y';

-- Chain execution audit
CREATE TABLE AIG_Chain_Execution (
    AIG_Chain_Execution_ID  NUMERIC(10,0) NOT NULL,
    AD_Client_ID            NUMERIC(10,0) NOT NULL,
    AD_Org_ID               NUMERIC(10,0) NOT NULL,

    -- Execution context
    AIG_Chain_Config_ID     NUMERIC(10,0) NOT NULL,
    AD_User_ID              NUMERIC(10,0) NOT NULL,
    ExecutionTimestamp      TIMESTAMP NOT NULL,

    -- Results
    DurationMS              NUMERIC(10,0),
    InputTokens             NUMERIC(10,0),
    OutputTokens            NUMERIC(10,0),
    TotalCost               NUMERIC(10,6),

    -- Status
    Status                  VARCHAR(20),            -- SUCCESS, FAILED, TIMEOUT, BUDGET_EXCEEDED
    ErrorMessage            TEXT,

    -- Version tracking
    ConfigVersion           VARCHAR(20),
    PromptVersion           VARCHAR(20),

    CONSTRAINT AIG_Chain_Execution_Key PRIMARY KEY (AIG_Chain_Execution_ID)
);
```

#### Layer 5: UI Configuration (iDempiere Window)

```
Window: AI Chain Configuration
├── Tab: Chain Config
│   ├── Field: Value (chain identifier)
│   ├── Field: Name
│   ├── Field: Description
│   ├── Field: Version
│   ├── Field: IsDefault
│   ├── Field: Provider (dropdown)
│   ├── Field: Model Name
│   ├── Field: Max Tokens
│   ├── Field: Temperature
│   ├── Field: Max Cost Per Execution
│   └── Field: Max Cost Per Day
├── Tab: Parameters
│   ├── Field: Parameter Name
│   ├── Field: Parameter Value
│   ├── Field: Parameter Type
│   └── Field: Min/Max Value
├── Tab: Prompt Templates
│   ├── Field: System Prompt
│   ├── Field: User Prompt Template
│   └── Field: Few-shot Examples
└── Tab: Execution History
    ├── Field: Timestamp
    ├── Field: User
    ├── Field: Duration
    ├── Field: Cost
    └── Field: Status
```

### Update Strategy

#### Tier 1: Hot Updates (No Deployment)

Changes that can be made via iDempiere UI, effective immediately:

| Change Type | How | Risk |
|-------------|-----|------|
| Prompt wording | AIG_Prompt_Config | Low |
| Temperature/tokens | AIG_Chain_Config | Low |
| Cost budgets | AIG_Chain_Config | Low |
| Feature flags | AIG_Chain_Parameter | Low |
| Enable/disable chain | AIG_Chain_Config.IsActive | Low |

#### Tier 2: OSGi Plugin Update (No Server Restart)

Changes requiring plugin redeployment:

| Change Type | How | Risk |
|-------------|-----|------|
| New chain implementation | New Java class | Medium |
| New tool | New @Tool method | Medium |
| Chain logic change | Java code update | Medium |
| LangChain4j upgrade | pom.xml dependency | Medium |
| New provider support | Provider factory | Medium |

**OSGi Hot Deploy Process:**
```bash
# 1. Build updated plugin
cd com.cloudempiere.ai
mvn clean package -DskipTests

# 2. Copy to iDempiere plugins folder
cp target/com.cloudempiere.ai-*.jar $IDEMPIERE_HOME/plugins/

# 3. Refresh OSGi bundle (no server restart)
# Via OSGi console or REST API
osgi> update com.cloudempiere.ai
```

#### Tier 3: Full Deployment (Server Restart)

Changes requiring iDempiere core update:

| Change Type | How | Risk |
|-------------|-----|------|
| New AD tables | 2Pack migration | High |
| UI component changes | ZK changes | High |
| iDempiere API changes | Core modifications | High |

### Version Management

```java
/**
 * Chain version resolution
 */
public class ChainVersionResolver {

    public ChainConfig resolveVersion(String chainName, Properties ctx) {
        int clientId = Env.getAD_Client_ID(ctx);

        // 1. Check for client-specific override
        ChainConfig clientConfig = repo.findByChainAndClient(chainName, clientId);
        if (clientConfig != null && clientConfig.isActive()) {
            return clientConfig;
        }

        // 2. Check for A/B test
        ChainConfig abTest = resolveABTest(chainName, ctx);
        if (abTest != null) {
            return abTest;
        }

        // 3. Return default version
        return repo.findDefaultVersion(chainName);
    }

    private ChainConfig resolveABTest(String chainName, Properties ctx) {
        List<ChainConfig> abConfigs = repo.findABTestConfigs(chainName);
        if (abConfigs.isEmpty()) {
            return null;
        }

        // Deterministic selection based on user ID for consistent experience
        int userId = Env.getAD_User_ID(ctx);
        double bucket = (userId % 100) / 100.0;

        double cumulative = 0;
        for (ChainConfig config : abConfigs) {
            cumulative += config.getABTestPercentage() / 100.0;
            if (bucket < cumulative) {
                return config;
            }
        }

        return null;
    }
}
```

### Rollback Strategy

```java
/**
 * Quick rollback via database flag
 */
public class ChainRollbackService {

    /**
     * Rollback to previous version (instant, no deployment)
     */
    public void rollbackToPreviousVersion(String chainName) {
        // 1. Find current default
        ChainConfig current = repo.findDefaultVersion(chainName);

        // 2. Find previous version
        ChainConfig previous = repo.findPreviousVersion(chainName, current.getVersion());

        // 3. Swap defaults
        current.setIsDefault(false);
        previous.setIsDefault(true);

        repo.save(current);
        repo.save(previous);

        // 4. Log rollback
        auditLog.record("CHAIN_ROLLBACK", chainName,
            current.getVersion() + " -> " + previous.getVersion());
    }

    /**
     * Emergency disable (instant)
     */
    public void emergencyDisable(String chainName) {
        repo.findAllVersions(chainName)
            .forEach(config -> {
                config.setIsActive(false);
                repo.save(config);
            });

        auditLog.record("CHAIN_EMERGENCY_DISABLE", chainName, "All versions disabled");
    }
}
```

### Implementation Notes

#### Phase 1: Framework Foundation

1. Create `AIG_Chain_Config` table and AD Window
2. Implement `ChainExecutor` framework
3. Implement `ChainVersionResolver`
4. Migrate `MAIPromptConfig` to support versioning

#### Phase 2: Existing Chain Migration

1. Refactor `SalesOpportunityChain` to use new framework
2. Refactor `ChartAnalysisChain` to use new framework
3. Add configuration UI for each chain
4. Implement A/B testing support

#### Phase 3: Observability & Governance

1. Implement `AIG_Chain_Execution` audit logging
2. Add cost tracking dashboard
3. Implement rollback UI
4. Add version comparison tools

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j adoption (foundation)
- [ADR-004](004-java-agent-framework.md) - Java Agent Framework Selection
- [ADR-010](010-agent-orchestration-architecture.md) - Agent Orchestration Architecture
- [ADR-013](013-observability-cost-tracking.md) - Observability and Cost Tracking
- [ADR-014](014-guardrails-and-safety.md) - Guardrails and Safety

### References

- [LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services/)
- [iDempiere Application Dictionary](https://wiki.idempiere.org/en/Application_Dictionary)
- [OSGi Hot Deployment](https://wiki.idempiere.org/en/Developing_Plug-Ins_-_Adding_Dependency_JARs)
- [A/B Testing Best Practices](https://www.optimizely.com/optimization-glossary/ab-testing/)
- [Quarkus LangChain4j Extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services)

---

*ADR-027 | Version 1.0 | 2025-12-03*
