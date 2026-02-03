# Legacy Plugin Deprecation Strategy

**Status:** 🚨 SAFE DEPRECATION (No Deletion)
**Approach:** Keep legacy plugin, mark as deprecated, migrate incrementally
**Target:** Complete migration by v1.1.0 (Q2 2026)

---

## Philosophy

**DON'T DELETE, DEPRECATE** until 100% validated that everything is migrated.

This approach ensures:
- ✅ No functionality is lost during migration
- ✅ Rollback is trivial (just keep using legacy plugin)
- ✅ Can run both legacy and new plugins side-by-side
- ✅ Gradual migration with validation at each step
- ✅ Production systems remain stable

---

## Current State (2026-02-02)

### Validation Results

```bash
./validate-legacy-plugin.sh
```

**Summary:**
- 🟡 8 packages DUPLICATED (exist in both legacy and core)
- 🔴 ~7 packages LEGACY ONLY (need migration)
- 🟢 1 package MIGRATED (orchestrator only in new)

### Duplicated Packages (Both Locations)

| Package | Legacy Files | Core Files | Status |
|---------|--------------|------------|--------|
| provider/ | 29 | 27 | ⚠️ Legacy has 2 extra files |
| database/ | 3 | 3 | ✅ Identical |
| context/ | 6 | 6 | ✅ Identical |
| component/ | 2 | 2 | ✅ Identical |
| util/ | 16 | 17 | ⚠️ Core has 1 extra file |
| model/ | 20 | 20 | ✅ Identical |
| process/ | 1 | 1 | ✅ Identical |
| boundary/ | 7 | 3 | ⚠️ Legacy has 4 extra files |

**Action:** These can be safely deleted from legacy plugin AFTER verifying no differences in functionality.

### Legacy-Only Packages (Need Migration)

| Package | Files | Priority | Target Location |
|---------|-------|----------|-----------------|
| rag/ | ~15 | 🔴 P0 | core/rag/ |
| guardrails/ | ~4 | 🟡 P1 | core/guardrails/ |
| observability/ | ~6 | 🟡 P1 | core/observability/ |
| kb/ | ~12 | 🟡 P1 | kb/kb/ OR core/kb/ |
| health/ | ~2 | 🟢 P2 | core/health/ |
| error/ | ~3 | 🟢 P2 | core/error/ |
| event/ | ~2 | 🟢 P2 | core/event/ |
| routing/ | ~4 | ❓ | Check if obsolete |
| service/ | ~8 | ❓ | Check if obsolete |
| tool/ | ~6 | ❓ | Check if obsolete |
| factory/ | ~3 | ❓ | Check if obsolete |
| function/ | ~2 | ❓ | Check if obsolete |

---

## Deprecation Steps

### ✅ Step 1: Mark as Deprecated (COMPLETE)

- [x] Created DEPRECATED.md in legacy plugin
- [x] Updated MANIFEST.MF Bundle-Name to indicate deprecation
- [x] Created validation scripts

### 🔄 Step 2: Validate Duplicates (IN PROGRESS)

Run comparison script:

```bash
./validate-legacy-plugin.sh > validation-report.txt
```

For each duplicated package, verify:
1. Are files identical? `diff -r legacy/provider core/provider`
2. Any differences in functionality?
3. Which version is more recent?
4. Are there any extra files in legacy?

### 🔄 Step 3: Migrate Unique Packages (NEXT)

**Priority Order:**

#### P0: RAG Infrastructure (2 hours)

```bash
# Create directory in core
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/{dto,embedding,ingest}

# Copy files
cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/rag/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/rag/

# Update MANIFEST.MF exports
# Add: com.cloudempiere.ai.rag;version="1.0.0"

# Test
mvn clean install -DskipTests
```

#### P1: Guardrails (1 hour)

```bash
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/dto

cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/guardrails/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/guardrails/

# Update MANIFEST.MF exports
```

#### P1: Observability (1 hour)

```bash
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/dto

cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/observability/* \
      com.cloudempiere.ai.core/src/com/cloudempiere/ai/observability/

# Update MANIFEST.MF exports
```

#### P1: KB Infrastructure (2 hours)

**Decision needed:** Move to `com.cloudempiere.ai.kb` plugin OR `core/kb`?

**Recommendation:** Move to KB plugin (domain-specific)

```bash
mkdir -p com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/{analysis,database,dto,parser}

cp -r com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/kb/* \
      com.cloudempiere.ai.kb/src/com/cloudempiere/ai/kb/

# Update kb plugin MANIFEST.MF
```

### 🔄 Step 4: Verify No Runtime Usage (BEFORE DELETION)

**CRITICAL:** Before deleting ANY code from legacy plugin:

```bash
# 1. Run runtime tracker
./track-legacy-usage.sh

# 2. Check OSGi console
telnet localhost 12612
osgi> ss | grep cloudempiere.ai
# Should show both legacy and new bundles

osgi> services | grep cloudempiere
# Check which bundle provides which services

# 3. Run full test suite
./run-unit-tests.sh
./run-unit-tests.sh --integration

# 4. Test chat panel manually
# - Open iDempiere
# - Open chat panel
# - Send queries to all 5 domains
# - Verify responses work correctly
```

### 🔄 Step 5: Gradual Deletion (ONLY AFTER VALIDATION)

Once migration is verified, delete in this order:

#### Phase A: Delete Exact Duplicates (Safe)

```bash
# Only delete packages where diff shows no differences
cd com.cloudempiere.ai.plugin/src/com/cloudempiere/ai

# Verify identical first
diff -r database ../../com.cloudempiere.ai.core/src/com/cloudempiere/ai/database
# If identical, delete
rm -rf database

# Repeat for each verified identical package
```

#### Phase B: Delete After Migration (Careful)

```bash
# Only after RAG migrated to core AND validated
rm -rf rag

# Only after guardrails migrated to core AND validated
rm -rf guardrails

# Etc.
```

#### Phase C: Investigate Obsolete (Research First)

```bash
# Don't delete until you understand what these do
# routing/ - Is this replaced by OrchestratorAgent?
# service/ - Is this replaced by facade layer?
# tool/ - Is this replaced by domain tools?

# For each, check:
grep -r "import.*routing" com.cloudempiere.ai.*/src
# If zero results → likely safe to delete
# If results found → need to migrate or replace
```

### 🔄 Step 6: Update Build Configuration

Only after ALL code migrated:

```xml
<!-- ROOT pom.xml -->
<modules>
    <!-- Remove this line ONLY when legacy plugin is empty -->
    <!-- <module>com.cloudempiere.ai.plugin</module> -->

    <module>com.cloudempiere.ai.core</module>
    <module>com.cloudempiere.ai.sales</module>
    <!-- ... -->
</modules>
```

### 🔄 Step 7: Archive Legacy Plugin

Instead of deleting, move to archive:

```bash
# Create archive directory
mkdir -p archive/

# Move legacy plugin to archive
mv com.cloudempiere.ai.plugin archive/com.cloudempiere.ai.plugin.ARCHIVED

# Keep for reference but exclude from build
git add archive/
git commit -m "chore: archive legacy plugin after complete migration"
```

---

## Validation Checklist

Before declaring legacy plugin obsolete, verify:

### Build Validation
- [ ] All new plugins build successfully: `mvn clean install`
- [ ] Legacy plugin excluded from build
- [ ] No compilation errors
- [ ] No unresolved OSGi dependencies

### Runtime Validation
- [ ] All 7 new bundles ACTIVE in OSGi console
- [ ] Legacy bundle NOT installed
- [ ] All domain agents registered as services
- [ ] Orchestrator service available
- [ ] Provider factory working

### Functional Validation
- [ ] Chat panel opens and displays correctly
- [ ] Streaming rendering works
- [ ] All 5 domain agents respond to queries:
  - [ ] SalesAgent (query: "show sales orders")
  - [ ] InventoryAgent (query: "show product stock")
  - [ ] PurchasingAgent (query: "show purchase orders")
  - [ ] SupportAgent (query: "show support tickets")
  - [ ] KnowledgeBaseAgent (query: "find documentation")
- [ ] Context extraction works (window context passed to agents)
- [ ] Database queries execute with proper security
- [ ] RAG retrieval works (if applicable)
- [ ] Guardrails enforce limits
- [ ] Observability metrics collected

### Test Validation
- [ ] Unit tests pass: `./run-unit-tests.sh`
- [ ] Integration tests pass: `./run-unit-tests.sh --integration`
- [ ] No ClassNotFoundException errors
- [ ] No NoClassDefFoundError errors
- [ ] Test coverage maintained or improved

### Regression Validation
- [ ] No new bugs introduced
- [ ] Performance not degraded
- [ ] Memory usage not increased
- [ ] All existing features still work

---

## Rollback Plan

If any validation fails:

```bash
# 1. Re-enable legacy plugin in pom.xml
<module>com.cloudempiere.ai.plugin</module>

# 2. Rebuild
mvn clean install

# 3. Deploy legacy plugin alongside new plugins
cp com.cloudempiere.ai.plugin/target/*.jar $IDEMPIERE_HOME/plugins/

# 4. Restart iDempiere
$IDEMPIERE_HOME/idempiere-server.sh restart

# 5. Investigate failure
# Check logs, OSGi console, test results
```

---

## Timeline

| Phase | Duration | Status | Complete By |
|-------|----------|--------|-------------|
| Mark as deprecated | 30min | ✅ Done | 2026-02-02 |
| Validate duplicates | 2h | 🔄 In progress | 2026-02-03 |
| Migrate unique packages | 8h | ⏳ Pending | 2026-02-10 |
| Verify no runtime usage | 2h | ⏳ Pending | 2026-02-11 |
| Delete duplicates | 1h | ⏳ Pending | 2026-02-12 |
| Archive legacy plugin | 30min | ⏳ Pending | 2026-02-13 |
| **Total** | **14h** | **7% complete** | **2026-02-13** |

---

## Success Criteria

Legacy plugin can be archived when:

1. ✅ All unique packages migrated to new plugins
2. ✅ All duplicated packages verified identical
3. ✅ All tests pass without legacy plugin
4. ✅ Runtime validation shows zero legacy usage
5. ✅ Production deployment successful without legacy plugin
6. ✅ No regressions reported after 1 week

---

## Monitoring

Track progress with:

```bash
# Run validation daily
./validate-legacy-plugin.sh > daily-validation-$(date +%Y%m%d).txt

# Compare reports
diff daily-validation-20260202.txt daily-validation-20260203.txt

# Should show decreasing duplicate count
# Should show increasing migration count
```

---

## Communication

**For Developers:**
- ⚠️ Don't import from `com.cloudempiere.ai.plugin`
- ✅ Use new plugins: `com.cloudempiere.ai.core`, domain plugins
- 📋 Check DEPRECATED.md for migration status

**For Deployments:**
- ⚠️ Legacy plugin still functional but deprecated
- ✅ New plugins can be deployed alongside legacy
- 🔄 Gradual migration path available
- 📅 Legacy removal target: v1.1.0 (Q2 2026)

---

**Next Action:** Run `./validate-legacy-plugin.sh` to generate current state report
