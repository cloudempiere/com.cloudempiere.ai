# Service Package Decision

**Date:** 2026-02-02
**Status:** ✅ DECISION MADE - Leave for Future

---

## Investigation Results

### Files in service/ Package (3 files)

1. **IChatAccessService.java** - Interface for chat ownership and access control
2. **ChatAccessService.java** - Implementation of chat access control
3. **LanguageDetectionService.java** - Language detection for multi-language responses

### Usage Analysis

#### ChatAccessService Status: 🔴 NOT IMPLEMENTED

**Evidence:**
```java
// com.cloudempiere.ai.core/src/com/cloudempiere/ai/model/MAIChat.java:24-26
// TEMPORARILY DISABLED - ChatAccessService will be implemented in future phase (ADR-036)
// import com.cloudempiere.ai.service.ChatAccessService;
// import com.cloudempiere.ai.service.IChatAccessService.ChatAccess;
```

**Files with commented imports:**
- `MAIChat.java` - Main chat model (lines 24-26)
- `AIChatWidget.java.disabled` - Old chat widget (entire file disabled)

**Status:** Planned for ADR-036 (Chat Ownership and Sharing Model), deferred to Phase 3 (Facade Layer)

---

#### LanguageDetectionService Status: 🔴 NOT IMPLEMENTED

**Evidence:**
```java
// com.cloudempiere.ai.core/src/com/cloudempiere/ai/component/AIChatStreamingMessage.java
// TEMPORARILY DISABLED - LanguageDetectionService will be implemented in future phase
// private com.cloudempiere.ai.service.LanguageDetectionService languageService;
```

**Files with commented imports:**
- `AIChatStreamingMessage.java` - Streaming message component (commented)
- `AIService.java.disabled` - Old AI service (entire file disabled)

**Status:** Planned for ADR-037 (Language Detection and Session Language Management), deferred to Phase 3

---

#### MAIChatOwnership Status: ✅ ALREADY MIGRATED

**Location:** This is NOT part of the service/ package - it's a model class in model/ package

**Files:**
- `I_AIG_ChatOwnership.java` - Interface (in BOTH legacy and core)
- `X_AIG_ChatOwnership.java` - Base model (in BOTH legacy and core)
- `MAIChatOwnership.java` - Business logic model (in BOTH legacy and core)

**Status:** Already identified as DUPLICATE in model/ package (will be deleted from legacy with other model duplicates)

---

## Decision: Leave for Future (Option B)

### Rationale

1. **Not Actively Used**: All imports are commented out with explicit "TEMPORARILY DISABLED" comments
2. **ADR References**: Both services reference specific ADRs for future implementation:
   - ADR-036: Chat Ownership and Sharing Model
   - ADR-037: Language Detection and Session Language Management
3. **Phase 3 Feature**: Both are planned for Phase 3 (Facade Layer), not MVP
4. **No Functionality Loss**: The new architecture doesn't use these services - they were never activated

### Action

Create `FUTURE.txt` marker in legacy plugin service/ directory:

```bash
cat > com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/service/FUTURE.txt << 'EOF'
⚠️  FUTURE IMPLEMENTATION

This package contains service layer components planned for Phase 3 (Facade Layer):

- **IChatAccessService.java** / **ChatAccessService.java**
  → Chat ownership and access control (ADR-036)
  → Will be migrated when implementing chat sharing features

- **LanguageDetectionService.java**
  → Multi-language support (ADR-037)
  → Will be migrated when implementing language detection

**Status:** Not yet implemented in new architecture
**Migration:** Phase 3 (Facade Layer)
**References:** ADR-036, ADR-037

Current imports in core files are commented out with "TEMPORARILY DISABLED" notices.
EOF
```

---

## Updated Migration Summary

### Obsolete Packages (No Migration Needed)

| Package | Files | Status | Reason |
|---------|-------|--------|--------|
| agent/ | 9 | ✅ OBSOLETE | Replaced by IDomainAgent interface + domain agents |
| routing/ | 10 | ✅ OBSOLETE | Replaced by OrchestratorAgent |
| tool/ | 9 | ✅ OBSOLETE | Replaced by domain-specific tools with @Tool annotations |
| factory/ | 1 | ✅ OBSOLETE | Replaced by ChatPanel component |
| function/ | 1 | ✅ OBSOLETE | Replaced by LangChain4j native function calling |
| **service/** | **3** | **⚠️ FUTURE** | **Deferred to Phase 3 - Not yet implemented** |

**Total Obsolete:** 33 files (42 files marked obsolete + service/ deferred)

---

## Impact on Effort Estimate

### Before Investigation
- Need Migration: 71 files (RAG, guardrails, obs, KB, health, error, event + 6 "unknown" packages)
- Investigation: 2 hours
- Migration: 8 hours
- **Total: 10 hours**

### After Investigation
- Need Migration: 33 files (RAG, guardrails, obs, KB, health, error, event)
- Obsolete: 30 files (no migration needed)
- Future: 3 files (deferred to Phase 3)
- Investigation: ✅ COMPLETE
- Migration: 6 hours
- **Total: 6 hours** (40% reduction!)

---

## Next Steps

1. ✅ Mark service/ as FUTURE with FUTURE.txt file
2. ✅ Mark obsolete packages with DEPRECATED.txt files
3. ⏳ Migrate 7 priority packages (33 files) to core/domain plugins
4. ⏳ Delete verified duplicates from legacy plugin
5. ⏳ Test and validate migration

---

**Decision:** ✅ Approved - Leave service/ for Phase 3
**Effort Saved:** ~1 hour (service/ migration avoided)
**Final Migration Scope:** 33 files (down from 71 files, 54% reduction!)
