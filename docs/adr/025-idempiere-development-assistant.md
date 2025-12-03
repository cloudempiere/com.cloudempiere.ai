# ADR-025: iDempiere Development Assistant Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: CloudEmpiere AI Team
**Phase**: 3 (Future)
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-003](003-mcp-server-integration.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

iDempiere development has a steep learning curve:
- Application Dictionary is complex (100+ AD tables)
- Best practices are scattered across wiki, forums, and code
- New developers make common mistakes repeatedly
- Code generation is manual and error-prone
- Plugin structure requires extensive boilerplate

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| New developer onboarding | 2-4 weeks | 3-5 days | 80% faster |
| Window/tab creation time | 2-4 hours | 15-30 min | 85% reduction |
| Common mistakes | Frequent | Rare | Quality improvement |
| Code review cycles | 2-3 rounds | 1 round | Efficiency |

### Reference Implementation

The [idempiere-mcp-server](../../../idempiere-mcp-server) already provides MCP tools for:
- `listTables` - List AD tables
- `describeTable` - Get table structure
- `addTable` - Create new table
- `addColumn` - Add column to table
- `syncTable` - Sync model classes

This ADR extends those capabilities with AI-driven assistance.

## Decision

Implement an **iDempiere Development Assistant** that uses AI to help developers with Application Dictionary operations, code generation, and best practice guidance.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Developer Interface                                │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  IDE (Claude Code, Cursor)  │  Chat UI  │  CLI                 │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    iDempiereDevAssistantAgent                         │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: iDempiere expert developer                   │  │
│  │   - Tools: AD operations, code generation, validation          │  │
│  │   - RAG: iDempiere documentation, wiki, best practices         │  │
│  │   - MCP Integration: idempiere-mcp-server tools                │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
┌────────────────────────┐    ┌────────────────────────────────────────┐
│  idempiere-mcp-server  │    │  iDempiere Plugin Project               │
│  ┌──────────────────┐  │    │  ┌──────────────────────────────────┐  │
│  │ MCP Tools:       │  │    │  │ Generated Artifacts:              │  │
│  │ - listTables     │  │    │  │ - Model classes (I_*, X_*, M*)   │  │
│  │ - describeTable  │  │    │  │ - Window/Tab/Field definitions   │  │
│  │ - addTable       │  │    │  │ - Process classes                │  │
│  │ - addColumn      │  │    │  │ - Callout classes                │  │
│  │ - syncTable      │  │    │  │ - Model validators               │  │
│  │ - (future: more) │  │    │  │ - 2Pack XML                      │  │
│  └──────────────────┘  │    │  └──────────────────────────────────┘  │
└────────────────────────┘    └────────────────────────────────────────┘
```

### Capability Matrix

```
┌─────────────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT ASSISTANT CAPABILITIES                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. APPLICATION DICTIONARY                                           │
│     ├── Create table with proper naming conventions                  │
│     ├── Add columns with correct data types                         │
│     ├── Define references (foreign keys, lists)                     │
│     ├── Create window/tab/field structure                           │
│     ├── Set up menu entries                                         │
│     └── Generate 2Pack for deployment                               │
│                                                                      │
│  2. CODE GENERATION                                                  │
│     ├── Model classes (I_*, X_*, M*)                                │
│     ├── SvrProcess classes with parameters                          │
│     ├── ModelValidator hooks                                        │
│     ├── Callout implementations                                     │
│     ├── Info window classes                                         │
│     └── Form controllers                                            │
│                                                                      │
│  3. BEST PRACTICES                                                   │
│     ├── Naming convention validation                                │
│     ├── Security pattern suggestions                                │
│     ├── Performance optimization tips                               │
│     ├── Multi-tenant considerations                                 │
│     └── Code review checklist                                       │
│                                                                      │
│  4. DOCUMENTATION                                                    │
│     ├── Explain existing tables/windows                             │
│     ├── Generate Javadoc comments                                   │
│     ├── Create user documentation                                   │
│     └── Answer "how to" questions                                   │
│                                                                      │
│  5. TROUBLESHOOTING                                                  │
│     ├── Diagnose common errors                                      │
│     ├── Suggest fixes for exceptions                                │
│     ├── Explain stack traces                                        │
│     └── Debug SQL/HQL issues                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### System Prompt

```
You are an expert iDempiere ERP developer assistant.

KNOWLEDGE BASE:
- iDempiere architecture (OSGi, Compiere heritage)
- Application Dictionary (AD_* tables)
- Model-View-Controller patterns
- Best practices for multi-tenant systems
- Security considerations

AVAILABLE TOOLS:
{tools_description}

TASK: Help developers with iDempiere customization and plugin development.

RULES:

1. Naming Conventions:
   - Tables: Use EntityName (e.g., C_Order, M_Product)
   - Custom tables: Prefix with module code (e.g., XX_MyTable)
   - Columns: Use ColumnName (e.g., DocumentNo, DateOrdered)
   - Never use reserved words

2. Application Dictionary:
   - Always create AD_Element before AD_Column
   - Use proper AD_Reference_ID for data types
   - Set up foreign keys via AD_Reference_Value
   - Define proper AD_Val_Rule for validation

3. Code Patterns:
   - Model classes extend X_ generated class
   - Use beforeSave/afterSave for validation/automation
   - SvrProcess for batch operations
   - Callouts for UI-driven logic

4. Security:
   - Always check AD_Role access
   - Use Env.getAD_Client_ID() for multi-tenant
   - Never hardcode credentials
   - Validate user input

5. Performance:
   - Use Query class instead of raw SQL
   - Implement caching for master data
   - Avoid N+1 queries
   - Use PreparedStatement

OUTPUT FORMAT:
When generating code, provide:
1. Complete, runnable code
2. Explanation of key decisions
3. Integration instructions
4. Testing suggestions

When answering questions:
1. Direct answer first
2. Explanation with examples
3. Related best practices
4. Links to documentation if available
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.developer;

@Agent(
    name = "iDempiereDevAssistant",
    domain = "DEVELOPMENT",
    riskLevel = RiskLevel.LOW
)
public interface IDempiereDevAssistantAgent {

    @SystemMessage(fromResource = "prompts/idempiere-dev-assistant.txt")
    String chat(
        @MemoryId String sessionId,
        @UserMessage String developerQuestion
    );

    @SystemMessage(fromResource = "prompts/table-creation.txt")
    TableCreationPlan planTableCreation(
        @UserMessage String requirements
    );

    @SystemMessage(fromResource = "prompts/code-generation.txt")
    GeneratedCode generateCode(
        @UserMessage String codeRequest
    );

    @SystemMessage(fromResource = "prompts/code-review.txt")
    CodeReview reviewCode(
        @UserMessage String codeToReview
    );
}

public record TableCreationPlan(
    @Description("Table definition") TableDefinition table,
    @Description("Column definitions") List<ColumnDefinition> columns,
    @Description("Window/Tab structure") WindowDefinition window,
    @Description("Menu entry") MenuDefinition menu,
    @Description("Estimated effort") String effort,
    @Description("Implementation steps") List<String> steps,
    @Description("Best practices notes") List<String> best_practices
) {}

public record TableDefinition(
    String tableName,
    String name,
    String description,
    String entityType,
    String accessLevel,
    boolean isHighVolume,
    boolean isChangeLog,
    List<String> foreignKeys
) {}

public record ColumnDefinition(
    String columnName,
    String name,
    String description,
    int referenceId,
    Integer referenceValueId,
    boolean isMandatory,
    boolean isKey,
    boolean isParent,
    String defaultValue,
    Integer fieldLength,
    String validationRule
) {}

public record GeneratedCode(
    @Description("Generated source code") String code,
    @Description("File path suggestion") String filePath,
    @Description("Code explanation") String explanation,
    @Description("Dependencies required") List<String> dependencies,
    @Description("Testing suggestions") List<String> testSuggestions
) {}

public record CodeReview(
    @Description("Overall assessment") String assessment,
    @Description("Score 0-100") int score,
    @Description("Issues found") List<CodeIssue> issues,
    @Description("Improvements suggested") List<String> improvements,
    @Description("Security concerns") List<String> securityConcerns,
    @Description("Performance notes") List<String> performanceNotes
) {}

public record CodeIssue(
    String severity,  // ERROR, WARNING, INFO
    int lineNumber,
    String message,
    String suggestion
) {}
```

### Boundaries (per ADR-009)

```yaml
Agent: IDempiereDevAssistantAgent
Domain: DEVELOPMENT
Risk Level: LOW (advisory only, no production data access)

Data Access:
  Read Tables:
    - AD_Table, AD_Column, AD_Element
    - AD_Window, AD_Tab, AD_Field
    - AD_Menu, AD_Process, AD_Form
    - AD_Reference, AD_Ref_List, AD_Ref_Table
    - AD_Val_Rule, AD_Callout
    - AD_ModelValidator, AD_EntityType
    - (All AD_* metadata tables)

  Write Tables: None (advisory only)
    - Writes via MCP tools go through idempiere-mcp-server

  Forbidden Tables:
    - Business data tables (C_*, M_*, etc.)
    - AD_User credentials
    - Financial data
    - Customer data

Organizational:
  - System-level access for AD tables
  - No client-specific data access
  - Code generation is file-based, not DB

Token Limits:
  Max Tokens/Request: 20,000 (code generation needs space)

Cost:
  Daily Budget: $50
  Cost per Interaction: ~$0.10 (estimated)

Performance:
  Response Time: <10 seconds (code generation)
```

### Development Tools

```java
public class IDempiereDevTools {

    @Tool("List all tables in the Application Dictionary")
    public String listTables(
        @P("Filter pattern (optional)") String pattern,
        @P("Entity type filter (optional)") String entityType
    ) {
        // Query AD_Table
        // Return list with name, description, entity type
    }

    @Tool("Get detailed table structure including columns")
    public String describeTable(
        @P("Table name") String tableName
    ) {
        // Query AD_Table + AD_Column
        // Return full structure with column details
    }

    @Tool("Get window/tab structure")
    public String describeWindow(
        @P("Window name or ID") String window
    ) {
        // Query AD_Window + AD_Tab + AD_Field
        // Return hierarchical structure
    }

    @Tool("Get process definition")
    public String describeProcess(
        @P("Process name or ID") String process
    ) {
        // Query AD_Process + AD_Process_Para
        // Return process structure with parameters
    }

    @Tool("Search iDempiere documentation")
    public String searchDocs(
        @P("Search query") String query
    ) {
        // RAG search against iDempiere wiki/docs
        // Return relevant documentation snippets
    }

    @Tool("Validate table/column naming")
    public String validateNaming(
        @P("Entity type (TABLE, COLUMN, etc.)") String entityType,
        @P("Proposed name") String name
    ) {
        // Check against naming conventions
        // Return validation result with suggestions
    }

    @Tool("Generate model class code")
    public String generateModelCode(
        @P("Table name") String tableName,
        @P("Package name") String packageName,
        @P("Include business logic") boolean includeBusinessLogic
    ) {
        // Generate I_*, X_*, M* classes
        // Return code with proper formatting
    }

    @Tool("Generate process class code")
    public String generateProcessCode(
        @P("Process name") String processName,
        @P("Parameters JSON") String parametersJson,
        @P("Package name") String packageName
    ) {
        // Generate SvrProcess subclass
        // Include parameter handling
    }

    @Tool("Generate callout class code")
    public String generateCalloutCode(
        @P("Table name") String tableName,
        @P("Column name") String columnName,
        @P("Logic description") String logicDescription
    ) {
        // Generate CalloutEngine subclass
        // Include trigger logic
    }
}
```

### RAG Knowledge Base

```java
public class IDempiereKnowledgeBase {

    /**
     * Knowledge sources for RAG
     */
    public enum KnowledgeSource {
        WIKI,           // wiki.idempiere.org content
        JAVADOC,        // iDempiere API documentation
        FORUMS,         // Community forum discussions
        BEST_PRACTICES, // Internal best practices docs
        EXAMPLES        // Code examples and templates
    }

    /**
     * Embed and index iDempiere documentation
     */
    public void indexDocumentation() {
        // 1. Wiki pages
        indexWikiPages();

        // 2. Javadoc
        indexJavadoc();

        // 3. Best practices
        indexBestPractices();

        // 4. Code examples
        indexCodeExamples();
    }

    /**
     * Search knowledge base
     */
    public List<KnowledgeChunk> search(String query, int maxResults) {
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("nomic-embed-text")
            .build();

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }

    /**
     * Common questions with cached answers
     */
    public static final Map<String, String> COMMON_QUESTIONS = Map.of(
        "how to create a table",
        "Use AD_Table and AD_Column. See wiki: Creating_Tables",

        "how to create a window",
        "Use AD_Window, AD_Tab, AD_Field hierarchy. See wiki: Creating_Windows",

        "how to create a process",
        "Create AD_Process, implement SvrProcess. See wiki: Creating_Processes",

        "naming conventions",
        "Tables: C_ for Commercial, M_ for Material, etc. See wiki: Naming_Conventions"
    );
}
```

### Code Templates

```java
public class CodeTemplates {

    /**
     * Model class template
     */
    public static final String MODEL_TEMPLATE = """
        package {{package}};

        import java.sql.ResultSet;
        import java.util.Properties;

        /**
         * {{description}}
         *
         * @author AI Generated
         * @version $Id$
         */
        public class M{{entityName}} extends X_{{tableName}} {

            private static final long serialVersionUID = 1L;

            /**
             * Standard Constructor
             */
            public M{{entityName}}(Properties ctx, int {{tableName}}_ID, String trxName) {
                super(ctx, {{tableName}}_ID, trxName);
            }

            /**
             * Load Constructor
             */
            public M{{entityName}}(Properties ctx, ResultSet rs, String trxName) {
                super(ctx, rs, trxName);
            }

            {{#beforeSave}}
            @Override
            protected boolean beforeSave(boolean newRecord) {
                if (!super.beforeSave(newRecord)) {
                    return false;
                }
                // Custom validation logic
                {{beforeSaveLogic}}
                return true;
            }
            {{/beforeSave}}

            {{#afterSave}}
            @Override
            protected boolean afterSave(boolean newRecord, boolean success) {
                if (!success) {
                    return false;
                }
                // Custom post-save logic
                {{afterSaveLogic}}
                return true;
            }
            {{/afterSave}}
        }
        """;

    /**
     * Process class template
     */
    public static final String PROCESS_TEMPLATE = """
        package {{package}};

        import org.compiere.process.SvrProcess;
        import org.compiere.process.ProcessInfoParameter;

        /**
         * {{description}}
         *
         * @author AI Generated
         */
        public class {{className}} extends SvrProcess {

            {{#parameters}}
            private {{type}} p_{{name}};
            {{/parameters}}

            @Override
            protected void prepare() {
                for (ProcessInfoParameter para : getParameter()) {
                    String name = para.getParameterName();
                    {{#parameters}}
                    if (name.equals("{{name}}")) {
                        p_{{name}} = para.getParameter{{typeMethod}}();
                    }
                    {{/parameters}}
                }
            }

            @Override
            protected String doIt() throws Exception {
                // Process logic
                {{processLogic}}

                return "@OK@";
            }
        }
        """;

    /**
     * Callout template
     */
    public static final String CALLOUT_TEMPLATE = """
        package {{package}};

        import org.compiere.model.CalloutEngine;
        import org.compiere.model.GridField;
        import org.compiere.model.GridTab;

        import java.util.Properties;

        /**
         * {{description}}
         *
         * @author AI Generated
         */
        public class {{className}} extends CalloutEngine {

            /**
             * {{methodDescription}}
             */
            public String {{methodName}}(Properties ctx, int WindowNo,
                                         GridTab mTab, GridField mField, Object value) {
                if (isCalloutActive() || value == null) {
                    return "";
                }

                // Callout logic
                {{calloutLogic}}

                return "";
            }
        }
        """;
}
```

## Implementation Plan

### Phase 1: Core Agent (Days 1-5)

| Task | Effort | Owner |
|------|--------|-------|
| `IDempiereDevAssistantAgent` interface | 4h | Dev |
| System prompt for development assistance | 8h | Dev |
| Basic tools (listTables, describeTable) | 6h | Dev |
| MCP server integration | 6h | Dev |
| Chat interface | 6h | Dev |

### Phase 2: Code Generation (Days 6-10)

| Task | Effort | Owner |
|------|--------|-------|
| Model class generation | 8h | Dev |
| Process class generation | 6h | Dev |
| Callout generation | 4h | Dev |
| Code templates | 6h | Dev |
| Validation tools | 6h | Dev |

### Phase 3: Knowledge Base (Days 11-15)

| Task | Effort | Owner |
|------|--------|-------|
| Wiki indexing (RAG) | 8h | Dev |
| Javadoc indexing | 4h | Dev |
| Best practices docs | 6h | Dev |
| Search integration | 6h | Dev |
| Common Q&A cache | 4h | Dev |

### Phase 4: UI & Testing (Days 16-20)

| Task | Effort | Owner |
|------|--------|-------|
| Chat UI in iDempiere | 8h | Dev |
| IDE integration guide | 4h | Dev |
| Testing with real scenarios | 12h | QA |
| Developer feedback | 8h | PM |
| Documentation | 8h | Doc |

### Deliverables

- `IDempiereDevAssistantAgent.java` (~200 lines)
- `IDempiereDevTools.java` (~400 lines)
- `IDempiereKnowledgeBase.java` (~300 lines)
- `CodeTemplates.java` (~200 lines)
- `idempiere-dev-assistant.txt` (system prompt)
- Code generation templates
- RAG knowledge base
- Chat UI component
- IDE integration guide

**Total Effort**: 20 days
**Risk Level**: Medium (knowledge base quality, code generation accuracy)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Answer iDempiere development questions
  - [ ] Generate model classes from table definitions
  - [ ] Generate process classes with parameters
  - [ ] Generate callout classes
  - [ ] Validate naming conventions
  - [ ] Search documentation
  - [ ] Provide best practice guidance

Non-Functional:
  - [ ] Response time: <10 seconds
  - [ ] Code generation accuracy: 90%+ compilable
  - [ ] Documentation relevance: 85%+ helpful
  - [ ] Cost per interaction: <$0.15

Code Quality:
  - [ ] Generated code follows iDempiere patterns
  - [ ] Proper naming conventions
  - [ ] Security best practices
  - [ ] Multi-tenant awareness
```

## Consequences

### Positive

1. **Faster Onboarding**: New developers productive faster
2. **Consistency**: Generated code follows best practices
3. **Quality**: Fewer common mistakes
4. **Documentation**: Always-available expert knowledge
5. **Efficiency**: Less time on boilerplate

### Negative

1. **Knowledge Gap**: May miss recent iDempiere changes
2. **Template Limits**: Not all scenarios covered
3. **Over-reliance**: Developers may not learn fundamentals
4. **Maintenance**: Templates need updates with iDempiere

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Outdated knowledge | MEDIUM | Regular RAG updates |
| Incorrect code | MEDIUM | Compilation checks, reviews |
| Security issues | HIGH | Security-focused prompts |
| Developer skill atrophy | LOW | Educational explanations |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Answer quality | 90%+ helpful | Developer survey |
| Code compilability | 90%+ | Build tests |
| Onboarding time | 80% reduction | Before/after |
| Developer satisfaction | 4.5/5 | NPS |
| Common mistakes | 50% reduction | Code review tracking |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-003: MCP Server Integration](003-mcp-server-integration.md)
- [idempiere-mcp-server](../../../idempiere-mcp-server)
- [iDempiere Wiki](https://wiki.idempiere.org/)

---

*ADR-025 | Version 1.0 | 2025-12-03*
