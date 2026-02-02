# Eclipse Workspace Refresh Guide

## Problem
Eclipse shows compile errors:
```
Cannot find the class file for org.slf4j.Logger
The type org.slf4j.Logger cannot be resolved
```

## Root Cause
- Deps bundle MANIFEST.MF was updated to export org.slf4j
- Eclipse hasn't reloaded the updated bundle metadata
- Core bundle can't resolve SLF4J at compile time

## Solution: 3-Step Refresh

### Step 1: Refresh Both Bundles
1. In Package Explorer, select **both**:
   - `com.cloudempiere.ai.deps`
   - `com.cloudempiere.ai.core`
2. Right-click → **Refresh** (or press F5)
3. Wait for "Building workspace" to complete

### Step 2: Clean Build
1. Menu: **Project** → **Clean...**
2. Select **Clean all projects**
3. Check **Start a build immediately**
4. Click **Clean**
5. Wait for full rebuild

### Step 3: Verify Resolution
1. Check **Problems** view
2. SLF4J errors should be gone
3. If still present, proceed to Step 4

### Step 4: Nuclear Option (if needed)
If errors persist:

1. **Close Eclipse completely**
2. Delete Eclipse metadata:
   ```bash
   cd /Users/developer/GitHub/com.cloudempiere.ai
   rm -rf .metadata/.plugins/org.eclipse.core.resources/.projects/com.cloudempiere.ai.core
   rm -rf .metadata/.plugins/org.eclipse.core.resources/.projects/com.cloudempiere.ai.deps
   ```
3. **Restart Eclipse**
4. Eclipse will rebuild indexes
5. Clean build again

## Verification Commands

### Maven Build (should work)
```bash
cd com.cloudempiere.ai.deps
mvn clean verify -DskipTests

cd ../com.cloudempiere.ai.core
mvn clean verify -DskipTests
```

### Check deps exports SLF4J
```bash
grep "org.slf4j" com.cloudempiere.ai.deps/META-INF/MANIFEST.MF
```
Should show:
```
 org.slf4j;version="1.7.36",
 org.slf4j.spi;version="1.7.36",
 org.slf4j.helpers;version="1.7.36"
```

### Check core requires deps
```bash
grep "com.cloudempiere.ai.deps" com.cloudempiere.ai.core/META-INF/MANIFEST.MF
```
Should show:
```
 com.cloudempiere.ai.deps;bundle-version="0.35.0",
```

## Why This Happens

Eclipse PDE caches bundle metadata (exports, imports, versions). When you:
1. Update MANIFEST.MF
2. Rebuild via Maven

Eclipse doesn't automatically reload the metadata until you **Refresh**.

## Alternative: Command-Line Build

If Eclipse is still stubborn, build via Maven:
```bash
cd /Users/developer/GitHub/com.cloudempiere.ai
mvn clean install -DskipTests
```

Then deploy the built JARs to iDempiere manually.
