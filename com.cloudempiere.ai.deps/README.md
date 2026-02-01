# CloudEmpiere AI - Shared Dependencies Plugin

**Bundle ID:** `com.cloudempiere.ai.deps`
**Version:** 0.35.0-SNAPSHOT
**Type:** OSGi Bundle (Shared Library)

## Purpose

Central repository for all third-party dependencies used by CloudEmpiere AI plugins. This plugin embeds and exports shared libraries to avoid JAR duplication across domain plugins.

## Contents

**55 Embedded JARs** (15 MB total):

### LangChain4j 0.35.0
- `langchain4j-0.35.0.jar` (core framework)
- `langchain4j-anthropic-0.35.0.jar` (Claude integration)
- `langchain4j-aws-bedrock-0.35.0.jar` (AWS Bedrock)
- `langchain4j-ollama-0.35.0.jar` (Ollama local LLMs)
- `langchain4j-open-ai-0.35.0.jar` (OpenAI compatible)

> **Note:** Version 0.35.0 is the last Java 11 compatible release. Version 0.36.0+ requires Java 17.

### AWS SDK 2.20.162
- Bedrock Runtime (Claude, Llama, Mistral models)
- Regions, HTTP client, authentication
- 10 JARs total

### JSON Processing
- Jackson 2.17.0 (4 JARs)
- Gson 2.10.1

### HTTP Clients
- OkHttp 4.12.0 (3 JARs)
- Apache HttpClient 4.5.14 (5 JARs)

### Async I/O
- Netty 4.1.100 (12 JARs)

### Kotlin Runtime
- Kotlin 1.9.10 (3 JARs) - Required by LangChain4j

### Reactive Streams
- Reactor 3.6.0 (2 JARs)
- Reactive Streams 1.0.4

## Exported Packages

All library packages are exported with their semantic versions:

```
dev.langchain4j.*;version="0.35.0"
software.amazon.awssdk.*;version="2.20.162"
com.fasterxml.jackson.*;version="2.17.0"
com.google.gson.*;version="2.10.1"
okhttp3.*;version="4.12.0"
org.apache.http.*;version="4.5.14"
io.netty.*;version="4.1.100"
kotlin.*;version="1.9.10"
reactor.core.*;version="3.6.0"
org.reactivestreams.*;version="1.0.4"
```

## How It Works

### 1. Maven Dependency Copy
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals>
        <goal>copy</goal>
      </goals>
      <configuration>
        <artifactItems>
          <artifactItem>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>0.35.0</version>
            <destFileName>langchain4j-0.35.0.jar</destFileName>
          </artifactItem>
          <!-- ... 54 more JARs ... -->
        </artifactItems>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 2. OSGi Bundle-ClassPath
```
Bundle-ClassPath: .,
 lib/langchain4j-0.35.0.jar,
 lib/langchain4j-anthropic-0.35.0.jar,
 lib/aws-sdk-bedrock-2.20.162.jar,
 ...
```

### 3. Export-Package
```
Export-Package: dev.langchain4j;version="0.35.0",
 dev.langchain4j.agent;version="0.35.0",
 software.amazon.awssdk.services.bedrock;version="2.20.162",
 ...
```

### 4. Importing in Other Plugins
```
# com.cloudempiere.ai.core/META-INF/MANIFEST.MF
Import-Package: dev.langchain4j;version="0.35.0",
 software.amazon.awssdk.services.bedrockruntime;version="2.20.162",
 ...
```

## Building

```bash
cd com.cloudempiere.ai.deps
mvn clean install
```

**Build Output:**
- `target/com.cloudempiere.ai.deps-0.35.0-SNAPSHOT.jar` (15 MB)
- Contains all 55 JARs in `lib/` directory

## Dependencies

**Required:**
- iDempiere v10 (Java 11)
- OSGi Framework 1.10.0+

**No runtime dependencies** - this plugin is self-contained.

## Version Constraints

| Library | Version | Constraint | Reason |
|---------|---------|------------|--------|
| LangChain4j | 0.35.0 | **Must stay 0.35.0** | Last Java 11 compatible version |
| AWS SDK | 2.20.162 | Can upgrade | Java 11 compatible |
| Jackson | 2.17.0 | Can upgrade | Java 11 compatible |

> ⚠️ **Critical:** Do not upgrade LangChain4j to 0.36.0+ until iDempiere migrates to Java 17 (v11).

## Upgrading Dependencies

### Safe Upgrades (Java 11 compatible):
```bash
# AWS SDK
mvn versions:use-latest-versions \
  -Dincludes="software.amazon.awssdk:*"

# Jackson
mvn versions:use-latest-versions \
  -Dincludes="com.fasterxml.jackson.core:*"
```

### Unsafe Upgrades (Requires Java 17):
- ❌ LangChain4j 0.36.0+
- ❌ LangChain4j 1.0.0+

## Usage in Domain Plugins

Domain plugins import packages from this plugin:

```java
// com.cloudempiere.ai.sales/src/.../SalesAgent.java
import dev.langchain4j.model.chat.ChatLanguageModel;  // From deps plugin
import dev.langchain4j.data.message.AiMessage;         // From deps plugin
import software.amazon.awssdk.services.bedrock.*;      // From deps plugin

public class SalesAgent {
    private ChatLanguageModel model;  // Interface from deps plugin
}
```

**OSGi resolves imports automatically:**
1. Plugin declares `Import-Package: dev.langchain4j;version="0.35.0"`
2. OSGi finds `Export-Package: dev.langchain4j;version="0.35.0"` in deps plugin
3. Classloader delegates to deps plugin for LangChain4j classes

## Troubleshooting

### ClassNotFoundException
**Symptom:** `java.lang.ClassNotFoundException: dev.langchain4j.model.chat.ChatLanguageModel`

**Solution:**
1. Verify deps plugin is installed: `ss com.cloudempiere.ai.deps`
2. Check MANIFEST.MF has `Import-Package` for missing class
3. Rebuild deps plugin: `cd com.cloudempiere.ai.deps && mvn clean install`

### Version Conflicts
**Symptom:** `Bundle constraint violation: Import-Package: dev.langchain4j; version="[0.36.0,0.37.0)"`

**Solution:**
- Domain plugin trying to import newer version than deps exports
- Update deps plugin to newer version OR
- Downgrade domain plugin's Import-Package version

## References

- **ADR-035:** Java Version Strategy and Migration Path
- **OSGI_MULTI_PLUGIN_ARCHITECTURE.md:** Multi-plugin design
- **PLUGIN_ARCHITECTURE.md:** Why plugins are split this way

## Maintainers

CloudEmpiere AI Team

**Last Updated:** 2026-01-30
