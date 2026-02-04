# Cloudempiere AI - Feature

**Feature ID:** `com.cloudempiere.ai.feature`
**Version:** 10.0.2-SNAPSHOT
**Type:** Eclipse Feature (Plugin Group)

## Purpose

Eclipse feature definition that groups all Cloudempiere AI plugins into a single installable unit for iDempiere Plugin Manager.

## What is a Feature?

In Eclipse/OSGi, a **feature** is a container for plugins that:
- Groups related plugins together
- Defines installation dependencies
- Provides version management
- Enables atomic updates (all plugins update together)

```
┌──────────────────────────────────────┐
│  com.cloudempiere.ai.feature         │  ← Feature (installable unit)
│                                      │
│  Includes:                           │
│  ├── com.cloudempiere.ai.deps        │  ← Plugin 1
│  ├── com.cloudempiere.ai.core        │  ← Plugin 2
│  ├── com.cloudempiere.ai.plugin      │  ← Plugin 3
│  └── com.cloudempiere.ai.theme       │  ← Plugin 4 (fragment)
└──────────────────────────────────────┘
```

## Contents

### feature.xml

```xml
<feature
      id="com.cloudempiere.ai.feature"
      label="Cloudempiere AI"
      version="10.0.2.qualifier"
      provider-name="Cloudempiere">

   <description>
      AI capabilities for iDempiere ERP including Claude, AWS Bedrock,
      and Ollama integration with domain-specific agents.
   </description>

   <license url="http://www.gnu.org/licenses/gpl-2.0.html">
      GNU General Public License v2.0
   </license>

   <!-- Plugin Dependencies -->
   <plugin
         id="com.cloudempiere.ai.deps"
         version="0.35.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.core"
         version="0.32.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai"
         version="10.0.2.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.theme"
         version="10.0.2.qualifier"
         fragment="true"
         unpack="false"/>

</feature>
```

## How Installation Works

### 1. User Installs Feature
```bash
# In iDempiere Plugin Manager or via command line
iDempiere → Plugin Manager → Install from Repository
→ Select "Cloudempiere AI"
→ Install
```

### 2. Feature Resolver
Eclipse P2 (Provisioning Platform) analyzes the feature:
```
Feature: com.cloudempiere.ai.feature 10.0.2
├── Requires: com.cloudempiere.ai.deps 0.35.0
├── Requires: com.cloudempiere.ai.core 0.32.0
├── Requires: com.cloudempiere.ai 10.0.2
└── Requires: com.cloudempiere.ai.theme 10.0.2
```

### 3. Dependency Resolution
P2 downloads all required plugins:
```
Download: com.cloudempiere.ai.deps-0.35.0-SNAPSHOT.jar (15 MB)
Download: com.cloudempiere.ai.core-0.32.0-SNAPSHOT.jar (500 KB)
Download: com.cloudempiere.ai-10.0.2-SNAPSHOT.jar (200 KB)
Download: com.cloudempiere.ai.theme-10.0.2-SNAPSHOT.jar (50 KB)
```

### 4. Installation
```
Install to: $IDEMPIERE_HOME/plugins/
├── com.cloudempiere.ai.deps_0.35.0.jar
├── com.cloudempiere.ai.core_0.32.0.jar
├── com.cloudempiere.ai_10.0.2.jar
└── com.cloudempiere.ai.theme_10.0.2.jar
```

### 5. OSGi Activation
```
OSGi Framework:
├── Resolve com.cloudempiere.ai.deps → RESOLVED
├── Resolve com.cloudempiere.ai.core → RESOLVED
├── Resolve com.cloudempiere.ai → RESOLVED
├── Attach fragment com.cloudempiere.ai.theme → RESOLVED
├── Start com.cloudempiere.ai.deps → ACTIVE
├── Start com.cloudempiere.ai.core → ACTIVE
└── Start com.cloudempiere.ai → ACTIVE
```

## Included Plugins

| Plugin | Version | Purpose | Size |
|--------|---------|---------|------|
| **com.cloudempiere.ai.deps** | 0.35.0 | Shared dependencies (55 JARs) | 15 MB |
| **com.cloudempiere.ai.core** | 0.32.0 | Core infrastructure | 500 KB |
| **com.cloudempiere.ai** | 10.0.2 | Legacy monolithic (being phased out) | 200 KB |
| **com.cloudempiere.ai.theme** | 10.0.2 | Theme fragment (CSS, images) | 50 KB |

**Total Download:** ~16 MB

## Building

```bash
cd com.cloudempiere.ai.feature
mvn clean install
```

**Build Output:**
- `target/com.cloudempiere.ai.feature-10.0.2-SNAPSHOT.jar`
- Contains feature.xml and metadata

## Future: Domain Plugins

When domain plugins are created (Week 5), they will be added to the feature:

```xml
<feature id="com.cloudempiere.ai.feature" ...>
   <!-- Existing plugins -->
   <plugin id="com.cloudempiere.ai.deps" .../>
   <plugin id="com.cloudempiere.ai.core" .../>

   <!-- NEW: Domain plugins (Week 5) -->
   <plugin
         id="com.cloudempiere.ai.sales"
         version="0.32.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.inventory"
         version="0.32.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.purchasing"
         version="0.32.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.support"
         version="0.32.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.cloudempiere.ai.kb"
         version="0.32.0.qualifier"
         unpack="false"/>
</feature>
```

## Version Management

### Feature Versioning
- Feature version: `10.0.2` (matches iDempiere version)
- Qualifier: `.qualifier` (replaced by build timestamp)

### Plugin Versions
- **deps:** `0.35.0` (LangChain4j version)
- **core:** `0.32.0` (semantic versioning)
- **plugin:** `10.0.2` (iDempiere version)
- **domain plugins:** `0.32.0` (matches core)

### Update Behavior
```
Feature Update: 10.0.2 → 10.0.3
├── Update deps: 0.35.0 → 0.35.0 (no change)
├── Update core: 0.32.0 → 0.32.1 (bug fix)
├── Update plugin: 10.0.2 → 10.0.3 (version bump)
└── Update theme: 10.0.2 → 10.0.3 (version bump)
```

All plugins update atomically - cannot update just one.

## Installation Prerequisites

**Required:**
- iDempiere v10.0+
- Java 11 (Amazon Corretto recommended)
- OSGi Framework R7+

**Conflicts:**
- None (first version)

## Dependencies

**iDempiere Core:**
- `org.adempiere.base` (ERP core)
- `org.adempiere.ui.zk` (ZK UI framework)
- `org.compiere.db.postgresql.provider` (database)

**OSGi:**
- `org.eclipse.osgi` (OSGi framework)
- `org.osgi.service.component` (declarative services)

## Uninstallation

### Via Plugin Manager
```
iDempiere → Plugin Manager
→ Installed Plugins
→ Select "Cloudempiere AI"
→ Uninstall
```

### Manual Removal
```bash
cd $IDEMPIERE_HOME/plugins
rm com.cloudempiere.ai*.jar
# Restart iDempiere
```

**All 4 plugins removed** - feature ensures clean uninstall.

## Troubleshooting

### Feature Won't Install
**Symptom:** "Cannot complete the install because of a conflicting dependency"

**Solution:**
1. Check iDempiere version compatibility (v10 required)
2. Verify Java 11 (not Java 8 or Java 17)
3. Check plugin repository is accessible

### Partial Installation
**Symptom:** Some plugins installed, others missing

**Solution:**
- Features guarantee atomic installation
- If this happens, P2 repository is corrupted
- Uninstall feature and reinstall from clean repository

### Update Failed
**Symptom:** Update stuck at "Resolving dependencies"

**Solution:**
1. Clear P2 cache: `rm -rf $IDEMPIERE_HOME/p2/org.eclipse.equinox.p2.core`
2. Restart iDempiere
3. Retry update

## P2 Repository Integration

This feature is published to the P2 repository:

```
com.cloudempiere.ai.p2/
└── target/repository/
    ├── features/
    │   └── com.cloudempiere.ai.feature_10.0.2.jar
    ├── plugins/
    │   ├── com.cloudempiere.ai.deps_0.35.0.jar
    │   ├── com.cloudempiere.ai.core_0.32.0.jar
    │   ├── com.cloudempiere.ai_10.0.2.jar
    │   └── com.cloudempiere.ai.theme_10.0.2.jar
    ├── artifacts.jar (metadata)
    └── content.jar (index)
```

**Repository URL:** `file:///path/to/com.cloudempiere.ai/com.cloudempiere.ai.p2/target/repository/`

## References

- **Eclipse Features Guide:** https://wiki.eclipse.org/FAQ_What_is_a_feature%3F
- **P2 Provisioning:** https://wiki.eclipse.org/Equinox/p2
- **iDempiere Plugin Installation:** https://wiki.idempiere.org/en/Plugin_Installation

## Maintainers

Cloudempiere AI Team

**Last Updated:** 2026-01-30
