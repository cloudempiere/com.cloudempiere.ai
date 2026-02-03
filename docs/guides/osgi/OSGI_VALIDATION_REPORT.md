# OSGi Validation Report - Cloudempiere AI Plugins

**Date:** 2026-02-02
**Validator:** idempiere-osgi-expert agent (ad407dc)
**Status:** 7 bundles analyzed

---

## Executive Summary

**Overall Health Score:** 72/100

### Bundle Scores
| Bundle | Score | Status | Critical Issues |
|--------|-------|--------|-----------------|
| com.cloudempiere.ai.deps | 85/100 | ✅ Good | 0 |
| com.cloudempiere.ai.core | 88/100 | ✅ Good | 0 |
| com.cloudempiere.ai.sales | 60/100 | ❌ Poor | 3 |
| com.cloudempiere.ai.inventory | 68/100 | ⚠️ Fair | 2 |
| com.cloudempiere.ai.purchasing | 68/100 | ⚠️ Fair | 2 |
| com.cloudempiere.ai.support | 68/100 | ⚠️ Fair | 2 |
| com.cloudempiere.ai.kb | 68/100 | ⚠️ Fair | 2 |

### Priority Fixes Required

#### 🔴 CRITICAL (Must Fix - Blocks Build)

1. **sales bundle - Missing parent POM reference**
   - Will fail Tycho build
   - Prevents proper dependency resolution

2. **All domain bundles - Remove Maven dependencies section**
   - OSGi dependencies should only be in MANIFEST.MF
   - Causes duplicate resolution attempts

3. **All domain bundles - Add version constraint to Require-Bundle**
   - Missing: `Require-Bundle: com.cloudempiere.ai.core`
   - Should be: `Require-Bundle: com.cloudempiere.ai.core;bundle-version="[0.32.0,1.0.0)"`
   - Can cause runtime resolution failures

---

## Detailed Findings

### Bundle 1: com.cloudempiere.ai.deps ✅

**Score:** 85/100

**What's Correct:**
- ✅ Proper library wrapper bundle pattern
- ✅ All 61 JARs listed in Bundle-ClassPath
- ✅ Comprehensive Export-Package with proper versioning
- ✅ Maven dependency plugin correctly configured
- ✅ No Require-Bundle (correct for library wrapper)
- ✅ Version 0.35.0 aligns with LangChain4j (ADR-035 compliant)

**Warnings:**
- ⚠️ Large bundle size (25MB+ with 61 JARs)
- ⚠️ Empty OSGI-INF directory exists (should be removed)
- ⚠️ No singleton:=true (should add)

**Recommendations:**
1. Add `singleton:=true` to Bundle-SymbolicName
2. Remove empty OSGI-INF/ directory
3. Add Bundle-Description and Bundle-License headers

---

### Bundle 2: com.cloudempiere.ai.core ✅

**Score:** 88/100

**What's Correct:**
- ✅ Singleton bundle
- ✅ Proper version ranges for org.adempiere.base [10.0.0,11.0.0)
- ✅ Import-Package from deps with versions
- ✅ Comprehensive Export-Package (21 packages)
- ✅ Service-Component declaration
- ✅ Lazy activation policy
- ✅ Bundle activator declared

**Warnings:**
- ⚠️ Require-Bundle for org.adempiere.plugin.utils has no version constraint
- ⚠️ ZK bundles use exact version (9.6.3) instead of range

**Recommendations:**
1. Add version constraint: `org.adempiere.plugin.utils;bundle-version="[10.0.0,11.0.0)"`
2. Use version ranges for ZK: `zk;bundle-version="[9.6.0,10.0.0)"`
3. Add Bundle-Description header

---

### Bundle 3: com.cloudempiere.ai.sales ❌

**Score:** 60/100

**What's Correct:**
- ✅ Singleton bundle
- ✅ Import-Package with versions
- ✅ Export-Package with versions
- ✅ Service-Component declaration
- ✅ Lazy activation

**Critical Errors:**
- ❌ **NO PARENT POM** - pom.xml missing parent reference
- ❌ **Dependencies in pom.xml** - Should only be in MANIFEST.MF
- ❌ **No version on Require-Bundle** - Must add bundle-version constraint

**Fix Instructions:**

**1. Add parent POM to pom.xml:**
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.cloudempiere.ai</groupId>
        <artifactId>com.cloudempiere.ai.parent</artifactId>
        <version>10.0.2-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>com.cloudempiere.ai.sales</artifactId>
    <version>0.32.0-SNAPSHOT</version>
    <packaging>eclipse-plugin</packaging>
</project>
```

**2. Remove dependencies section from pom.xml**

**3. Add version to MANIFEST.MF:**
```
Require-Bundle: com.cloudempiere.ai.core;bundle-version="[0.32.0,1.0.0)"
```

---

### Bundles 4-7: inventory, purchasing, support, kb ⚠️

**Score:** 68/100 each

**What's Correct:**
- ✅ All have parent POM reference (better than sales)
- ✅ Singleton bundles
- ✅ Import-Package with versions
- ✅ Export-Package with versions
- ✅ Service declarations

**Critical Errors:**
- ❌ **Dependencies in pom.xml** - Should only be in MANIFEST.MF
- ❌ **No version on Require-Bundle**

**Warnings:**
- ⚠️ groupId override (should inherit from parent)
- ⚠️ Tycho plugin redefinition (should inherit)
- ⚠️ jars.compile.order unnecessary

**Fix Instructions (Apply to all 4 bundles):**

**1. Remove from pom.xml:**
```xml
<!-- DELETE THIS ENTIRE SECTION -->
<dependencies>
    <dependency>
        <groupId>com.cloudempiere.ai</groupId>
        <artifactId>com.cloudempiere.ai.core</artifactId>
        <version>0.32.0-SNAPSHOT</version>
    </dependency>
    ...
</dependencies>
```

**2. Remove from pom.xml:**
```xml
<!-- DELETE groupId override -->
<groupId>com.cloudempiere</groupId>
```

**3. Add version to MANIFEST.MF:**
```
Require-Bundle: com.cloudempiere.ai.core;bundle-version="[0.32.0,1.0.0)"
```

**4. Remove from pom.xml (Tycho plugins section):**
```xml
<!-- DELETE - inherit from parent -->
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    ...
</plugin>
```

**5. Remove from build.properties:**
```
jars.compile.order = .
```

---

## Cross-Bundle Analysis

### ✅ Split Package Check: PASSED
No split packages detected. Each bundle exports unique packages.

### ✅ Service Declaration Check: PASSED
All bundles with services have matching OSGI-INF/*.xml files.

### ✅ Bundle-ClassPath Check: PASSED
- deps: 61 JARs properly listed
- All others: No embedded JARs (correct)

### ⚠️ Version Consistency
MANIFEST.MF versions (0.32.0/0.35.0) differ from parent POM (10.0.2).
This is acceptable but should be documented.

### ❌ Require-Bundle Version Constraints: FAILED
All 5 domain bundles missing version constraints on core dependency.

---

## Action Plan

### Phase 1: Fix Critical Issues (BLOCKING)

**File:** `com.cloudempiere.ai.sales/pom.xml`
```xml
<!-- Add parent reference at top -->
<parent>
    <groupId>com.cloudempiere.ai</groupId>
    <artifactId>com.cloudempiere.ai.parent</artifactId>
    <version>10.0.2-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

**Files:** All 5 domain bundle `pom.xml` files
- Remove `<dependencies>` section entirely
- Remove `<groupId>com.cloudempiere</groupId>` override
- Remove Tycho plugin configuration

**Files:** All 5 domain bundle `META-INF/MANIFEST.MF` files
```
Change:
Require-Bundle: com.cloudempiere.ai.core

To:
Require-Bundle: com.cloudempiere.ai.core;bundle-version="[0.32.0,1.0.0)"
```

### Phase 2: Cleanup (Non-Blocking)

**File:** `com.cloudempiere.ai.deps/META-INF/MANIFEST.MF`
```
Change:
Bundle-SymbolicName: com.cloudempiere.ai.deps

To:
Bundle-SymbolicName: com.cloudempiere.ai.deps;singleton:=true
```

**Directory:** Remove `com.cloudempiere.ai.deps/OSGI-INF/`

**Files:** All domain bundle `build.properties`
- Remove `jars.compile.order = .`

**File:** `com.cloudempiere.ai.core/META-INF/MANIFEST.MF`
```
Change:
Require-Bundle: org.adempiere.base;bundle-version="[10.0.0,11.0.0)",
 org.adempiere.plugin.utils,

To:
Require-Bundle: org.adempiere.base;bundle-version="[10.0.0,11.0.0)",
 org.adempiere.plugin.utils;bundle-version="[10.0.0,11.0.0)",
```

### Phase 3: Documentation (Nice to Have)

Add to all MANIFEST.MF files:
```
Bundle-Description: [Description]
Bundle-License: [License URL]
Bundle-Copyright: Cloudempiere
```

---

## Verification Checklist

After applying fixes:

### Build Verification
```bash
cd /Users/developer/GitHub/com.cloudempiere.ai
mvn clean verify
```
**Expected:** All bundles build successfully

### Eclipse Verification
1. Refresh all projects (F5)
2. Clean all projects
3. Check Problems view for 0 errors

### OSGi Runtime Verification
1. Launch server.product
2. Check all bundles ACTIVE:
   ```
   osgi> ss com.cloudempiere.ai
   ```
3. Verify no resolution errors

---

## Best Practices Compliance Summary

| Practice | Compliance |
|----------|-----------|
| No split packages | ✅ 100% |
| Bundle-ClassPath correct | ✅ 100% |
| Export-Package with versions | ✅ 100% |
| Import-Package with versions | ✅ 100% |
| Service-Component when needed | ✅ 100% |
| Singleton for core bundles | ✅ 86% (6/7) |
| Version constraints on Require-Bundle | ❌ 14% (1/7) |
| Parent POM reference | ✅ 86% (6/7) |
| No OSGi deps in pom.xml | ❌ 29% (2/7) |
| Proper Maven inheritance | ❌ 29% (2/7) |

**Overall:** 72/100

---

## Conclusion

The plugin architecture is fundamentally sound with proper separation of concerns and no split package violations. However, **critical Maven configuration issues** in domain bundles must be fixed to ensure reliable builds and deployments.

**Priority:** Fix Phase 1 issues before next release.

**Validated by:** idempiere-osgi-expert agent (ad407dc)
**Date:** 2026-02-02
