# Phase 2 - Week 4: Core Plugin Creation (IN PROGRESS)

**Date:** 2026-01-30
**Status:** 80% COMPLETE

---

## Summary

Created `com.cloudempiere.ai.core` plugin with provider, database, context, component, util, and model packages. Core infrastructure successfully extracted from monolithic plugin.

## Deliverables ✅

### ✅ Plugin Structure Created
```
com.cloudempiere.ai.core/
├── META-INF/
│   └── MANIFEST.MF (with Import/Export-Package)
├── OSGI-INF/ (service configurations copied)
├── build.properties
├── pom.xml
└── src/com/cloudempiere/ai/
    ├── boundary/          # NEW: DomainBoundary.java
    ├── provider/          # Moved from plugin
    ├── database/          # Moved from plugin
    ├── context/           # Moved from plugin
    ├── component/         # Moved from plugin
    ├── util/              # Moved from plugin
    ├── model/             # Moved from plugin
    └── Activator.java     # Copied from plugin
```

### ✅ Core Packages Moved
- **provider/** - AI provider layer (LangChain4j integration)
- **database/** - Secure database query execution
- **context/** - Context extraction layer
- **component/** - ZK UI components (AIChatWidget, AIChatStreamingMessage)
- **util/** - Utilities (MarkdownRenderer, SecuritySanitizer, etc.)
- **model/** - iDempiere model classes (MAIProvider, MAIChatEntry, etc.)

### ✅ Domain Boundary Created
```java
com.cloudempiere.ai.boundary.DomainBoundary.java
- Abstract base class for domain enforcement
- Subclasses register allowed tables
- Prevents cross-domain data access
- Security validation methods
```

### ✅ MANIFEST.MF Configuration
- **Import-Package:** LangChain4j 0.35.0, AWS SDK 2.20.162, Jackson, CommonMark
- **Export-Package:** All core packages with version 0.32.0
- **Service-Component:** OSGI-INF/*.xml

### ✅ Shared Dependencies Updated
- Added 4 additional AWS SDK JARs to deps plugin:
  - regions-2.20.162.jar
  - aws-core-2.20.162.jar
  - sdk-core-2.20.162.jar
  - auth-2.20.162.jar
- **Total JARs in deps:** 51 (was 47)

## Remaining Tasks ❌

### ⏳ Compilation Issues
**Status:** 861 compilation errors (down from 903)

**Issue:** Eclipse PDE "Access restriction" warnings
- Accessing non-API classes/methods from deps plugin
- Need to configure `.settings/org.eclipse.jdt.core.prefs` to relax API restrictions

**Sample Error:**
```
Access restriction: The type 'Embedding' is not API
(restriction on classpath entry 'langchain4j-core-0.35.0.jar')
```

**Solution Options:**
1. **Configure Eclipse settings** - Disable API restrictions (recommended for now)
2. **Use only API classes** - Refactor to avoid internal LangChain4j classes
3. **Export internal packages** - Add more exports to deps MANIFEST.MF

### ⏳ Build Verification
- Full `mvn clean install` not yet successful
- Need to resolve PDE restrictions first

---

## Week 4 Progress: 80%

**Completed:**
- [x] Create plugin structure
- [x] Move core packages from monolithic plugin
- [x] Remove domain-specific code (none found - only RagTools kept)
- [x] Create DomainBoundary.java
- [x] Create MANIFEST.MF
- [x] Create pom.xml
- [x] Update shared deps plugin

**Pending:**
- [ ] Resolve compilation errors (PDE restrictions)
- [ ] Build and verify (`mvn clean install`)
- [ ] Test OSGi bundle resolution

---

## Next Steps

### Option A: Fix Compilation (Quick Win)
1. Create `.settings/org.eclipse.jdt.core.prefs` to relax API restrictions
2. Build core plugin
3. Verify exports are correct

### Option B: Continue to Week 5 (Domain Plugins)
1. Mark core plugin as "in progress"
2. Start creating domain plugins (sales, inventory, etc.)
3. Return to fix core compilation later

---

## Architectural Validation

✅ **Separation of Concerns:**
- Shared dependencies isolated in `com.cloudempiere.ai.deps`
- Core infrastructure in `com.cloudempiere.ai.core`
- Domain boundaries defined for future domain plugins

✅ **Import/Export Contracts:**
- Core imports from deps (LangChain4j, AWS SDK, Jackson)
- Core exports API for domain plugins (boundary, provider, database, util)

✅ **Package Structure:**
- Clean separation between core and future domain code
- RagTools kept in core (infrastructure-level)
- Domain-specific tools will go in domain plugins (Week 5-6)

---

## Files Created This Week

```
com.cloudempiere.ai.core/
├── META-INF/MANIFEST.MF
├── build.properties
├── pom.xml
└── src/com/cloudempiere/ai/boundary/DomainBoundary.java

Updated:
├── com.cloudempiere.ai.deps/META-INF/MANIFEST.MF (added AWS SDK exports)
├── com.cloudempiere.ai.deps/pom.xml (added 4 AWS SDK JARs)
└── pom.xml (added core and deps modules to reactor)
```

---

**Recommendation:** Continue with Option A (fix compilation) to complete Week 4, then proceed to Week 5 domain plugins.

**Phase 2 Progress:** Week 4/10 (40% - core infrastructure)
**Next:** Complete Week 4 compilation, then Week 5 - Domain Plugins
