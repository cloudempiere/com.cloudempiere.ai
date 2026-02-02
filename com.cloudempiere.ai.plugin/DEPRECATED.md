# DEPRECATED: Legacy Monolithic Plugin

**Status:** 🚨 DEPRECATED
**Deprecation Date:** 2026-02-02
**Removal Target:** Version 1.1.0 (Q2 2026)
**Replacement:** Multi-plugin architecture (core + 5 domains)

---

## ⚠️ Warning

This plugin is **deprecated** and will be removed in a future version. All functionality has been migrated to the new multi-plugin architecture:

- **com.cloudempiere.ai.core** - Core infrastructure
- **com.cloudempiere.ai.sales** - Sales domain agent
- **com.cloudempiere.ai.inventory** - Inventory domain agent
- **com.cloudempiere.ai.purchasing** - Purchasing domain agent
- **com.cloudempiere.ai.support** - Support domain agent
- **com.cloudempiere.ai.kb** - Knowledge base domain agent
- **com.cloudempiere.ai.deps** - Shared dependencies

---

## Migration Status

### ✅ Fully Migrated (Safe to Delete)

| Legacy Package | New Location | Verification |
|----------------|--------------|--------------|
| `provider/` | `core/provider/` | ✅ Verified |
| `database/` | `core/database/` | ✅ Verified |
| `context/` | `core/context/` | ✅ Verified |
| `component/` | `core/component/` | ✅ Verified |
| `util/` | `core/util/` | ✅ Verified |
| `model/` | `core/model/` | ✅ Verified |
| `process/` | `core/process/` | ✅ Verified |
| `boundary/` | `core/boundary/` | ✅ Verified |

### ⚠️ Partial Migration (Still In Use)

| Legacy Package | Status | Blocker | Target |
|----------------|--------|---------|--------|
| `rag/` | 🟡 In Use | Need PgVectorStore | `core/rag/` |
| `guardrails/` | 🟡 In Use | Need validation | `core/guardrails/` |
| `observability/` | 🟡 In Use | Need metrics collection | `core/observability/` |
| `kb/` | 🟡 In Use | Need parser migration | `kb/kb/` |
| `health/` | 🟡 In Use | Need health checks | `core/health/` |
| `error/` | 🟡 In Use | Need exception classes | `core/error/` |
| `event/` | 🟡 In Use | Need event system | `core/event/` |

### ❓ Under Investigation

| Legacy Package | Purpose | Investigation Status |
|----------------|---------|---------------------|
| `routing/` | Query routing | Check if OrchestratorAgent replaces this |
| `service/` | Service layer | Check if facade replaces this |
| `tool/` | Monolithic tools | Check if domain tools replace this |
| `factory/` | Factory classes | Check if provider factory replaces this |
| `function/` | Unknown | Need to investigate |
| `agent/langchain4j/` | Agent implementation | Check if domain agents replace this |

---

## How to Migrate

**Developers:** Do NOT import from `com.cloudempiere.ai.plugin`. Use new plugins instead:

```java
// ❌ BAD (deprecated)
import com.cloudempiere.ai.plugin.provider.IAIProvider;

// ✅ GOOD (new architecture)
import com.cloudempiere.ai.provider.IAIProvider;
```

**Deployments:** Install new plugins alongside legacy plugin. Once validated, remove legacy plugin.

---

## Validation Checklist

Before removing this plugin, verify:

- [ ] All unit tests pass with new plugins only
- [ ] All integration tests pass
- [ ] Chat panel works without legacy plugin
- [ ] All 5 domain agents operational
- [ ] Provider integration works
- [ ] RAG infrastructure migrated
- [ ] Guardrails migrated
- [ ] Observability migrated
- [ ] No runtime exceptions from missing classes

---

## Support

For migration assistance, see:
- [Multi-Plugin Architecture Guide](../OSGI_MULTI_PLUGIN_ARCHITECTURE.md)
- [Migration FAQ](../docs/MIGRATION_FAQ.md)
- Contact: Cloudempiere Development Team
