# Cloudempiere AI - Parent POM

**Artifact ID:** `com.cloudempiere.ai.parent`
**Version:** 10.0.2-SNAPSHOT
**Type:** Maven Parent POM
**Packaging:** pom

## Purpose

Maven parent POM that provides:
- **Centralized dependency management** for all Cloudempiere AI plugins
- **Shared build configuration** (compiler, plugins, versions)
- **Tycho configuration** for OSGi bundle builds
- **Version inheritance** for child modules

## Why a Parent POM?

### Without Parent POM (Duplicate Configuration)
```xml
<!-- com.cloudempiere.ai.core/pom.xml -->
<properties>
    <tycho.version>2.7.5</tycho.version>
    <maven.compiler.source>11</maven.compiler.source>
    ...
</properties>
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-maven-plugin</artifactId>
    <version>2.7.5</version>  ← Duplicated
</plugin>

<!-- com.cloudempiere.ai.sales/pom.xml -->
<properties>
    <tycho.version>2.7.5</tycho.version>  ← Duplicated
    <maven.compiler.source>11</maven.compiler.source>  ← Duplicated
    ...
</properties>
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-maven-plugin</artifactId>
    <version>2.7.5</version>  ← Duplicated
</plugin>
```

### With Parent POM (Single Source of Truth)
```xml
<!-- com.cloudempiere.ai.parent/pom.xml -->
<properties>
    <tycho.version>2.7.5</tycho.version>
    <maven.compiler.source>11</maven.compiler.source>
</properties>
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-maven-plugin</artifactId>
    <version>${tycho.version}</version>
</plugin>

<!-- com.cloudempiere.ai.core/pom.xml -->
<parent>
    <groupId>com.cloudempiere</groupId>
    <artifactId>com.cloudempiere.ai.parent</artifactId>
    <version>10.0.2-SNAPSHOT</version>
    <relativePath>../com.cloudempiere.ai.parent</relativePath>
</parent>
<!-- Inherits all properties and plugin configuration! -->
```

## Structure

```
com.cloudempiere.ai.parent/
├── pom.xml                    ← Parent POM
└── README.md                  ← This file
```

**No source code** - this is purely a Maven configuration project.

## pom.xml Overview

### 1. Project Coordinates
```xml
<groupId>com.cloudempiere</groupId>
<artifactId>com.cloudempiere.ai.parent</artifactId>
<version>10.0.2-SNAPSHOT</version>
<packaging>pom</packaging>
```

### 2. Properties (Versions)
```xml
<properties>
    <!-- Java Version -->
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Tycho (OSGi Build) -->
    <tycho.version>2.7.5</tycho.version>

    <!-- iDempiere Version -->
    <idempiere.version>10.0.0-SNAPSHOT</idempiere.version>

    <!-- LangChain4j Version (CRITICAL: Java 11 compatible) -->
    <langchain4j.version>0.35.0</langchain4j.version>

    <!-- AWS SDK Version -->
    <aws.sdk.version>2.20.162</aws.sdk.version>

    <!-- Jackson Version -->
    <jackson.version>2.17.0</jackson.version>
</properties>
```

### 3. Dependency Management
```xml
<dependencyManagement>
    <dependencies>
        <!-- LangChain4j -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- AWS SDK -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bedrockruntime</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**What this does:**
- Child POMs can reference dependencies without specifying versions
- Example in child POM:
  ```xml
  <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j</artifactId>
      <!-- No version needed - inherited from parent -->
  </dependency>
  ```

### 4. Plugin Management
```xml
<build>
    <pluginManagement>
        <plugins>
            <!-- Tycho Maven Plugin -->
            <plugin>
                <groupId>org.eclipse.tycho</groupId>
                <artifactId>tycho-maven-plugin</artifactId>
                <version>${tycho.version}</version>
                <extensions>true</extensions>
            </plugin>

            <!-- Tycho Compiler Plugin -->
            <plugin>
                <groupId>org.eclipse.tycho</groupId>
                <artifactId>tycho-compiler-plugin</artifactId>
                <version>${tycho.version}</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>

            <!-- Maven Dependency Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.6.0</version>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

### 5. Repositories
```xml
<repositories>
    <!-- iDempiere Repository -->
    <repository>
        <id>idempiere-releases</id>
        <url>https://maven.idempiere.org/repository/maven-releases/</url>
    </repository>

    <!-- Maven Central -->
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
</repositories>
```

## Child Modules

All Cloudempiere AI plugins inherit from this parent:

```
com.cloudempiere.ai.parent
├── com.cloudempiere.ai.deps
├── com.cloudempiere.ai.core
├── com.cloudempiere.ai.plugin
├── com.cloudempiere.ai.sales (future)
├── com.cloudempiere.ai.inventory (future)
├── com.cloudempiere.ai.purchasing (future)
├── com.cloudempiere.ai.support (future)
└── com.cloudempiere.ai.kb (future)
```

## Version Management

### Semantic Versioning

Parent POM follows iDempiere version:
```
10.0.2-SNAPSHOT
││ │ └─ SNAPSHOT (development build)
││ └─── Patch version
│└───── Minor version
└────── Major version (iDempiere v10)
```

### SNAPSHOT vs Release

**SNAPSHOT:**
```xml
<version>10.0.2-SNAPSHOT</version>
```
- Development builds
- Can be republished
- Maven always downloads latest

**Release:**
```xml
<version>10.0.2</version>
```
- Stable release
- Immutable (cannot republish)
- Maven caches once

### Version Inheritance

**Parent POM:**
```xml
<version>10.0.2-SNAPSHOT</version>
```

**Child POM automatically inherits:**
```xml
<parent>
    <groupId>com.cloudempiere</groupId>
    <artifactId>com.cloudempiere.ai.parent</artifactId>
    <version>10.0.2-SNAPSHOT</version>  ← Inherited
</parent>
```

## Dependency Version Constraints

### Critical Version Requirements

| Dependency | Current | Constraint | Reason |
|------------|---------|------------|--------|
| **Java** | 11 | **Must be 11** | iDempiere v10 requirement |
| **LangChain4j** | 0.35.0 | **Must be ≤0.35.0** | Last Java 11 compatible version |
| **iDempiere** | 10.0.0 | Must be 10.x | v11 requires Java 17 |
| **Tycho** | 2.7.5 | Can upgrade | Build tool only |
| **AWS SDK** | 2.20.162 | Can upgrade | Java 11 compatible |
| **Jackson** | 2.17.0 | Can upgrade | Java 11 compatible |

⚠️ **WARNING:** Do not upgrade LangChain4j to 0.36.0+ until iDempiere migrates to Java 17 (v11).

### Reference: ADR-035

See [ADR-035: Java Version Strategy and Migration Path](../docs/adr/035-java-version-strategy.md) for full details on version constraints.

## Building

### Build All Modules
```bash
cd com.cloudempiere.ai
mvn clean install
```

**Maven reactor resolves:**
1. Parent POM first
2. All child modules in dependency order

### Build Single Module
```bash
cd com.cloudempiere.ai.core
mvn clean install
```

**Parent POM still used** - child inherits configuration.

### Skip Tests
```bash
mvn clean install -DskipTests
```

### Parallel Build
```bash
mvn clean install -T 4  # 4 threads
```

## Changing Versions

### Update Parent Version
```bash
# Use Maven Versions Plugin
mvn versions:set -DnewVersion=10.0.3-SNAPSHOT

# Updates parent and all children
# Commit changes
git add pom.xml */pom.xml
git commit -m "chore: bump version to 10.0.3-SNAPSHOT"
```

### Update Dependency Version
```xml
<!-- In parent pom.xml -->
<properties>
    <langchain4j.version>0.35.0</langchain4j.version>
</properties>

<!-- Change to: -->
<properties>
    <langchain4j.version>0.36.0</langchain4j.version>  ← Only if Java 17!
</properties>
```

**All child modules automatically use new version.**

## Multi-Module Reactor Build

### Reactor Order

Maven builds in dependency order:
```
[INFO] Reactor Build Order:
[INFO]
[INFO] Cloudempiere AI - Parent                        [pom]
[INFO] Cloudempiere AI - Shared Dependencies           [eclipse-plugin]
[INFO] Cloudempiere AI - Core Infrastructure           [eclipse-plugin]
[INFO] Cloudempiere AI                                 [eclipse-plugin]
[INFO] Cloudempiere AI - Theme                         [eclipse-plugin]
[INFO] Cloudempiere AI - Feature                       [eclipse-feature]
[INFO] Cloudempiere AI - P2 Repository                 [eclipse-repository]
```

### Dependency Graph
```
parent
  ↓
deps (no dependencies)
  ↓
core (depends on deps)
  ↓
plugin (depends on core)
theme (fragment of org.adempiere.ui.zk)
  ↓
feature (includes all plugins)
  ↓
p2 (publishes feature)
```

## IDE Integration

### Eclipse Import

1. **Import parent first:**
   ```
   File → Import → Maven → Existing Maven Projects
   → Select com.cloudempiere.ai/com.cloudempiere.ai.parent
   ```

2. **Import child modules:**
   ```
   → Select com.cloudempiere.ai/com.cloudempiere.ai.core
   → Select com.cloudempiere.ai/com.cloudempiere.ai.deps
   ```

Eclipse automatically recognizes parent-child relationship.

### IntelliJ IDEA Import

```
File → Open
→ Select com.cloudempiere.ai/pom.xml (root aggregator)
```

IntelliJ imports entire multi-module project.

## Troubleshooting

### Parent POM Not Found
**Symptom:** `Could not find artifact com.cloudempiere:com.cloudempiere.ai.parent:pom:10.0.2-SNAPSHOT`

**Solution:**
1. Build parent first: `cd com.cloudempiere.ai.parent && mvn clean install`
2. Verify relativePath in child POM is correct
3. Check parent version matches child's parent declaration

### Version Mismatch
**Symptom:** `The project ... has an invalid parent`

**Solution:**
- Parent version in child POM must match parent's actual version
- Run `mvn versions:set` to sync versions

### Tycho Version Conflict
**Symptom:** `Plugin org.eclipse.tycho:tycho-maven-plugin or one of its dependencies could not be resolved`

**Solution:**
1. Check internet connection (downloads from Maven Central)
2. Clear local repository: `rm -rf ~/.m2/repository/org/eclipse/tycho`
3. Retry build

## Best Practices

1. **Always build parent first** in new workspace
2. **Keep versions synchronized** across all child modules
3. **Update parent POM only** for shared dependency versions
4. **Test after version changes** - run full reactor build
5. **Document version constraints** in comments

## Migration Path

### From Standalone POMs to Parent POM

**Before (Standalone):**
```xml
<!-- com.cloudempiere.ai.core/pom.xml -->
<project>
    <groupId>com.cloudempiere</groupId>
    <artifactId>com.cloudempiere.ai.core</artifactId>
    <version>0.32.0-SNAPSHOT</version>
    <properties>
        <tycho.version>2.7.5</tycho.version>
    </properties>
</project>
```

**After (With Parent):**
```xml
<!-- com.cloudempiere.ai.core/pom.xml -->
<project>
    <parent>
        <groupId>com.cloudempiere</groupId>
        <artifactId>com.cloudempiere.ai.parent</artifactId>
        <version>10.0.2-SNAPSHOT</version>
        <relativePath>../com.cloudempiere.ai.parent</relativePath>
    </parent>

    <artifactId>com.cloudempiere.ai.core</artifactId>
    <version>0.32.0-SNAPSHOT</version>
    <!-- Properties inherited from parent -->
</project>
```

## References

- **Maven Parent POM Guide:** https://maven.apache.org/guides/introduction/introduction-to-the-pom.html
- **Tycho Documentation:** https://www.eclipse.org/tycho/
- **Maven Version Plugin:** https://www.mojohaus.org/versions-maven-plugin/

## Maintainers

Cloudempiere AI Team

**Last Updated:** 2026-01-30
