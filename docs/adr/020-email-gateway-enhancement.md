# ADR-020: Email Gateway Enhancement Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: CloudEmpiere AI Team
**Related**: [ADR-019](019-support-ticket-classification.md), [ADR-002](002-langchain4j-strategic-adoption.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Inbound emails to support require manual cleanup:
- Email signatures and footers clutter tickets
- Small logos and images add noise
- Inconsistent formatting makes reading difficult
- Key information (order numbers, dates) buried in text
- Original email often needs reformatting for ticket display

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Email reading time | 2-3 min | <1 min | 60% reduction |
| Entity extraction | Manual | Automatic | Time savings |
| Signature removal accuracy | 0% (manual) | 95%+ | Clean tickets |
| Format consistency | Variable | Standardized | Readability |

### Relationship to ADR-019

This ADR complements ADR-019 (Support Ticket Classification):
- ADR-019 classifies the **content** (type, priority, category)
- ADR-020 cleans the **format** (signatures, images, entities)

The `EmailParser` from ADR-019 is extended here with additional cleaning capabilities.

## Decision

Implement an **Email Gateway Enhancement** feature that automatically cleans, formats, and enriches inbound emails before they reach support agents.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Raw Inbound Email                                │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  Subject: Re: Order Problem                                      ││
│  │  From: john@customer.com                                         ││
│  │  Body: <HTML with inline images, signature, formatting>          ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  EmailEnhancementAgent                                │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Processing Pipeline:                                            │  │
│  │   1. SignatureDetector - ML/pattern-based signature removal     │  │
│  │   2. ImageFilter - Remove <5KB images, keep documents           │  │
│  │   3. TextCleaner - Fix whitespace, encoding, excessive breaks   │  │
│  │   4. EntityExtractor - Order IDs, SKUs, dates, amounts          │  │
│  │   5. GrammarEnhancer - Optional light corrections               │  │
│  │   6. OutputFormatter - Standardize for ticket display           │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Enhanced Email Result                              │
│  {                                                                    │
│    "cleaned_body": "Clean, readable email text...",                  │
│    "signature_detected": true,                                       │
│    "signature_removed": "Best regards, John...",                     │
│    "images_removed": 2,                                              │
│    "images_kept": 1,                                                 │
│    "extracted_entities": {                                           │
│      "order_numbers": ["SO-1234"],                                   │
│      "skus": ["SKU-ABC123"],                                         │
│      "dates": ["2025-01-15"],                                        │
│      "amounts": ["$1,500.00"]                                        │
│    },                                                                │
│    "grammar_issues_fixed": 3,                                        │
│    "original_preserved": true,                                       │
│    "processing_steps": ["Signature removed", "2 logos removed"]      │
│  }                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Processing Pipeline (LangGraph-inspired)

```
┌─────────────────┐
│   Raw Email     │
│   Input         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 1. HTML Parser  │ ──▶ Convert HTML to plaintext
│    (jsoup)      │     Preserve links, tables
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. Signature    │ ──▶ Pattern matching + position analysis
│    Detector     │     Confidence score for each candidate
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. Image        │ ──▶ Size threshold (5KB)
│    Filter       │     Keep: attachments, screenshots
│                 │     Remove: logos, avatars, tracking pixels
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. Text         │ ──▶ Normalize whitespace
│    Cleaner      │     Fix encoding issues
│                 │     Remove excessive line breaks
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. Entity       │ ──▶ Regex + NER patterns
│    Extractor    │     Order IDs, SKUs, dates, amounts
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 6. Grammar      │ ──▶ Optional (configurable)
│    Enhancer     │     Light corrections only
│    (AI)         │     Never change meaning
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 7. Output       │ ──▶ Standardized format
│    Formatter    │     Ticket-ready display
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Enhanced       │
│  Email Result   │
└─────────────────┘
```

### System Prompt (for Grammar Enhancement)

```
You are an email grammar enhancement assistant.

TASK: Improve email text readability with light corrections.

INPUT:
- Email body (already cleaned of signature)

RULES:
1. Fix obvious typos and spelling errors
2. Fix grammar issues (subject-verb agreement, tense)
3. Fix punctuation (missing periods, comma splices)
4. DO NOT change meaning
5. DO NOT add or remove information
6. DO NOT change business terms or names
7. DO NOT make stylistic changes
8. Preserve original tone (formal/informal)

OUTPUT FORMAT (JSON only):
{
  "enhanced_text": "The corrected email text",
  "changes_made": [
    {"original": "recieved", "corrected": "received"},
    {"original": "there product", "corrected": "their product"}
  ],
  "changes_count": 2
}

EXAMPLES:

Input: "We recieved the product but there was a issue with the invoice."
Output: {
  "enhanced_text": "We received the product but there was an issue with the invoice.",
  "changes_made": [
    {"original": "recieved", "corrected": "received"},
    {"original": "a issue", "corrected": "an issue"}
  ],
  "changes_count": 2
}

Input: "The order SO-1234 need to be shiped urgently!!!"
Output: {
  "enhanced_text": "The order SO-1234 needs to be shipped urgently.",
  "changes_made": [
    {"original": "need", "corrected": "needs"},
    {"original": "shiped", "corrected": "shipped"},
    {"original": "urgently!!!", "corrected": "urgently."}
  ],
  "changes_count": 3
}
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.mvp;

@Agent(
    name = "EmailEnhancementAgent",
    domain = "SUPPORT",
    riskLevel = RiskLevel.LOW
)
public interface EmailEnhancementAgent {

    @SystemMessage(fromResource = "prompts/email-grammar.txt")
    GrammarResult enhanceGrammar(
        @UserMessage String emailBody
    );
}

public record EnhancedEmail(
    @Description("Cleaned email body") String cleaned_body,
    @Description("Was signature detected?") boolean signature_detected,
    @Description("Removed signature text") String signature_removed,
    @Description("Number of images removed") int images_removed,
    @Description("Number of images kept") int images_kept,
    @Description("Extracted entities") ExtractedEntities extracted_entities,
    @Description("Grammar issues fixed") int grammar_issues_fixed,
    @Description("Original email preserved") boolean original_preserved,
    @Description("Processing steps performed") List<String> processing_steps
) {}

public record GrammarResult(
    @Description("Enhanced text") String enhanced_text,
    @Description("Changes made") List<GrammarChange> changes_made,
    @Description("Number of changes") int changes_count
) {}

public record GrammarChange(
    String original,
    String corrected
) {}
```

### Signature Detector

```java
public class SignatureDetector {

    private static final double CONFIDENCE_THRESHOLD = 0.80;

    /**
     * Common signature start patterns
     */
    private static final List<Pattern> SIGNATURE_PATTERNS = List.of(
        // Explicit signature markers
        Pattern.compile("(?m)^--\\s*$"),                    // -- on its own line
        Pattern.compile("(?m)^_{3,}\\s*$"),                 // ___ divider
        Pattern.compile("(?m)^-{3,}\\s*$"),                 // --- divider

        // Closing phrases (English)
        Pattern.compile("(?mi)^(Best|Kind|Warm)\\s+regards,?\\s*$"),
        Pattern.compile("(?mi)^Sincerely,?\\s*$"),
        Pattern.compile("(?mi)^Thanks,?\\s*$"),
        Pattern.compile("(?mi)^Thank you,?\\s*$"),
        Pattern.compile("(?mi)^Cheers,?\\s*$"),
        Pattern.compile("(?mi)^Regards,?\\s*$"),

        // Closing phrases (German)
        Pattern.compile("(?mi)^Mit freundlichen Grüßen,?\\s*$"),
        Pattern.compile("(?mi)^Viele Grüße,?\\s*$"),
        Pattern.compile("(?mi)^Beste Grüße,?\\s*$"),

        // Mobile signatures
        Pattern.compile("(?mi)^Sent from my (iPhone|Android|iPad|mobile).*$"),
        Pattern.compile("(?mi)^Get Outlook for.*$"),
        Pattern.compile("(?mi)^Sent from Mail for Windows.*$"),

        // Legal disclaimers
        Pattern.compile("(?mi)^(CONFIDENTIAL|DISCLAIMER|This email and any attachments).*$"),
        Pattern.compile("(?mi)^If you have received this email in error.*$")
    );

    /**
     * Detect and remove signature from email body
     */
    public SignatureResult detectAndRemove(String body) {
        String[] lines = body.split("\n");
        int signatureStartLine = -1;
        double maxConfidence = 0.0;

        // Scan from bottom up (signatures usually at end)
        for (int i = lines.length - 1; i >= 0; i--) {
            SignatureCandidate candidate = analyzeLineAsSignatureStart(lines, i);
            if (candidate.confidence > maxConfidence) {
                maxConfidence = candidate.confidence;
                signatureStartLine = i;
            }
        }

        if (signatureStartLine >= 0 && maxConfidence >= CONFIDENCE_THRESHOLD) {
            // Extract body (before signature)
            String cleanedBody = String.join("\n",
                Arrays.copyOfRange(lines, 0, signatureStartLine)).trim();

            // Extract signature
            String signature = String.join("\n",
                Arrays.copyOfRange(lines, signatureStartLine, lines.length)).trim();

            return new SignatureResult(
                cleanedBody,
                signature,
                true,
                maxConfidence
            );
        }

        return new SignatureResult(body, null, false, 0.0);
    }

    /**
     * Analyze if a line could be the start of a signature
     */
    private SignatureCandidate analyzeLineAsSignatureStart(String[] lines, int lineIndex) {
        String line = lines[lineIndex].trim();
        double confidence = 0.0;

        // Check pattern matches
        for (Pattern pattern : SIGNATURE_PATTERNS) {
            if (pattern.matcher(line).matches()) {
                confidence += 0.4;
                break;
            }
        }

        // Position heuristics (signature usually in last 30% of email)
        double positionRatio = (double) lineIndex / lines.length;
        if (positionRatio > 0.7) {
            confidence += 0.2;
        }

        // Line count after this line (signatures usually 3-10 lines)
        int linesAfter = lines.length - lineIndex;
        if (linesAfter >= 2 && linesAfter <= 15) {
            confidence += 0.2;
        }

        // Check for name-like pattern in next line (e.g., "John Smith")
        if (lineIndex + 1 < lines.length) {
            String nextLine = lines[lineIndex + 1].trim();
            if (looksLikeName(nextLine)) {
                confidence += 0.2;
            }
        }

        // Check for contact info patterns in following lines
        if (hasContactInfo(lines, lineIndex)) {
            confidence += 0.2;
        }

        return new SignatureCandidate(lineIndex, Math.min(confidence, 1.0));
    }

    private boolean looksLikeName(String line) {
        // Simple heuristic: 2-4 capitalized words
        return line.matches("^([A-Z][a-z]+\\s*){2,4}$");
    }

    private boolean hasContactInfo(String[] lines, int startIndex) {
        // Look for phone, email, or address patterns in following lines
        Pattern contactPattern = Pattern.compile(
            "(?i)(phone|tel|mobile|email|@|www\\.|http|\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4})"
        );

        for (int i = startIndex; i < Math.min(startIndex + 10, lines.length); i++) {
            if (contactPattern.matcher(lines[i]).find()) {
                return true;
            }
        }
        return false;
    }
}

public record SignatureResult(
    String cleanedBody,
    String signature,
    boolean signatureDetected,
    double confidence
) {}
```

### Image Filter

```java
public class EmailImageFilter {

    private static final int MAX_INLINE_IMAGE_SIZE = 5 * 1024; // 5KB

    /**
     * Filter images from HTML email
     * Keep: Large images, attachments, screenshots
     * Remove: Logos, avatars, tracking pixels
     */
    public ImageFilterResult filterImages(String htmlBody, List<Attachment> attachments) {
        Document doc = Jsoup.parse(htmlBody);
        List<String> removedImages = new ArrayList<>();
        List<String> keptImages = new ArrayList<>();

        // Process inline images (<img> tags)
        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            int size = estimateImageSize(img, src);

            if (shouldRemoveImage(img, size)) {
                img.remove();
                removedImages.add(describeImage(img, src));
            } else {
                keptImages.add(describeImage(img, src));
            }
        }

        // Process attachments
        List<Attachment> keptAttachments = new ArrayList<>();
        for (Attachment att : attachments) {
            if (isRelevantAttachment(att)) {
                keptAttachments.add(att);
            }
        }

        return new ImageFilterResult(
            doc.body().html(),
            removedImages,
            keptImages,
            keptAttachments
        );
    }

    private boolean shouldRemoveImage(Element img, int size) {
        // Remove if too small (likely logo/avatar)
        if (size > 0 && size < MAX_INLINE_IMAGE_SIZE) {
            return true;
        }

        // Remove tracking pixels (1x1)
        String width = img.attr("width");
        String height = img.attr("height");
        if ("1".equals(width) || "1".equals(height)) {
            return true;
        }

        // Remove common logo patterns
        String src = img.attr("src").toLowerCase();
        if (src.contains("logo") ||
            src.contains("avatar") ||
            src.contains("signature") ||
            src.contains("tracking") ||
            src.contains("pixel")) {
            return true;
        }

        // Remove base64 small images
        if (src.startsWith("data:image") && size < MAX_INLINE_IMAGE_SIZE) {
            return true;
        }

        return false;
    }

    private boolean isRelevantAttachment(Attachment att) {
        // Keep documents
        String mimeType = att.getMimeType().toLowerCase();
        if (mimeType.contains("pdf") ||
            mimeType.contains("word") ||
            mimeType.contains("excel") ||
            mimeType.contains("spreadsheet")) {
            return true;
        }

        // Keep large images (likely screenshots/evidence)
        if (mimeType.contains("image") && att.getSize() > MAX_INLINE_IMAGE_SIZE) {
            return true;
        }

        return false;
    }
}
```

### Configuration

```java
public class EmailEnhancementConfig {

    /**
     * Per-tenant configuration for email enhancement
     */
    @Column(name = "IsSignatureRemovalEnabled")
    private boolean signatureRemovalEnabled = true;

    @Column(name = "IsImageFilteringEnabled")
    private boolean imageFilteringEnabled = true;

    @Column(name = "IsGrammarEnhancementEnabled")
    private boolean grammarEnhancementEnabled = false;  // Optional, off by default

    @Column(name = "IsEntityExtractionEnabled")
    private boolean entityExtractionEnabled = true;

    @Column(name = "PreserveOriginalEmail")
    private boolean preserveOriginalEmail = true;  // Always recommended

    @Column(name = "SignatureConfidenceThreshold")
    private double signatureConfidenceThreshold = 0.80;

    @Column(name = "MaxImageSizeBytes")
    private int maxImageSizeBytes = 5 * 1024;  // 5KB
}
```

### Integration with ADR-019

```java
public class EmailEnhancementService {

    private final SignatureDetector signatureDetector;
    private final EmailImageFilter imageFilter;
    private final TextCleaner textCleaner;
    private final EntityExtractor entityExtractor;
    private final EmailEnhancementAgent grammarAgent;
    private final EmailEnhancementConfig config;

    /**
     * Full email enhancement pipeline
     */
    public EnhancedEmail enhance(String rawEmail, List<Attachment> attachments) {
        List<String> processingSteps = new ArrayList<>();

        // 1. Parse HTML to text
        String body = parseHtmlToText(rawEmail);

        // 2. Signature detection and removal
        SignatureResult sigResult = null;
        if (config.isSignatureRemovalEnabled()) {
            sigResult = signatureDetector.detectAndRemove(body);
            body = sigResult.cleanedBody();
            if (sigResult.signatureDetected()) {
                processingSteps.add("Signature removed (confidence: " +
                    String.format("%.0f%%", sigResult.confidence() * 100) + ")");
            }
        }

        // 3. Image filtering
        ImageFilterResult imgResult = null;
        if (config.isImageFilteringEnabled()) {
            imgResult = imageFilter.filterImages(rawEmail, attachments);
            if (!imgResult.removedImages().isEmpty()) {
                processingSteps.add(imgResult.removedImages().size() + " small images removed");
            }
        }

        // 4. Text cleaning
        body = textCleaner.clean(body);
        processingSteps.add("Text cleaned");

        // 5. Entity extraction
        ExtractedEntities entities = null;
        if (config.isEntityExtractionEnabled()) {
            entities = entityExtractor.extractEntities(body);
            if (!entities.isEmpty()) {
                processingSteps.add("Entities extracted: " + entities.summary());
            }
        }

        // 6. Grammar enhancement (optional)
        int grammarFixes = 0;
        if (config.isGrammarEnhancementEnabled()) {
            GrammarResult grammarResult = grammarAgent.enhanceGrammar(body);
            body = grammarResult.enhanced_text();
            grammarFixes = grammarResult.changes_count();
            if (grammarFixes > 0) {
                processingSteps.add(grammarFixes + " grammar issues fixed");
            }
        }

        return new EnhancedEmail(
            body,
            sigResult != null && sigResult.signatureDetected(),
            sigResult != null ? sigResult.signature() : null,
            imgResult != null ? imgResult.removedImages().size() : 0,
            imgResult != null ? imgResult.keptImages().size() : 0,
            entities,
            grammarFixes,
            config.isPreserveOriginalEmail(),
            processingSteps
        );
    }
}

// Integration with TicketCreator from ADR-019
public class EnhancedTicketCreator extends TicketCreator {

    private final EmailEnhancementService enhancementService;

    @Override
    public MRequest createTicket(ParsedEmail email, TicketClassification classification) {
        // Enhance email first
        EnhancedEmail enhanced = enhancementService.enhance(
            email.originalBody(),
            email.attachments()
        );

        // Create ticket with enhanced content
        MRequest request = super.createTicket(email, classification);

        // Add enhanced body as display version
        request.set_ValueOfColumn("EnhancedDescription", enhanced.cleaned_body());

        // Add extracted entities as references
        addEntityReferences(request, enhanced.extracted_entities());

        // Store processing metadata
        request.set_ValueOfColumn("EmailProcessingSteps",
            String.join(", ", enhanced.processing_steps()));

        return request;
    }
}
```

## Implementation Plan

### Phase 1: Core Processing (Days 1-2)

| Task | Effort | Owner |
|------|--------|-------|
| Create `SignatureDetector` | 6h | Dev |
| Create `EmailImageFilter` | 4h | Dev |
| Create `TextCleaner` | 2h | Dev |
| Unit tests for each component | 4h | Dev |

### Phase 2: Enhancement Service (Days 3-4)

| Task | Effort | Owner |
|------|--------|-------|
| Create `EmailEnhancementAgent` (grammar) | 4h | Dev |
| Create `EmailEnhancementService` pipeline | 4h | Dev |
| Create configuration model | 2h | Dev |
| Integration with ADR-019 `TicketCreator` | 4h | Dev |

### Phase 3: Testing & Config (Days 5-6)

| Task | Effort | Owner |
|------|--------|-------|
| Admin configuration UI | 4h | Dev |
| Test with 200 real emails | 6h | QA |
| Signature detection accuracy (95%+ target) | 4h | QA |
| Grammar enhancement validation | 2h | QA |

### Deliverables

- `SignatureDetector.java` (~200 lines)
- `EmailImageFilter.java` (~150 lines)
- `TextCleaner.java` (~80 lines)
- `EmailEnhancementAgent.java` (~50 lines)
- `EmailEnhancementService.java` (~150 lines)
- `EmailEnhancementConfig.java` (~50 lines)
- `email-grammar.txt` (system prompt)
- Admin configuration window
- Unit and integration tests

**Total Effort**: 6 days
**Risk Level**: Low (mostly pattern-based processing)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Remove email signatures automatically (95%+ accuracy)
  - [ ] Strip small logos and images (<5KB)
  - [ ] Extract key entities (order IDs, dates, amounts)
  - [ ] Optional grammar enhancement (configurable)
  - [ ] Maintain original in "View Raw" option
  - [ ] Per-tenant configuration

Non-Functional:
  - [ ] Processing time: <1 second per email
  - [ ] Signature detection accuracy: 95%+
  - [ ] False positive rate: <5% (don't remove real content)
  - [ ] Memory efficient (stream processing)

Security:
  - [ ] Original emails never lost
  - [ ] Processing logged for audit
  - [ ] Grammar AI never sees sensitive fields
```

## Consequences

### Positive

1. **Clean Tickets**: No more signature clutter
2. **Time Savings**: Agents focus on content
3. **Entity Visibility**: Key info highlighted
4. **Consistency**: Standardized format
5. **Configurable**: Per-tenant settings

### Negative

1. **False Positives**: May occasionally remove real content
2. **Grammar Risk**: AI changes could alter meaning
3. **Processing Time**: Adds latency to email handling

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Remove real content | MEDIUM | Conservative threshold, preserve original |
| Grammar changes meaning | LOW | Light corrections only, disable by default |
| Slow processing | LOW | Optimize patterns, skip grammar if slow |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Signature removal accuracy | 95%+ | Manual review |
| False positive rate | <5% | Manual review |
| Processing time | <1 second | Performance logs |
| Agent satisfaction | 4.0/5 | Survey |
| Grammar errors fixed | 80%+ | Review sample |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-019: Support Ticket Classification](019-support-ticket-classification.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) - Reference implementation

---

*ADR-020 | Version 1.0 | 2025-12-03*
