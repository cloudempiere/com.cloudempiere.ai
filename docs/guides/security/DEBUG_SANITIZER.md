# Debug Sanitizer Issue

## Test Case

```java
String input = "<div>Welcome to ERP & CRM in the cloud</div>";
String output = MarkdownSyntaxSanitizer.sanitize(input);
System.out.println("Input: " + input);
System.out.println("Output: " + output);
```

## Expected Output
```
Input: <div>Welcome to ERP & CRM in the cloud</div>
Output: &lt;div&gt;Welcome to ERP &amp; CRM in the cloud&lt;/div&gt;
```

## Hypothesis

The issue could be:

1. **Code block protector protecting HTML** - If the AI response contains HTML in what looks like a code block pattern
2. **Double-decoding in ZK** - ZK Html component decoding entities
3. **Regex replacement issue** - Matcher.appendReplacement not working as expected

## Quick Fix

The problem is likely that we need to **NOT** escape HTML in the sanitizer, since the StreamingMarkdownRenderer already handles escaping. Instead, we should:

1. **Remove/Strip** HTML tags entirely (not escape them)
2. **OR** Let the markdown renderer handle the escaping

The issue is we're escaping too early, and then something downstream is un-escaping.

## Immediate Solution

Change `MarkdownSyntaxSanitizer.escapeRawHtml()` to:

```java
private static String escapeRawHtml(String markdown) {
    // REMOVE HTML tags entirely instead of escaping
    return HTML_TAG_PATTERN.matcher(markdown).replaceAll("");
}
```

This way:
- `<div>Welcome</div>` → `Welcome`
- No HTML tags at all
- No double-escaping issues
- Markdown renderer handles remaining content safely
