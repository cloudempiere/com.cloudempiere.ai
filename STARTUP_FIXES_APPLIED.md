# OSGi Startup Fixes Applied

**Date:** 2026-02-02
**Status:** ✅ **3 of 4 CRITICAL ISSUES FIXED**
**Remaining:** ⚠️ **1 USER ACTION REQUIRED**

---

## Problem Diagnosed

The iDempiere server hung during startup with no OSGi console output after disabling legacy plugin and enabling new plugins.

### Root Cause Analysis (by OSGi Expert + Architecture Expert)

**Why the hang occurred:**
1. OSGi framework attempts to resolve bundle dependencies during startup
2. Core bundle failed to resolve (missing `com.cloudempiere.core.health` import)
3. Domain bundles blocked because they require core bundle
4. Even if resolved, orchestrator wouldn't find agents (wrong service interface)
5. OSGi resolver hung in unresolvable state
6. No bundle activation = no logs

---

## ✅ Fixes Applied (Automatically)

### Fix 1: Removed Missing Health Package Import

**File:** `com.cloudempiere.ai.core/META-INF/MANIFEST.MF`

**Change:**
```diff
Import-Package: com.anthropic.client;version="2.10.0",
 com.anthropic.core;version="2.10.0",
 com.anthropic.models;version="2.10.0",
- com.cloudempiere.core.health,
 com.fasterxml.jackson.annotation;version="2.17.0",
```

**Reason:** Package `com.cloudempiere.core.health` doesn't exist in workspace or iDempiere platform.

**Impact:** Core bundle can now resolve.

---

### Fix 2: Disabled Health Check Files (Temporary)

**Files:**
- `AIHealthCheckRegistrar.java` → `AIHealthCheckRegistrar.java.disabled`
- `AIPluginHealthService.java` → `AIPluginHealthService.java.disabled`

**Reason:** These files reference the non-existent health package.

**Future:** Will be re-enabled when health check infrastructure is added or integrated with iDempiere's health system.

---

### Fix 3: Fixed Domain Agent Service Declarations

**Problem:** Orchestrator expects `IDomainAgent` interface, but agent XML files only provided concrete class.

**Files Fixed:**
- ✅ `com.cloudempiere.ai.sales/OSGI-INF/com.cloudempiere.ai.sales.agent.SalesAgent.xml`
- ✅ `com.cloudempiere.ai.inventory/OSGI-INF/com.cloudempiere.ai.inventory.agent.InventoryAgent.xml`
- ✅ `com.cloudempiere.ai.purchasing/OSGI-INF/com.cloudempiere.ai.purchasing.agent.PurchasingAgent.xml`
- ✅ `com.cloudempiere.ai.support/OSGI-INF/com.cloudempiere.ai.support.agent.SupportAgent.xml`
- ✅ `com.cloudempiere.ai.kb/OSGI-INF/com.cloudempiere.ai.kb.agent.KbAgent.xml`

**Change Applied to Each:**
```xml
<service>
   <provide interface="com.cloudempiere.ai.sales.agent.SalesAgent"/>
   <provide interface="com.cloudempiere.ai.boundary.IDomainAgent"/>  <!-- ADDED -->
</service>
```

**Reason:** Orchestrator uses:
```xml
<reference
    cardinality="0..n"
    interface="com.cloudempiere.ai.boundary.IDomainAgent"
    .../>
```

Without this, orchestrator starts with ZERO agents.

**Impact:** Orchestrator will now discover all 5 domain agents.

---

## ⚠️ CRITICAL: USER ACTION REQUIRED

### Fix 4: Remove Legacy Plugin from Launch Configuration

**Problem:** Both legacy plugin and new core plugin export the same packages (split package violation).

**Affected Packages (exported by BOTH legacy and core):**
- com.cloudempiere.ai.component
- com.cloudempiere.ai.context
- com.cloudempiere.ai.database
- com.cloudempiere.ai.model
- com.cloudempiere.ai.provider.dto
- com.cloudempiere.ai.provider.langchain4j
- com.cloudempiere.ai.provider.langchain4j.tools
- com.cloudempiere.ai.util
- dev.langchain4j.* (multiple packages)

**Impact:** OSGi cannot resolve which bundle provides these packages → resolver hangs.

**YOUR ACTION:**

#### Option A: Eclipse Launch Configuration (Recommended)

1. **Open Eclipse**
2. **Go to:** Run → Run Configurations
3. **Find:** Your server.product launch configuration
4. **Click:** Plug-ins tab
5. **Uncheck:** `com.cloudempiere.ai` (legacy plugin)
6. **Verify checked:**
   - ✅ com.cloudempiere.ai.core
   - ✅ com.cloudempiere.ai.deps
   - ✅ com.cloudempiere.ai.sales
   - ✅ com.cloudempiere.ai.inventory
   - ✅ com.cloudempiere.ai.purchasing
   - ✅ com.cloudempiere.ai.support
   - ✅ com.cloudempiere.ai.kb
7. **IMPORTANT:** If you see `com.cloudempiere.ai.orchestrator` in the list, **UNCHECK IT** (doesn't exist as separate bundle)
8. **Apply** and **Run**

#### Option B: Feature.xml (If Using Features)

If your launch uses feature.xml, edit:
```bash
/Users/developer/GitHub/com.cloudempiere.ai/com.cloudempiere.ai.feature/feature.xml
```

**Remove lines 74-80:**
```xml
<!-- DELETE THIS -->
<plugin
    id="com.cloudempiere.ai"
    download-size="0"
    install-size="0"
    version="0.0.0"
    unpack="false"/>
```

---

## Verification Steps

After removing legacy plugin from launch config:

### 1. Refresh Eclipse Projects
```
Select all projects → F5 (Refresh)
```

### 2. Clean Workspace
```
Project → Clean → Clean all projects → OK
```

### 3. Launch Server
```
Run → Run As → server.product
```

### 4. Check OSGi Console Output

You should now see:
```
osgi> ss com.cloudempiere.ai

Framework is launched.

...
ACTIVE    com.cloudempiere.ai.deps
ACTIVE    com.cloudempiere.ai.core
ACTIVE    com.cloudempiere.ai.sales
ACTIVE    com.cloudempiere.ai.inventory
ACTIVE    com.cloudempiere.ai.purchasing
ACTIVE    com.cloudempiere.ai.support
ACTIVE    com.cloudempiere.ai.kb
```

### 5. Verify Agent Discovery

Check logs for:
```
OrchestratorAgent: Checking 5 available domain agents
```

If you see `Checking 0 available domain agents`, the service declarations weren't applied correctly.

### 6. Test Chat Panel

1. Open iDempiere UI
2. Open chat panel
3. Send test queries:
   - "show sales orders" → SalesAgent should respond
   - "check inventory" → InventoryAgent should respond
   - "find support tickets" → SupportAgent should respond

---

## Troubleshooting

### If Server Still Hangs

**Check 1: Legacy plugin still loaded?**
```bash
# In OSGi console
ss com.cloudempiere.ai

# If you see BOTH:
# com.cloudempiere.ai (legacy)
# com.cloudempiere.ai.core (new)
# → Legacy plugin is still in launch config
```

**Fix:** Go back to launch config and uncheck legacy plugin.

---

**Check 2: Core bundle not resolving?**
```bash
# In OSGi console
diag com.cloudempiere.ai.core

# Look for:
# "Missing Constraint: Import-Package: ..."
```

**Fix:** Check if missing import is still in MANIFEST.MF. Should NOT see `com.cloudempiere.core.health`.

---

**Check 3: Domain agents not found?**
```bash
# Check orchestrator logs
grep "available domain agents" idempiere.log

# Should show: "Checking 5 available domain agents"
# If shows 0, service declarations weren't applied
```

**Fix:** Refresh projects, clean workspace, rebuild.

---

### If Bundles Show INSTALLED (not ACTIVE)

```bash
# In OSGi console
ss com.cloudempiere.ai.sales

# If shows INSTALLED (not ACTIVE)
diag <bundle-id>

# Check what's preventing activation
```

**Common Causes:**
- Missing dependency (check Require-Bundle)
- Unsatisfied import (check Import-Package)
- Service reference not satisfied

---

## What's Different Now

### Before (Not Working):
```
✗ Core imports non-existent health package → won't resolve
✗ Domain agents only provide concrete class → orchestrator can't find them
✗ Legacy plugin loaded → split packages with core
✗ OSGi resolver hangs → no logs, no startup
```

### After (Should Work):
```
✓ Core has no missing imports → will resolve
✓ Domain agents provide IDomainAgent interface → orchestrator finds them
✓ Legacy plugin removed from launch → no split packages
✓ OSGi resolver succeeds → bundles activate, logs appear, startup completes
```

---

## Summary of Changes

| Component | Change | Status |
|-----------|--------|--------|
| Core MANIFEST.MF | Removed `com.cloudempiere.core.health` import | ✅ Fixed |
| AIHealthCheckRegistrar.java | Disabled (renamed .java.disabled) | ✅ Fixed |
| AIPluginHealthService.java | Disabled (renamed .java.disabled) | ✅ Fixed |
| SalesAgent.xml | Added `IDomainAgent` interface | ✅ Fixed |
| InventoryAgent.xml | Added `IDomainAgent` interface | ✅ Fixed |
| PurchasingAgent.xml | Added `IDomainAgent` interface | ✅ Fixed |
| SupportAgent.xml | Added `IDomainAgent` interface | ✅ Fixed |
| KbAgent.xml | Added `IDomainAgent` interface | ✅ Fixed |
| Launch config | Remove legacy plugin | ⚠️ **USER ACTION** |

---

## Expected Result

After removing legacy plugin from launch config and restarting:

✅ **Server starts successfully**
✅ **All 7 new bundles ACTIVE** (core + 5 domains + deps)
✅ **Orchestrator finds 5 domain agents**
✅ **Chat panel works**
✅ **Queries routed to correct agents**
✅ **Streaming responses work**

---

## Next Steps

1. ✅ **Code fixes applied** (this commit)
2. ⏳ **Remove legacy from launch config** (your action)
3. ⏳ **Test startup** (should work now)
4. ⏳ **Test chat panel** (all 5 agents)
5. ⏳ **Run validation checklist** (see MIGRATION_COMPLETE.md)

---

**Commit:** c76145f (startup fixes applied)
**Status:** Ready for testing after legacy plugin removed from launch config
**Experts Consulted:** OSGi Expert (afc6a38) + Architecture Expert (a54edc2)
