# OSGi Validation Scripts

## validate-osgi-deps.sh

Validates OSGi dependency configuration for bundles with embedded JARs.

### What it checks

1. **Bundle-ClassPath completeness** - All JARs in `lib/` are listed in `Bundle-ClassPath`
2. **OSGi bundle detection** - Identifies which JARs are OSGi bundles (have MANIFEST.MF with Export-Package)
3. **Package re-export validation** - Verifies exported packages from embedded OSGi bundles are re-exported
4. **Transitive dependency hints** - Shows Import-Package declarations from embedded JARs

### Usage

```bash
./scripts/validate-osgi-deps.sh <bundle-dir>
```

**Examples:**
```bash
# Validate deps bundle
./scripts/validate-osgi-deps.sh com.cloudempiere.ai.deps

# Validate any bundle with lib/
./scripts/validate-osgi-deps.sh com.cloudempiere.ai.plugin
```

### Exit codes

- `0` - All validations passed
- `1` - Critical issues found (missing JARs in Bundle-ClassPath)

### Sample output

```
🔍 Validating OSGi dependencies for: com.cloudempiere.ai.deps

📋 Bundle-ClassPath entries:
.
lib/langchain4j-0.35.0.jar
lib/langchain4j-core-0.35.0.jar
...

📦 Export-Package entries:
dev.langchain4j;version="0.35.0"
dev.langchain4j.model.chat.listener;version="0.35.0"
org.slf4j;version="1.7.36"
...

🔎 Analyzing JARs in lib/...

════════════════════════════════════════════════════════════
VALIDATION RESULTS
════════════════════════════════════════════════════════════

✅ All JARs are in Bundle-ClassPath

📦 OSGi bundles detected (21):
   - slf4j-api-1.7.36.jar
   - jackson-core-2.17.0.jar
   ...

✅ All OSGi bundle packages are re-exported

════════════════════════════════════════════════════════════

✅ Validation passed!
```

## Other OSGi Validation Tools

### 1. Maven Dependency Tree

Shows project dependencies (but not transitive deps of embedded JARs):

```bash
cd com.cloudempiere.ai.deps
mvn dependency:tree -Dverbose > dependency-tree.txt
```

### 2. BND Tools

Comprehensive OSGi tooling for analyzing JARs and generating manifests:

```bash
# Install bnd CLI
brew install bnd

# Analyze a JAR
bnd print lib/langchain4j-core-0.35.0.jar

# Wrap non-OSGi JAR
bnd wrap -o output.jar input.jar
```

### 3. Eclipse PDE Analysis

In Eclipse IDE:
- Right-click bundle → PDE Tools → Open Manifest Analysis
- Shows unused imports, missing exports, version conflicts

### 4. OSGi Console (Runtime)

Check bundle status in running iDempiere:

```
osgi> ss | grep cloudempiere
osgi> diag <bundle-id>
osgi> packages <package-name>
```

### 5. Tycho Dependency Resolution

Build-time validation:

```bash
# Full build shows resolution errors
mvn clean install

# Verbose resolution
mvn -X clean install | grep "Resolving"
```

## Common Issues Detected

### Missing in Bundle-ClassPath
```
❌ JARs NOT in Bundle-ClassPath:
   - slf4j-api-1.7.36.jar
```
**Fix:** Add to MANIFEST.MF Bundle-ClassPath

### Missing Package Exports
```
⚠️  Packages NOT re-exported:
   - org.slf4j (from slf4j-api-1.7.36.jar)
```
**Fix:** Add to MANIFEST.MF Export-Package

### Transitive Dependencies
```
⚠️  langchain4j-bedrock-0.35.0.jar imports: org.slf4j (may need to be provided)
```
**Fix:** Ensure org.slf4j is available (exported or in classpath)

## Best Practices

1. **Run validation** before committing MANIFEST.MF changes
2. **Check after adding** new JARs to lib/
3. **Re-run after Maven build** (validate phase downloads JARs)
4. **Include in CI/CD** pipeline for automated checks

## Integration with CI/CD

Add to GitHub Actions / Jenkins:

```yaml
- name: Validate OSGi Dependencies
  run: |
    ./scripts/validate-osgi-deps.sh com.cloudempiere.ai.deps
```
