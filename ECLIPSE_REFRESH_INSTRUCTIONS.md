# Eclipse PDE Errors - Refresh Instructions

**Problem:** Eclipse showing PDE errors even though MANIFEST.MF files are correct:
```
No available bundle exports package 'dev.langchain4j.rag.content'
No available bundle exports package 'dev.langchain4j.rag.query'
Unsatisfied constraint: 'Import-Package: dev.langchain4j.rag.content.retriever'
```

**Root Cause:** Eclipse hasn't picked up the updated `com.cloudempiere.ai.deps/META-INF/MANIFEST.MF` file which now exports the RAG packages.

---

## Solution: Refresh Eclipse Workspace

Follow these steps **IN ORDER**:

### 1. Refresh All Projects

**In Package Explorer:**
1. Select all com.cloudempiere.ai.* projects (Ctrl+A or Cmd+A)
2. Press **F5** (or Right-click → Refresh)
3. Wait for refresh to complete

### 2. Clean All Projects

**Menu:** Project → Clean...
1. Select "Clean all projects"
2. Check "Start a build immediately"
3. Click OK
4. Wait for build to complete (watch progress bar in bottom-right)

### 3. Verify deps Bundle MANIFEST.MF

**Check that RAG packages are exported:**

Open: `com.cloudempiere.ai.deps/META-INF/MANIFEST.MF`

**Verify these lines exist** (around line 84-91):
```manifest
dev.langchain4j.rag;version="0.35.0",
dev.langchain4j.rag.content;version="0.35.0",
dev.langchain4j.rag.content.aggregator;version="0.35.0",
dev.langchain4j.rag.content.injector;version="0.35.0",
dev.langchain4j.rag.content.retriever;version="0.35.0",
dev.langchain4j.rag.query;version="0.35.0",
dev.langchain4j.rag.query.router;version="0.35.0",
dev.langchain4j.rag.query.transformer;version="0.35.0",
```

If these are **missing**, run:
```bash
git pull
```

Then repeat steps 1-2.

### 4. Verify Core Bundle MANIFEST.MF

Open: `com.cloudempiere.ai.core/META-INF/MANIFEST.MF`

**Verify these lines exist** (around line 38-40):
```manifest
dev.langchain4j.rag.content;version="0.35.0",
dev.langchain4j.rag.content.retriever;version="0.35.0",
dev.langchain4j.rag.query;version="0.35.0",
```

### 5. Check for Red X Markers

**In Problems view:**
1. Window → Show View → Problems (if not already open)
2. Filter by "cloudempiere.ai"
3. **Expected:** No PDE errors about RAG packages

If errors persist:

**Option A: Close/Reopen Project**
1. Right-click `com.cloudempiere.ai.deps` → Close Project
2. Wait 5 seconds
3. Right-click → Open Project

**Option B: Restart Eclipse**
1. File → Restart
2. After restart, repeat steps 1-2

---

## Verification

Once Eclipse PDE errors are resolved:

### Check 1: No MANIFEST.MF Errors

Open `com.cloudempiere.ai.core/META-INF/MANIFEST.MF`

**Expected:**
- No red underlines
- No yellow warnings about "No available bundle exports package"

### Check 2: Problems View Clean

**Expected Problems count for AI plugins:** 0 PDE errors

### Check 3: Dependencies Tab

1. Open `com.cloudempiere.ai.core/META-INF/MANIFEST.MF`
2. Click **Dependencies** tab
3. Find "dev.langchain4j.rag.content" in Imported Packages
4. Should show: **Resolved** with green checkmark

---

## Why This Happens

**Eclipse PDE caches bundle metadata** from MANIFEST.MF files.

When you:
- Edit MANIFEST.MF manually
- Pull changes from git
- Switch branches

Eclipse may not automatically detect the changes until you:
- Refresh (F5)
- Clean
- Restart

This is normal Eclipse behavior, not a project issue.

---

## After Fixing Eclipse Errors

Once Eclipse PDE is clean, proceed with testing:

1. ✅ Eclipse PDE errors resolved
2. ⏳ **Test startup performance** (with timing logs)
3. ⏳ Check [STARTUP TIMING] logs in console
4. ⏳ Identify which service takes longest to activate
5. ⏳ Report findings

---

**Next:** After Eclipse refresh, test server startup and check timing logs.
