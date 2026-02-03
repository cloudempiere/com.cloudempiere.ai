# Migration Next Steps

**Date:** 2026-02-02
**Status:** Investigation Complete ✅ | Ready for Migration ⏳

---

## Executive Summary

**Investigation Results:**
- ✅ 42 files confirmed **OBSOLETE** (no migration needed)
- ✅ 3 files marked **FUTURE** (deferred to Phase 3)
- ⏳ 33 files need **MIGRATION** (RAG, guardrails, obs, KB, health, error, event)

**Effort Saved:** 54% reduction in migration scope (from 71 files to 33 files)

**Documents Created:**
1. [OBSOLETE_PACKAGES_INVESTIGATION.md](OBSOLETE_PACKAGES_INVESTIGATION.md) - Investigation results
2. [OBSOLETE_FUNCTIONALITY_MAPPING.md](OBSOLETE_FUNCTIONALITY_MAPPING.md) - Proof of migration
3. [SERVICE_PACKAGE_DECISION.md](SERVICE_PACKAGE_DECISION.md) - service/ decision
4. DEPRECATED.txt markers in 5 obsolete packages
5. FUTURE.txt marker in service/ package

---

## Migration Priority Matrix

### 🔴 P0 - Critical (Must Migrate First)

#### 1. RAG Infrastructure → `core/rag/`

**Files:** 13 files
**Effort:** 2 hours
**Why Critical:** Domain agents need RAG for semantic search and context augmentation

**Files to Migrate:**
```bash
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

**Migration Command:**
```bash
# Create directory in core
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/{dto,embedding,ingest}

# Copy files
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/rag/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/

# Update core/MANIFEST.MF to export packages
# Add: com.cloudempiere.ai.rag;version="1.0.0"
#      com.cloudempiere.ai.rag.dto;version="1.0.0"
#      com.cloudempiere.ai.rag.embedding;version="1.0.0"
#      com.cloudempiere.ai.rag.ingest;version="1.0.0"

# Test
mvn clean install -DskipTests
```

---

### 🟡 P1 - Important (Should Migrate Soon)

#### 2. Guardrails → `core/guardrails/`

**Files:** 4 files
**Effort:** 1 hour
**Why Important:** Safety and compliance (ADR-014)

**Files to Migrate:**
```bash
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/guardrails/
├── InputGuard.java
├── OutputGuard.java
├── ExecutionGuard.java
└── dto/GuardResult.java
```

**Migration Command:**
```bash
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/dto

cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/guardrails/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/

# Update core/MANIFEST.MF
# Add: com.cloudempiere.ai.guardrails;version="1.0.0"
#      com.cloudempiere.ai.guardrails.dto;version="1.0.0"

mvn clean install -DskipTests
```

---

#### 3. Observability → `core/observability/`

**Files:** 3 files
**Effort:** 1 hour
**Why Important:** Cost tracking and monitoring (ADR-013)

**Files to Migrate:**
```bash
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/observability/
├── AIMetricsListener.java
├── CostGuard.java
└── dto/*.java
```

**Migration Command:**
```bash
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/dto

cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/observability/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/

# Update core/MANIFEST.MF
# Add: com.cloudempiere.ai.observability;version="1.0.0"
#      com.cloudempiere.ai.observability.dto;version="1.0.0"

mvn clean install -DskipTests
```

---

#### 4. KB Infrastructure → `com.cloudempiere.ai.kb/src/.../kb/`

⚠️ **IMPORTANT:** Migrates to **KB PLUGIN**, not core!

**Files:** 10 files
**Effort:** 2 hours
**Why Important:** Knowledge base agent needs parsers and analyzers

**Files to Migrate:**
```bash
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

**Migration Command:**
```bash
# Create directory in KB PLUGIN (NOT core!)
mkdir -p com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/{parser,analysis,database,dto}

# Copy files
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/kb/* \
      com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/

# Update kb/MANIFEST.MF
# Add exports for parser, analysis, database, dto packages

mvn clean install -DskipTests
```

---

### 🟢 P2 - Nice to Have (Can Wait)

#### 5. Health Monitoring → `core/health/`

**Files:** 4 files
**Effort:** 30 minutes

```bash
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/health/
├── AIHealthCheckRegistrar.java
├── AIPluginHealthService.java
├── AIUIService.java
└── ...
```

---

#### 6. Error Handling → `core/error/`

**Files:** 1 file
**Effort:** 15 minutes

```bash
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/error/
└── AIErrorHandler.java
```

---

#### 7. Event System → `core/event/`

**Files:** 2 files
**Effort:** 15 minutes

```bash
com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/event/
├── AIBudgetEventHandler.java
└── KEntryEventHandler.java
```

---

## Summary Table

| Package | Priority | Files | Effort | Target Location | Status |
|---------|----------|-------|--------|----------------|--------|
| rag/ | 🔴 P0 | 13 | 2h | core/rag/ | ⏳ Ready |
| guardrails/ | 🟡 P1 | 4 | 1h | core/guardrails/ | ⏳ Ready |
| observability/ | 🟡 P1 | 3 | 1h | core/observability/ | ⏳ Ready |
| kb/ | 🟡 P1 | 10 | 2h | **kb/kb/** ⚠️ | ⏳ Ready |
| health/ | 🟢 P2 | 4 | 30m | core/health/ | ⏳ Ready |
| error/ | 🟢 P2 | 1 | 15m | core/error/ | ⏳ Ready |
| event/ | 🟢 P2 | 2 | 15m | core/event/ | ⏳ Ready |
| **TOTAL** | | **37** | **7h** | | |

---

## Obsolete Packages (No Migration)

| Package | Files | Status | Marker |
|---------|-------|--------|--------|
| agent/ | 9 | ✅ Obsolete | DEPRECATED.txt |
| routing/ | 10 | ✅ Obsolete | DEPRECATED.txt |
| tool/ | 9 | ✅ Obsolete | DEPRECATED.txt |
| factory/ | 1 | ✅ Obsolete | DEPRECATED.txt |
| function/ | 1 | ✅ Obsolete | DEPRECATED.txt |
| service/ | 3 | ⚠️ Future | FUTURE.txt |
| **TOTAL** | **33** | **Marked** | **6 files** |

**Verification:** See [OBSOLETE_FUNCTIONALITY_MAPPING.md](OBSOLETE_FUNCTIONALITY_MAPPING.md) for proof that all functionality has been migrated.

---

## Recommended Execution Plan

### Week 1: Critical Infrastructure (4 hours)

**Day 1: RAG (2 hours)**
```bash
# Migrate RAG infrastructure to core
./scripts/migrate-package.sh rag core

# Update domain agents to import from core
grep -r "import.*rag" com.cloudempiere.ai.*/src | \
  sed 's/plugin/core/' > rag-imports-to-fix.txt

# Fix imports
# Test
mvn clean install && ./run-unit-tests.sh
```

**Day 2: Guardrails + Observability (2 hours)**
```bash
# Migrate guardrails
./scripts/migrate-package.sh guardrails core

# Migrate observability
./scripts/migrate-package.sh observability core

# Test
mvn clean install && ./run-unit-tests.sh
```

---

### Week 2: Domain-Specific + Utilities (3 hours)

**Day 3: KB Infrastructure (2 hours)**
```bash
# Migrate KB to KB PLUGIN (not core!)
./scripts/migrate-package.sh kb kb

# Update KB agent
# Test
mvn clean install && ./run-unit-tests.sh
```

**Day 4: Health, Error, Event (1 hour)**
```bash
# Migrate remaining utilities
./scripts/migrate-package.sh health core
./scripts/migrate-package.sh error core
./scripts/migrate-package.sh event core

# Final test
mvn clean install && ./run-unit-tests.sh --integration
```

---

## Validation Checklist

After each migration step:

### Build Validation
- [ ] `mvn clean install` succeeds
- [ ] No compilation errors
- [ ] All new plugins build successfully
- [ ] OSGi bundle exports correct packages

### Import Validation
- [ ] Search for old imports: `grep -r "import.*plugin" com.cloudempiere.ai.*/src`
- [ ] Update imports to point to new locations (core or domain plugin)
- [ ] No unresolved import errors

### Runtime Validation
- [ ] Deploy to iDempiere
- [ ] Check OSGi console: all bundles ACTIVE
- [ ] Test chat panel: send queries to all 5 domain agents
- [ ] Check logs for ClassNotFoundException

### Test Validation
- [ ] Unit tests pass: `./run-unit-tests.sh`
- [ ] Integration tests pass: `./run-unit-tests.sh --integration`
- [ ] No new test failures

---

## Delete Duplicates (After Migration)

Once migration is complete and validated, delete verified duplicates from legacy plugin:

```bash
cd com.cloudempiere.ai.plugin/src/com/cloudempiere/ai

# Verify identical first
diff -r database ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/database
diff -r context ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/context
diff -r component ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/component
diff -r model ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/model
diff -r process ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/process

# If identical, delete
rm -rf database context component model process

# Total freed: 32 files
```

---

## Success Criteria

Migration is complete when:

1. ✅ All 33 priority files migrated to core/domain plugins
2. ✅ All imports updated to point to new locations
3. ✅ Build succeeds: `mvn clean install`
4. ✅ Tests pass: `./run-unit-tests.sh && ./run-unit-tests.sh --integration`
5. ✅ Runtime validation: chat panel works with all 5 agents
6. ✅ Verified duplicates deleted from legacy plugin
7. ✅ Legacy plugin marked as deprecated (Bundle-Name updated)

**Final State:**
- Legacy plugin: ~40 obsolete files (marked DEPRECATED/FUTURE)
- Core plugin: Complete infrastructure (RAG, guardrails, obs, health, error, event)
- Domain plugins: Domain-specific implementations (agents, tools, KB infrastructure)

---

## Rollback Plan

If any migration fails:

1. Keep legacy plugin in build (it's still functional)
2. Revert MANIFEST.MF changes in core/domain plugins
3. Investigate failure
4. Fix and retry

The safe deprecation approach ensures we can always fall back to the legacy plugin if needed.

---

## Timeline

| Week | Phase | Hours | Status |
|------|-------|-------|--------|
| Week 1 | P0: RAG | 2h | ⏳ Ready |
| Week 1 | P1: Guardrails + Obs | 2h | ⏳ Ready |
| Week 2 | P1: KB | 2h | ⏳ Ready |
| Week 2 | P2: Health, Error, Event | 1h | ⏳ Ready |
| **Total** | **Migration** | **7h** | **⏳ Ready to Start** |

---

**Next Action:** Start with P0 (RAG) migration - see migration commands above.

**Questions?** Check these documents:
- [OBSOLETE_PACKAGES_INVESTIGATION.md](OBSOLETE_PACKAGES_INVESTIGATION.md) - Investigation results
- [OBSOLETE_FUNCTIONALITY_MAPPING.md](OBSOLETE_FUNCTIONALITY_MAPPING.md) - Functionality verification
- [LEGACY_VALIDATION_REPORT.md](LEGACY_VALIDATION_REPORT.md) - Initial validation
- [LEGACY_DEPRECATION_STRATEGY.md](LEGACY_DEPRECATION_STRATEGY.md) - Overall strategy
- [SERVICE_PACKAGE_DECISION.md](SERVICE_PACKAGE_DECISION.md) - service/ decision
