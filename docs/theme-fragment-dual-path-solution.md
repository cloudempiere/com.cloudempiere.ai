# AI Theme Fragment - Dual-Path Solution

## Problem

AI theme CSS fragment was not being detected by iDempiere's ThemeManager, resulting in custom AI chat styles not being included in the rendered theme.css.dsp output.

**Symptoms:**
- `ThemeManager.class.getResource(toClassPathResourcePath(customCSSURL))` returned null
- `isThemeHasCustomCSSFragment()` returned false
- AI chat CSS classes were missing from browser's theme.css.dsp

## Root Cause

**Path Mismatch Between Physical Structure and Classloader Expectations**

ThemeManager detection flow:
```java
// ThemeManager.isThemeHasCustomCSSFragment()
String customCSSURL = "~./theme/cloudempiere/css/fragment/custom.css.dsp";
String classpathPath = toClassPathResourcePath(customCSSURL);
// classpathPath = "/web/theme/cloudempiere/css/fragment/custom.css.dsp"

if (ThemeManager.class.getResource(classpathPath) == null) {
    return false; // Fragment NOT found
}
```

**The Issue:**
- ThemeManager converts ZK URL `~./theme/{themeName}/...` to classpath path `/web/theme/{themeName}/...`
- With `Bundle-ClassPath: .`, classloader looks for resources from JAR root
- Our files were at `WEB-INF/src/web/theme/...` (standard iDempiere location)
- But classloader couldn't find `/web/theme/...` because it was nested under `WEB-INF/src/`

## Solution: Dual-Path Approach

**Place CSS files at TWO locations:**

1. **Root-level `web/` directory** - For classloader detection
2. **`WEB-INF/src/web/` directory** - For standard iDempiere convention

### Directory Structure

```
com.cloudempiere.ai.theme/
├── web/
│   └── theme/
│       ├── default/css/fragment/custom.css.dsp
│       └── cloudempiere/css/fragment/custom.css.dsp
└── WEB-INF/
    └── src/
        └── web/
            └── theme/
                ├── default/css/fragment/custom.css.dsp
                └── cloudempiere/css/fragment/custom.css.dsp
```

### Configuration Files

**build.properties:**
```properties
bin.includes = META-INF/,\
               WEB-INF/,\
               web/,\        # ← Critical: includes root-level web/
               .
```

**META-INF/MANIFEST.MF:**
```manifest
Bundle-ClassPath: .
Jetty-WarFragmentFolderPath: /
Fragment-Host: org.adempiere.ui.zk;bundle-version="10.0.0"
```

### Why This Works

**Classloader Access (Detection):**
- With `Bundle-ClassPath: .`, the OSGi classloader starts from JAR root
- `ThemeManager.class.getResource("/web/theme/cloudempiere/css/fragment/custom.css.dsp")`
- Finds file at root-level `web/theme/cloudempiere/css/fragment/custom.css.dsp`
- ✅ Returns non-null → `isThemeHasCustomCSSFragment()` = TRUE

**Web/Jetty Access (Serving):**
- `Jetty-WarFragmentFolderPath: /` makes all resources web-accessible
- Files at `WEB-INF/src/web/` follow standard iDempiere convention
- Both paths work for actual CSS delivery

### JAR Verification

After building, verify both paths exist in JAR:

```bash
unzip -l com.cloudempiere.ai.theme-10.0.1-SNAPSHOT.jar | grep custom.css.dsp
```

Expected output:
```
4901  web/theme/default/css/fragment/custom.css.dsp
4901  web/theme/cloudempiere/css/fragment/custom.css.dsp
4901  WEB-INF/src/web/theme/default/css/fragment/custom.css.dsp
4901  WEB-INF/src/web/theme/cloudempiere/css/fragment/custom.css.dsp
```

## Key Files Modified

1. **Created:** `web/theme/default/css/fragment/custom.css.dsp`
2. **Created:** `web/theme/cloudempiere/css/fragment/custom.css.dsp`
3. **Updated:** `build.properties` - added `web/` to `bin.includes`
4. **Verified:** `META-INF/MANIFEST.MF` - kept `Bundle-ClassPath: .` (no theme/ prefix)

## References

- iDempiere NF8.2: https://wiki.idempiere.org/en/NF8.2_Lightweight_theme_customization
- Official Example: https://github.com/CarlosRuiz-globalqss/idempiere.example.custom.theme
- ThemeManager Source: `org.adempiere.ui.zk.theme.ThemeManager.java`

## Testing

1. **Build:** `mvn clean package`
2. **Deploy:** Copy JAR to iDempiere plugins directory
3. **Verify Detection:**
   - Debug breakpoint in `ThemeManager.isThemeHasCustomCSSFragment()`
   - Confirm `ThemeManager.class.getResource()` returns non-null
4. **Verify Rendering:**
   - Browser DevTools → Network → theme.css.dsp
   - Search for AI custom classes (e.g., `.ai-chat-panel`)

## Troubleshooting

**Fragment not detected:**
- Check `Bundle-ClassPath` is `.` (not `., theme/`)
- Verify `web/` is in `build.properties` `bin.includes`
- Rebuild: `mvn clean package`
- Check JAR contents with `unzip -l`

**CSS not rendering:**
- Clear browser cache
- Refresh bundle in Felix console: `refresh [bundle-id]`
- Check theme.css.dsp source in browser DevTools
- Verify fragment is attached: OSGi console → `inspect capability service [host-bundle-id]`
