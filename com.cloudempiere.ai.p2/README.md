# CloudEmpiere AI - P2 Repository

**Project ID:** `com.cloudempiere.ai.p2`
**Version:** 10.0.2-SNAPSHOT
**Type:** Eclipse P2 Repository (Update Site)

## Purpose

Eclipse P2 repository (update site) that hosts all CloudEmpiere AI plugins and features for installation via iDempiere Plugin Manager.

## What is a P2 Repository?

**P2** (Provisioning Platform) is Eclipse's installation and update framework. A P2 repository contains:
- **Features** - Installable units (groups of plugins)
- **Plugins** - OSGi bundles (JAR files)
- **Metadata** - Dependency information, versions, requirements

```
┌──────────────────────────────────────┐
│  com.cloudempiere.ai.p2              │  ← P2 Repository
│  (Update Site)                       │
│                                      │
│  target/repository/                  │
│  ├── features/                       │
│  │   └── com.cloudempiere.ai.feature_10.0.2.jar
│  ├── plugins/                        │
│  │   ├── com.cloudempiere.ai.deps_0.35.0.jar
│  │   ├── com.cloudempiere.ai.core_0.32.0.jar
│  │   ├── com.cloudempiere.ai_10.0.2.jar
│  │   └── com.cloudempiere.ai.theme_10.0.2.jar
│  ├── artifacts.jar (metadata)        │
│  └── content.jar (index)             │
└──────────────────────────────────────┘
```

## Repository Structure

### After Build (target/repository/)

```
target/repository/
├── features/
│   └── com.cloudempiere.ai.feature_10.0.2.202601301952.jar
│
├── plugins/
│   ├── com.cloudempiere.ai.deps_0.35.0.202601301952.jar (15 MB)
│   ├── com.cloudempiere.ai.core_0.32.0.202601301952.jar (500 KB)
│   ├── com.cloudempiere.ai_10.0.2.202601301952.jar (200 KB)
│   └── com.cloudempiere.ai.theme_10.0.2.202601301952.jar (50 KB)
│
├── artifacts.jar
│   └── Contains P2 artifact metadata (checksums, sizes, etc.)
│
├── content.jar
│   └── Contains installable unit definitions and dependencies
│
└── p2.index
    └── Repository index file
```

**Total Repository Size:** ~16 MB

### Metadata Files

**artifacts.jar:**
```xml
<artifact classifier="osgi.bundle" id="com.cloudempiere.ai.deps" version="0.35.0">
  <properties>
    <property name="artifact.size" value="15728640"/>
    <property name="download.size" value="15728640"/>
    <property name="download.md5" value="..."/>
  </properties>
</artifact>
```

**content.jar:**
```xml
<unit id="com.cloudempiere.ai.feature.feature.group" version="10.0.2">
  <requires>
    <required namespace="org.eclipse.equinox.p2.iu"
              name="com.cloudempiere.ai.deps"
              range="[0.35.0,0.36.0)"/>
    <required namespace="org.eclipse.equinox.p2.iu"
              name="com.cloudempiere.ai.core"
              range="[0.32.0,0.33.0)"/>
  </requires>
</unit>
```

## Building the Repository

```bash
cd com.cloudempiere.ai.p2
mvn clean install
```

### Build Process

1. **Collect Artifacts:**
   ```
   [INFO] Collecting plugins...
   [INFO]   - com.cloudempiere.ai.deps_0.35.0
   [INFO]   - com.cloudempiere.ai.core_0.32.0
   [INFO]   - com.cloudempiere.ai_10.0.2
   [INFO]   - com.cloudempiere.ai.theme_10.0.2
   [INFO] Collecting features...
   [INFO]   - com.cloudempiere.ai.feature_10.0.2
   ```

2. **Generate Metadata:**
   ```
   [INFO] Building P2 repository...
   [INFO]   artifacts.jar (120 KB)
   [INFO]   content.jar (80 KB)
   ```

3. **Create Repository:**
   ```
   [INFO] P2 repository created: target/repository/
   [INFO] Repository size: 16.2 MB
   ```

### Build Output

```
target/
├── repository/               ← P2 repository (deployable)
│   ├── features/
│   ├── plugins/
│   ├── artifacts.jar
│   └── content.jar
│
└── com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip
    └── Same as repository/ (zipped for distribution)
```

## Installation from Repository

### Method 1: Local File Repository

```bash
# iDempiere Plugin Manager
Add Repository → Local → Browse
→ Select: /path/to/com.cloudempiere.ai/com.cloudempiere.ai.p2/target/repository/
→ Install Available Software
→ Select "CloudEmpiere AI"
→ Finish
```

### Method 2: HTTP Repository

```bash
# Deploy repository to web server
scp -r target/repository/* user@server:/var/www/html/p2/cloudempiere-ai/

# In iDempiere
Add Repository → http://server/p2/cloudempiere-ai/
→ Install Available Software
```

### Method 3: Composite Repository

```bash
# Add to existing iDempiere composite repository
cd $IDEMPIERE_HOME/repository
echo "http://server/p2/cloudempiere-ai/" >> compositeArtifacts.xml
```

## Repository Categories

### category.xml (Optional)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <category-def name="cloudempiere.ai" label="CloudEmpiere AI">
      <description>
         AI capabilities for iDempiere including Claude, AWS Bedrock, and Ollama.
      </description>
   </category-def>

   <feature
         id="com.cloudempiere.ai.feature"
         version="10.0.2.qualifier">
      <category name="cloudempiere.ai"/>
   </feature>
</site>
```

**Benefit:** Features grouped in installation UI.

## Versioning Strategy

### Timestamp Qualifiers

Maven Tycho replaces `.qualifier` with build timestamp:

```
Before Build:
- com.cloudempiere.ai.core_0.32.0.qualifier.jar

After Build:
- com.cloudempiere.ai.core_0.32.0.202601301952.jar
                              └────────────┘
                                 Timestamp
```

**Format:** `YYYYMMDDHHmm`

### Version Ranges

P2 uses version ranges for dependencies:

```xml
<!-- Accepts any 0.35.x version -->
<required name="com.cloudempiere.ai.deps" range="[0.35.0,0.36.0)"/>

<!-- Accepts any 0.32.x version -->
<required name="com.cloudempiere.ai.core" range="[0.32.0,0.33.0)"/>
```

**Syntax:**
- `[0.35.0,0.36.0)` - Inclusive 0.35.x, exclusive 0.36.0
- `[1.0.0,2.0.0)` - Any 1.x version
- `0.0.0` - Any version (not recommended)

## Composite Repositories

For multi-version support:

```
composite-repository/
├── compositeArtifacts.xml
├── compositeContent.xml
├── 10.0/  (iDempiere v10)
│   └── repository/
└── 11.0/  (iDempiere v11 - future)
    └── repository/
```

**compositeArtifacts.xml:**
```xml
<?xml version='1.0' encoding='UTF-8'?>
<repository name='CloudEmpiere AI Composite Repository'
            type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository'
            version='1.0.0'>
  <children size='2'>
    <child location='10.0'/>
    <child location='11.0'/>
  </children>
</repository>
```

## Mirroring

### Create Mirror

```bash
# Eclipse P2 Director
p2.director \
  -source file:///path/to/source/repository \
  -destination file:///path/to/mirror \
  -mirror
```

### Incremental Updates

```bash
# Add new builds to existing repository
mvn clean install -Dp2.append=true
```

## Repository Hosting

### 1. Local File System

```
file:///home/user/repositories/cloudempiere-ai/
```

**Use Case:** Development, testing

### 2. HTTP/HTTPS Server

```
https://repository.cloudempiere.com/p2/ai/
```

**Use Case:** Production, public releases

**Requirements:**
- Static file hosting (Apache, Nginx, S3)
- No special server-side processing
- Just serve files with correct MIME types

### 3. Composite Repository

```
https://repository.cloudempiere.com/p2/composite/
├── 10.0/ (iDempiere v10)
└── 11.0/ (iDempiere v11)
```

**Use Case:** Multi-version support

## Security

### Repository Signing

```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <configuration>
    <includeAllDependencies>true</includeAllDependencies>
    <compress>true</compress>
  </configuration>
</plugin>
```

### HTTPS Only

For production repositories:
- Always use HTTPS
- Verify SSL certificates
- Use trusted certificate authorities

## Troubleshooting

### Repository Not Found
**Symptom:** "Unable to read repository at http://..."

**Solution:**
1. Verify URL is correct
2. Check server is running
3. Verify firewall allows access
4. Check MIME types: `.jar` → `application/java-archive`

### Dependency Resolution Failed
**Symptom:** "Cannot satisfy dependency: com.cloudempiere.ai.core requires..."

**Solution:**
1. Verify all plugins are in repository
2. Check version ranges are correct
3. Rebuild repository: `mvn clean install`

### Corrupted Metadata
**Symptom:** "Invalid repository metadata"

**Solution:**
```bash
cd com.cloudempiere.ai.p2
mvn clean  # Remove corrupted build
mvn install  # Rebuild clean repository
```

### Large Repository Size
**Symptom:** Repository is too large for download

**Solution:**
1. Enable compression in pom.xml: `<compress>true</compress>`
2. Split into multiple repositories
3. Use composite repository

## Updating the Repository

### Add New Plugin

1. **Create plugin**
2. **Add to feature:**
   ```xml
   <plugin
         id="com.cloudempiere.ai.newplugin"
         version="0.32.0.qualifier"/>
   ```
3. **Rebuild:**
   ```bash
   cd com.cloudempiere.ai.p2
   mvn clean install
   ```

### Version Bump

1. **Update plugin version:** `0.32.0` → `0.32.1`
2. **Update feature version:** `10.0.2` → `10.0.3`
3. **Rebuild repository**
4. **Deploy to server**

### Repository URL in Installation

```
Old Version:
https://repo.cloudempiere.com/p2/ai/10.0.2/

New Version:
https://repo.cloudempiere.com/p2/ai/10.0.3/

Latest (always current):
https://repo.cloudempiere.com/p2/ai/latest/
  → Symlink to current version
```

## Continuous Integration

### GitHub Actions

```yaml
name: Build P2 Repository

on:
  push:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Build with Maven
        run: mvn clean install
      - name: Deploy Repository
        run: |
          scp -r com.cloudempiere.ai.p2/target/repository/* \
              deploy@server:/var/www/html/p2/ai/latest/
```

## References

- **P2 Documentation:** https://wiki.eclipse.org/Equinox/p2
- **Tycho P2 Plugin:** https://www.eclipse.org/tycho/sitedocs/tycho-p2/tycho-p2-repository-plugin/
- **iDempiere Plugin Management:** https://wiki.idempiere.org/en/Plugin_Installation

## Maintainers

CloudEmpiere AI Team

**Last Updated:** 2026-01-30
