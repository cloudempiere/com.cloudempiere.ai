# Cloudempiere AI - Legacy Monolithic Plugin

**Bundle ID:** `com.cloudempiere.ai`
**Version:** 10.0.2-SNAPSHOT
**Type:** OSGi Bundle (Legacy)
**Status:** ⚠️ **Being Phased Out** - Migrating to multi-plugin architecture

## ⚠️ Migration Notice

This is the **original monolithic plugin** that is being replaced by the multi-plugin architecture:

```
OLD (Monolithic):                    NEW (Multi-Plugin):
┌────────────────────────────┐      ┌────────────────────────────┐
│ com.cloudempiere.ai        │      │ com.cloudempiere.ai.deps   │
│ (Everything in one plugin) │  →   │ com.cloudempiere.ai.core   │
│                            │      │ com.cloudempiere.ai.sales  │
│                            │      │ com.cloudempiere.ai.inv... │
│                            │      │ com.cloudempiere.ai.pur... │
└────────────────────────────┘      └────────────────────────────┘
```

**Migration Timeline:**
- ✅ **Phase 2 Week 3:** Created `com.cloudempiere.ai.deps` (shared dependencies)
- ✅ **Phase 2 Week 4:** Created `com.cloudempiere.ai.core` (core infrastructure)
- 🔄 **Phase 2 Week 5:** Create domain plugins (sales, inventory, purchasing, support, kb)
- 🔄 **Phase 2 Week 6:** Migrate remaining code from this plugin to domain plugins
- ⏳ **Phase 2 Week 7:** Deprecate and remove this plugin

## Current Contents

### Still in Monolithic Plugin (Not Yet Migrated)

**UI Components:**
- Forms and windows not yet migrated
- Legacy callouts
- Legacy processes

**Utilities:**
- `MarkdownSyntaxSanitizer` (migrated to core)
- `StreamingMarkdownRenderer` (migrated to core)
- `ZoomLinkProcessor` (migrated to core)

### Already Migrated to Core Plugin

The following have been **moved to `com.cloudempiere.ai.core`**:
- ✅ Provider framework (`IAIProvider`, `AIProviderFactory`)
- ✅ LangChain4j integration (`LangChain4jProviderFactory`)
- ✅ Database security (`SecureDatabaseQueryExecutor`)
- ✅ Context providers (`WindowContextProvider`, `ChartContextProvider`)
- ✅ Domain boundaries (`DomainBoundary` base class)
- ✅ Data models (`MAIProvider`, `MAIChat`, `MAIUsageMetrics`)
- ✅ Utilities (`StreamingMarkdownRenderer`, `MarkdownTableRenderer`)

### Already Migrated to Deps Plugin

The following have been **moved to `com.cloudempiere.ai.deps`**:
- ✅ All 55 third-party JARs (LangChain4j, AWS SDK, Jackson, etc.)

## Why Migration is Happening

### Problems with Monolithic Approach

1. **JAR Duplication Risk**
   - If we created domain plugins without deps plugin, each would embed 55 JARs
   - 5 domain plugins × 55 JARs = 275 JARs (vs 55 with shared deps)

2. **No Domain Boundaries**
   - Sales code can access inventory tables
   - Inventory code can access financial tables
   - Security risk: AI agents could leak sensitive data

3. **Tight Coupling**
   - All features in one plugin
   - Bug fix in sales requires testing entire plugin
   - Cannot deploy domain-specific hotfixes

4. **Team Conflicts**
   - Multiple developers editing same plugin
   - Merge conflicts in large files
   - Unclear ownership

### Benefits of Multi-Plugin Architecture

1. **Dependency Management**
   - 55 JARs stored once in `com.cloudempiere.ai.deps`
   - Domain plugins import packages, no embedding
   - Consistent library versions across all plugins

2. **Security Boundaries**
   - Each domain plugin extends `DomainBoundary`
   - Sales plugin cannot query warehouse tables
   - Enforced at runtime by `validateReadTable()`

3. **Independent Deployment**
   - Bug fix in sales plugin doesn't affect inventory
   - Hotfix deployed to single domain
   - Faster QA cycles

4. **Clear Ownership**
   - Sales team owns `com.cloudempiere.ai.sales`
   - Inventory team owns `com.cloudempiere.ai.inventory`
   - Reduced merge conflicts

## Building

```bash
cd com.cloudempiere.ai.plugin
mvn clean install
```

**⚠️ Deprecation Warning:** This plugin still builds and works, but new features should be added to:
- Core infrastructure → `com.cloudempiere.ai.core`
- Domain features → Domain-specific plugins (Week 5)

## Dependencies

**Required:**
- `com.cloudempiere.ai.deps` (for shared libraries)
- `com.cloudempiere.ai.core` (for provider interfaces)
- iDempiere v10 (Java 11)

## Usage

### Current (Deprecated)
```java
// Old way - accessing monolithic plugin
import com.cloudempiere.ai.provider.IAIProvider;  // ❌ Old location
```

### New (Recommended)
```java
// New way - accessing core plugin
import com.cloudempiere.ai.provider.IAIProvider;  // ✅ From core plugin
import com.cloudempiere.ai.boundary.DomainBoundary;
```

**Package paths are the same**, but now resolved from `com.cloudempiere.ai.core` instead of `com.cloudempiere.ai`.

## Migration Plan for Remaining Code

### Week 5: Extract Domain Features

**Sales Domain:**
- Extract sales-related processes
- Extract sales-related callouts
- Move to `com.cloudempiere.ai.sales`

**Inventory Domain:**
- Extract inventory processes
- Move to `com.cloudempiere.ai.inventory`

**Purchasing Domain:**
- Extract purchasing processes
- Move to `com.cloudempiere.ai.purchasing`

**Support Domain:**
- Extract support processes
- Move to `com.cloudempiere.ai.support`

**Knowledge Base Domain:**
- Extract KB features
- Move to `com.cloudempiere.ai.kb`

### Week 6: Deprecation

1. Mark plugin as deprecated in MANIFEST.MF
2. Add deprecation warnings to remaining classes
3. Update documentation to point to new plugins
4. Create migration guide for custom extensions

### Week 7: Removal

1. Remove deprecated plugin from feature
2. Remove from P2 repository
3. Update installation documentation
4. Archive legacy code for reference

## For Plugin Developers

### If You Extended This Plugin

**Check if your code depends on:**

1. **Provider interfaces** → Now in `com.cloudempiere.ai.core`
   ```xml
   <!-- Update your MANIFEST.MF -->
   <Import-Package>
     com.cloudempiere.ai.provider;version="0.32.0"
   </Import-Package>
   ```

2. **Third-party libraries** → Now in `com.cloudempiere.ai.deps`
   ```xml
   <Import-Package>
     dev.langchain4j;version="0.35.0"
   </Import-Package>
   ```

3. **Domain-specific features** → Will be in domain plugins (Week 5)
   ```xml
   <Import-Package>
     com.cloudempiere.ai.sales;version="0.32.0"
   </Import-Package>
   ```

### Migration Steps for Your Plugin

1. **Update MANIFEST.MF:**
   - Change imports from `com.cloudempiere.ai` to `com.cloudempiere.ai.core`
   - Add `com.cloudempiere.ai.deps` for library imports
   - Add domain plugin imports as needed

2. **Update pom.xml:**
   ```xml
   <dependency>
     <groupId>com.cloudempiere</groupId>
     <artifactId>com.cloudempiere.ai.core</artifactId>
     <version>0.32.0-SNAPSHOT</version>
   </dependency>
   ```

3. **Test:**
   - Verify OSGi bundle resolution
   - Test provider access
   - Test database queries

## Troubleshooting

### Import-Package Errors
**Symptom:** `Import-Package: com.cloudempiere.ai.provider could not be resolved`

**Solution:**
- Change to `com.cloudempiere.ai.core` (core plugin exports this package)
- Update dependency in pom.xml

### ClassNotFoundException
**Symptom:** `ClassNotFoundException: dev.langchain4j.model.chat.ChatLanguageModel`

**Solution:**
- Add `com.cloudempiere.ai.deps` dependency
- Add Import-Package for `dev.langchain4j.*`

### Missing Provider
**Symptom:** `AIProviderFactory.getProvider(id)` returns null

**Solution:**
- Ensure `com.cloudempiere.ai.core` is installed and started
- Check AIG_Provider configuration record

## References

- **PLUGIN_ARCHITECTURE.md:** Why multi-plugin architecture
- **PHASE_2_WEEK_4_COMPLETION.md:** Week 4 completion report
- **ADR-009:** Domain Boundaries and Agent Scope
- **OSGI_MULTI_PLUGIN_ARCHITECTURE.md:** Multi-plugin design

## Maintainers

Cloudempiere AI Team

**Deprecation Notice:** This plugin will be removed in Phase 2 Week 7 (estimated 2026-02-10).

**Last Updated:** 2026-01-30
