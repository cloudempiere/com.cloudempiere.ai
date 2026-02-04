# Eclipse Synchronization After OSGi Fixes

## Issue
Eclipse build path errors after removing slf4j and cleaning up domain bundles.

## Root Cause
Eclipse caches build state. After removing:
- slf4j JARs from deps bundle
- Dependencies sections from domain bundles
- Maven configuration changes

Eclipse needs to resync its internal state with the new configuration.

## Fix Steps

### Step 1: Update Maven Projects
This syncs Eclipse's Maven cache with pom.xml changes.

**In Eclipse:**
1. Select ALL com.cloudempiere.ai.* projects in Project Explorer
   - com.cloudempiere.ai.deps
   - com.cloudempiere.ai.core
   - com.cloudempiere.ai.sales
   - com.cloudempiere.ai.inventory
   - com.cloudempiere.ai.purchasing
   - com.cloudempiere.ai.support
   - com.cloudempiere.ai.kb
   - com.cloudempiere.ai.orchestrator (if exists)
   - com.cloudempiere.ai.test

2. Right-click → Maven → Update Project...

3. In the dialog:
   - ✅ Check "Force Update of Snapshots/Releases"
   - ✅ Check "Update project configuration from pom.xml"
   - ✅ Check "Clean projects"
   - Click OK

**Expected:** Maven will resync, removing references to deleted slf4j JARs.

---

### Step 2: Refresh All Projects
After Maven update, refresh to pick up file system changes.

**In Eclipse:**
1. Select all com.cloudempiere.ai.* projects
2. Press F5 (or right-click → Refresh)

---

### Step 3: Clean All Projects
This removes stale compiled classes and forces rebuild.

**In Eclipse:**
1. Project menu → Clean...
2. Select "Clean all projects"
3. ✅ Check "Start a build immediately"
4. Select "Build the entire workspace"
5. Click Clean

**Expected:** All projects rebuild from scratch.

---

### Step 4: Verify No Errors

**Check Problems View:**
- Window → Show View → Problems
- Filter: Show "Errors" only
- Expected: 0 errors

**If still errors:**
- Close Eclipse
- Delete .metadata/.plugins/org.eclipse.core.resources/.projects/com.cloudempiere.ai.*/.markers
- Restart Eclipse
- Repeat steps 1-3

---

## Expected Outcome

After these steps:
- ✅ deps bundle builds (no slf4j references)
- ✅ core bundle builds
- ✅ All 5 domain bundles build
- ✅ All dependent projects build
- ✅ 0 build path errors
- ✅ Maven configuration in sync

---

## Why slf4j Errors?

We deliberately removed slf4j JARs from deps bundle (commit: "fix(osgi): remove slf4j from deps bundle").

**Reason:** Uses constraint violation - deps exported org.slf4j 2.0.9, but platform provides slf4j.api 1.7.30. This blocked other plugins (bomconfigurator) from loading.

**Solution:** AI plugins now use platform's slf4j.api (1.7.30) via Import-Package.

Eclipse's build path still references the deleted JARs until Maven → Update Project syncs the configuration.

---

## Verification Command

After fixing Eclipse errors, verify OSGi resolution:
```bash
cd /Users/developer/GitHub/com.cloudempiere.ai
mvn clean verify
```

**Expected output:**
```
[INFO] com.cloudempiere.ai.deps ........................... SUCCESS
[INFO] com.cloudempiere.ai.core ........................... SUCCESS
[INFO] com.cloudempiere.ai.sales .......................... SUCCESS
[INFO] com.cloudempiere.ai.inventory ...................... SUCCESS
[INFO] com.cloudempiere.ai.purchasing ..................... SUCCESS
[INFO] com.cloudempiere.ai.support ........................ SUCCESS
[INFO] com.cloudempiere.ai.kb ............................. SUCCESS
[INFO] BUILD SUCCESS
```
