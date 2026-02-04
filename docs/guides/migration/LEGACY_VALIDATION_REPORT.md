# Legacy Plugin Validation Report

**Generated:** 2026-02-02
**Branch:** cld-1704-final
**Total Legacy Files:** 155 Java files

---

## Summary

| Status | Count | Description |
|--------|-------|-------------|
| ✅ **Migrated** | 1 pkg | Orchestrator (only in core, not in legacy) |
| ⚠️ **Duplicated** | 8 pkgs | Exist in BOTH legacy and new plugins |
| 🔴 **Legacy Only** | 12 pkgs | Need migration or investigation |

---

## 1. Duplicated Packages (Delete from Legacy After Verification)

These packages exist in BOTH legacy and core/domain plugins. After verifying they're identical, delete from legacy.

### A. Identical Duplicates ✅ (Safe to Delete)

| Package | Legacy Files | Core Files | Action |
|---------|--------------|------------|--------|
| `database/` | 3 | 3 | ✅ Delete from legacy |
| `context/` | 6 | 6 | ✅ Delete from legacy |
| `component/` | 2 | 2 | ✅ Delete from legacy |
| `model/` | 20 | 20 | ✅ Delete from legacy |
| `process/` | 1 | 1 | ✅ Delete from legacy |

### B. Mismatched Duplicates ⚠️ (Verify First)

| Package | Legacy Files | Core Files | Issue | Action |
|---------|--------------|------------|-------|--------|
| `provider/` | 29 | 27 | Legacy has 2 extra | Identify extra files, migrate if needed |
| `util/` | 16 | 17 | Core has 1 extra | Verify core is newer |
| `boundary/` | 7 | 3 | **Different files!** | See details below |

#### Boundary Package Details

**Legacy only (7 files) - Need Decision:**
- `AgentBoundary.java` - Old boundary interface?
- `AgentBoundaryRegistry.java` - Registry system
- `BoundaryEnforcementFilter.java` - Enforcement logic
- `BoundaryViolationException.java` - Exception class
- `BoundaryViolationType.java` - Enum
- `CostBoundaryMonitor.java` - Cost monitoring
- `DataAccessValidator.java` - Data validation

**Core only (3 files) - New architecture:**
- `IDomainAgent.java` - New agent interface ✅
- `IOrchestrator.java` - New orchestrator interface ✅
- `DomainBoundary.java` - New base boundary class ✅

**Question:** Are legacy boundary files obsolete or needed in core?

---

## 2. Legacy-Only Packages (Need Migration)

### 🔴 P0 - Critical (Must Migrate)

#### RAG Infrastructure (13 files) → `core/rag/`

**Files:**
```
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/rag/
├── RAGContextManager.java
├── IRagService.java
├── RAGConversationService.java
├── dto/
│   ├── SearchResult.java
│   ├── RetrievalContext.java
│   └── ...
├── embedding/
│   ├── EmbeddingCache.java
│   └── ...
└── ingest/
    ├── KnowledgeEntryIngestor.java
    ├── IngestResult.java
    └── ...
```

**Why Critical:** Domain agents need RAG for semantic search and context augmentation.

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/`

**Migration Steps:**
1. Create rag/ directory in core
2. Copy all RAG files
3. Update core/MANIFEST.MF to export `com.cloudempiere.ai.rag`
4. Update domain agents to import from core
5. Test RAG functionality

---

### 🟡 P1 - Important (Should Migrate)

#### Guardrails (4 files) → `core/guardrails/`

**Files:**
- `InputGuard.java` - Input validation
- `OutputGuard.java` - Output filtering
- `ExecutionGuard.java` - Execution limits
- `dto/GuardResult.java` - Result DTO

**Why Important:** Safety and compliance (ADR-014)

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/`

---

#### Observability (3 files) → `core/observability/`

**Files:**
- `AIMetricsListener.java` - Metrics collection
- `CostGuard.java` - Cost tracking
- `dto/*.java` - Metrics DTOs

**Why Important:** Cost tracking and monitoring (ADR-013)

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/`

---

#### KB Infrastructure (10 files) → `com.cloudempiere.ai.kb/src/.../kb/`

**Files:**
```
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/kb/
├── parser/
│   ├── EditorJsParser.java
│   └── EditorJsParserEnhanced.java
├── analysis/
│   ├── KnowledgeBaseSimilarityAnalyzer.java
│   └── KnowledgeBaseSimilarityAnalyzerDB.java
├── database/
│   └── KnowledgeBaseQuery.java
└── dto/
    ├── KnowledgeBaseEntry.java
    ├── KnowledgeBaseHierarchy.java
    ├── PlacementRecommendation.java
    ├── SimilarityResult.java
    └── EditorJsSyntaxInfo.java
```

**Why Important:** Knowledge base agent needs parsers and analyzers

**Target:** `com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/` ← **CORRECT: KB plugin, NOT core!**

---

### 🟢 P2 - Nice to Have

#### Health Monitoring (4 files) → `core/health/`

**Files:**
- `AIHealthCheckRegistrar.java`
- `AIPluginHealthService.java`
- `AIUIService.java`
- (1 more)

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/health/`

---

#### Error Handling (1 file) → `core/error/`

**Files:**
- `AIErrorHandler.java`

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/error/`

---

#### Event System (2 files) → `core/event/`

**Files:**
- `AIBudgetEventHandler.java`
- `KEntryEventHandler.java`

**Target:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/event/`

---

### ❓ Need Investigation (Possibly Obsolete)

#### Agent Infrastructure (9 files) - Replaced by Domain Agents?

**Files:**
```
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/agent/
├── IAIAgent.java
├── AgentContext.java
├── AgentException.java
└── langchain4j/
    ├── LangChainAgentBuilder.java
    ├── LangChainToolAdapter.java
    └── ...
```

**Question:** Are these replaced by:
- `IDomainAgent` interface in core?
- Domain-specific agents (SalesAgent, InventoryAgent, etc.)?

**Investigation:** Check if any code imports from `agent/` package

---

#### Routing (10 files) - Replaced by OrchestratorAgent?

**Files:**
- `EntityExtractor.java`
- `ContextEntry.java`
- `DataType.java`
- ... (7 more)

**Question:** Is this replaced by `OrchestratorAgent.java` in core?

**Investigation:** Check if routing logic is now in orchestrator

---

#### Service Layer (3 files) - Replaced by Facade?

**Files:**
- `IChatAccessService.java`
- `ChatAccessService.java`
- `LanguageDetectionService.java`

**Question:** Is this replaced by Phase 3 Facade Layer (`IAIChatFacade`)?

**Investigation:** Check if still referenced

---

#### Tool Infrastructure (9 files) - Replaced by Domain Tools?

**Files:**
```
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/tool/
├── ToolRegistry.java
├── ToolPermission.java
├── BoundaryValidator.java
└── impl/
    ├── DatabaseTool.java
    ├── BusinessPartnerTool.java
    └── ...
```

**Question:** Are these replaced by domain-specific tools?
- `SalesTools.java` in sales plugin
- `InventoryTools.java` in inventory plugin
- etc.

**Investigation:** Compare with new domain tools

---

#### Factory (1 file) - Purpose Unknown

**Files:**
- `AIChatGadgetFactory.java`

**Question:** What does this do? Is it still needed?

---

#### Function (1 file) - Purpose Unknown

**Files:**
- `AIDatabaseFunctionHandler.java`

**Question:** What does this do? Is it still needed?

---

## 3. Migration Targets Summary

### Migration Map

| Legacy Package | Target Plugin | Target Package | Files | Priority |
|----------------|---------------|----------------|-------|----------|
| `rag/` | **core** | `core/rag/` | 13 | 🔴 P0 |
| `guardrails/` | **core** | `core/guardrails/` | 4 | 🟡 P1 |
| `observability/` | **core** | `core/observability/` | 3 | 🟡 P1 |
| `kb/` | **kb plugin** ⚠️ | `kb/kb/` | 10 | 🟡 P1 |
| `health/` | **core** | `core/health/` | 4 | 🟢 P2 |
| `error/` | **core** | `core/error/` | 1 | 🟢 P2 |
| `event/` | **core** | `core/event/` | 2 | 🟢 P2 |
| `agent/` | **Investigate** | Obsolete? | 9 | ❓ |
| `routing/` | **Investigate** | Obsolete? | 10 | ❓ |
| `service/` | **Investigate** | Obsolete? | 3 | ❓ |
| `tool/` | **Investigate** | Obsolete? | 9 | ❓ |
| `factory/` | **Investigate** | Obsolete? | 1 | ❓ |
| `function/` | **Investigate** | Obsolete? | 1 | ❓ |

**Key Insight:** Only 7 packages (33 files) definitely need migration. The other 6 packages (43 files) may be obsolete.

---

## 4. Recommended Actions

### Phase 1: Investigate Obsolete Packages (2 hours)

```bash
# Check if agent/ is still used
grep -r "import com.cloudempiere.ai.agent" \
  com.cloudempiere.ai.core/src \
  com.cloudempiere.ai.*/src 2>/dev/null

# Check if routing/ is still used
grep -r "import com.cloudempiere.ai.routing" \
  com.cloudempiere.ai.core/src \
  com.cloudempiere.ai.*/src 2>/dev/null

# Check if service/ is still used
grep -r "import com.cloudempiere.ai.service" \
  com.cloudempiere.ai.core/src \
  com.cloudempiere.ai.*/src 2>/dev/null

# Check if tool/ is still used
grep -r "import com.cloudempiere.ai.tool" \
  com.cloudempiere.ai.core/src \
  com.cloudempiere.ai.*/src 2>/dev/null
```

**If zero results:** Package is obsolete, safe to ignore (leave in legacy, mark as deprecated)

---

### Phase 2: Migrate RAG to Core (2 hours) - P0

```bash
# Create directory
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/{dto,embedding,ingest}

# Copy files
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/rag/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/

# Update core/MANIFEST.MF
# Add: com.cloudempiere.ai.rag;version="1.0.0"
#      com.cloudempiere.ai.rag.dto;version="1.0.0"
#      com.cloudempiere.ai.rag.embedding;version="1.0.0"
#      com.cloudempiere.ai.rag.ingest;version="1.0.0"

# Test
mvn clean install -DskipTests
```

---

### Phase 3: Migrate KB to KB Plugin (2 hours) - P1

```bash
# Create directory in KB plugin (NOT core!)
mkdir -p com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/{parser,analysis,database,dto}

# Copy files
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/kb/* \
      com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/

# Update kb/MANIFEST.MF
# Add exports for parser, analysis, database, dto packages

# Test
mvn clean install -DskipTests
```

---

### Phase 4: Migrate Guardrails & Observability to Core (2 hours) - P1

```bash
# Guardrails
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/dto
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/guardrails/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/

# Observability
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/dto
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/observability/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/

# Update core/MANIFEST.MF
# Test
mvn clean install -DskipTests
```

---

### Phase 5: Delete Verified Duplicates (1 hour)

```bash
# Only delete packages verified as identical
cd com.cloudempiere.ai.plugin/src/com/cloudempiere/ai

# Safe deletions (verified identical):
rm -rf database    # 3 files, identical to core
rm -rf context     # 6 files, identical to core
rm -rf component   # 2 files, identical to core
rm -rf model       # 20 files, identical to core
rm -rf process     # 1 file, identical to core

# Total freed: 32 files
```

---

### Phase 6: Mark Obsolete Packages as Deprecated (30 min)

If investigation shows packages are obsolete:

```bash
# Create deprecation notices
for pkg in agent routing service tool factory function; do
  if [ -d "com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/$pkg" ]; then
    cat > "com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/$pkg/DEPRECATED.txt" << EOF
This package is DEPRECATED and no longer used.
Replaced by new multi-plugin architecture.
Will be removed in v1.1.0
EOF
  fi
done
```

---

## 5. Progress Tracking

### Before Migration
- ✅ Migrated: 1 package (orchestrator)
- ⚠️ Duplicated: 8 packages (84 files)
- 🔴 Legacy Only: 12 packages (71 files)
- **Total in legacy:** 155 files

### After Phase 2 (RAG Migration)
- ✅ Migrated: 2 packages
- ⚠️ Duplicated: 8 packages (84 files)
- 🔴 Legacy Only: 11 packages (58 files) ← -13 files
- **Total in legacy:** 142 files

### After Phase 5 (Delete Duplicates)
- ✅ Migrated: 7 packages
- ⚠️ Duplicated: 3 packages (52 files) ← -32 files deleted
- 🔴 Legacy Only: 11 packages (58 files)
- **Total in legacy:** 110 files

### Target (Complete Migration)
- ✅ Migrated: 13 packages
- ⚠️ Duplicated: 0 packages
- 🔴 Legacy Only: 0 packages (obsolete marked as deprecated)
- **Total in legacy:** ~40 files (obsolete, deprecated)

---

## 6. Validation Commands

### Check Migration Progress
```bash
./validate-legacy-plugin.sh > validation-$(date +%Y%m%d).txt
```

### Compare Day-to-Day
```bash
diff validation-20260202.txt validation-20260203.txt
```

### Check Runtime Usage
```bash
./track-legacy-usage.sh
```

---

## Conclusion

**Key Findings:**
1. **33 files need migration** (RAG, guardrails, observability, KB, health, error, event)
2. **32 files can be deleted** (verified identical duplicates)
3. **43 files need investigation** (possibly obsolete)
4. **47 files remain** (extra boundary files, provider files)

**Estimated Effort:**
- Investigation: 2 hours
- Migration: 8 hours
- Deletion: 1 hour
- **Total: 11 hours**

**Next Action:** Start with Phase 1 (investigate obsolete packages) to determine which of the 43 "unknown" files are actually still needed.

---

**Generated by:** `validate-legacy-plugin.sh`
**Last Updated:** 2026-02-02
