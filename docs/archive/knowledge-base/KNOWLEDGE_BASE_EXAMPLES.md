# Knowledge Base Agent - Practical Examples

## Example 1: Basic Usage - Adding Docker Deployment Guide

```java
// Setup
Properties ctx = Env.getCtx();
IAIProvider aiProvider = getAnthropicProvider(); // Your AI provider instance
KnowledgeBaseAgent kbAgent = new KnowledgeBaseAgent(aiProvider);

// User input
String newContent = """
# Docker Deployment Guide

## Prerequisites
- Docker Desktop installed (v20.10+)
- Docker Compose installed (v1.29+)
- 4GB RAM minimum, 8GB recommended
- Stable internet connection

## Installation Steps

1. Pull the Cloudempiere image:
   ```bash
   docker pull cloudempiere/cloudempiere:latest
   ```

2. Create docker-compose.yml:
   ```yaml
   version: '3.8'
   services:
     db:
       image: postgres:14
       environment:
         POSTGRES_DB: idempiere
         POSTGRES_PASSWORD: admin123
     cloudempiere:
       image: cloudempiere/cloudempiere:latest
       depends_on:
         - db
       ports:
         - "8080:8080"
   ```

3. Deploy:
   ```bash
   docker-compose up -d
   ```

## Troubleshooting
- Port 8080 already in use? Use port mapping: 8081:8080
- Database connection errors? Wait 30s for DB startup
- Memory issues? Increase docker desktop RAM limit
""";

String title = "Docker Deployment Guide";

// Analyze placement
PlacementRecommendation rec = kbAgent.analyzePlacement(
    ctx,
    "DEPLOYMENT",  // KB type
    newContent,
    title
);

// Display results
System.out.println("═══════════════════════════════════════════════");
System.out.println("Knowledge Base Placement Recommendation");
System.out.println("═══════════════════════════════════════════════");
System.out.println("\n📋 Recommendation: " + rec.getAction());
System.out.println("✅ Confidence:     " + rec.getConfidencePercent() + "%");
System.out.println("\n📝 Reasoning:");
System.out.println(rec.getReasoning());

if (!rec.getBestPractices().isEmpty()) {
    System.out.println("\n✨ Best Practices Applied:");
    for (String practice : rec.getBestPractices()) {
        System.out.println("  • " + practice);
    }
}

if (rec.getWarningMessage() != null) {
    System.out.println("\n⚠️  Warning: " + rec.getWarningMessage());
}

// Expected Output:
// ═══════════════════════════════════════════════
// Knowledge Base Placement Recommendation
// ═══════════════════════════════════════════════
//
// 📋 Recommendation: CREATE_NEW
// ✅ Confidence:     87%
//
// 📝 Reasoning:
// This is a new, comprehensive Docker deployment guide. The knowledge base
// has Docker-related content but not a dedicated Docker guide. Creating a
// new entry under "Deployment → Container Platforms" makes sense.
//
// ✨ Best Practices Applied:
//   • Sequential structure (prerequisites → installation → troubleshooting)
//   • Code examples with proper formatting
//   • Clear prerequisites section
//   • Troubleshooting at the end
//
// No warnings detected.
```

## Example 2: Duplicate Detection - Kubernetes Addition

```java
String newContent = """
# Kubernetes Deployment

## Prerequisites
- Kubernetes cluster (v1.20+)
- kubectl CLI installed
- Docker images pushed to registry

## Helm Chart Installation

1. Add Cloudempiere helm repo:
   ```bash
   helm repo add cloudempiere https://charts.cloudempiere.io
   ```

2. Install:
   ```bash
   helm install cloudempiere cloudempiere/cloudempiere
   ```

## Scaling & High Availability
- Configure replicas: 3 minimum for production
- Use persistent volumes for database
- Set resource requests/limits
""";

String title = "Kubernetes Deployment";

PlacementRecommendation rec = kbAgent.analyzePlacement(
    ctx,
    "DEPLOYMENT",
    newContent,
    title
);

// Expected Scenario: Similar to Docker deployment found
System.out.println("Recommendation: " + rec.getAction());
// → EXTEND_EXISTING (add Kubernetes subsection to Docker guide)

System.out.println("Target Entry: " + rec.getTargetEntryId());
// → 1234 (Docker Deployment Guide)

System.out.println("Confidence: " + rec.getConfidencePercent() + "%");
// → 75% (Somewhat similar but can stand alone)

System.out.println("Reasoning: " + rec.getReasoning());
// → "While Kubernetes is related to Docker deployments, it's a distinct
//     orchestration platform. Recommend creating subsection 'Kubernetes'
//     under existing 'Container Platforms' parent."
```

## Example 3: Editor.js Parsing Example

```java
// Editor.js JSON content (from iDempiere k_entry.content field)
String editorJsContent = """
{
  "blocks": [
    {
      "type": "heading",
      "data": {
        "level": 1,
        "text": "Configuration Best Practices"
      }
    },
    {
      "type": "paragraph",
      "data": {
        "text": "This guide covers best practices for Cloudempiere configuration."
      }
    },
    {
      "type": "list",
      "data": {
        "style": "unordered",
        "items": [
          "Use environment variables for sensitive data",
          "Enable audit logging for compliance",
          "Regular database backups (daily minimum)"
        ]
      }
    },
    {
      "type": "code",
      "data": {
        "code": "# Example configuration\\nDATABASE_URL=jdbc:postgresql://db:5432/idempiere\\nDATABASE_USER=admin\\nLOG_LEVEL=INFO",
        "language": "bash"
      }
    },
    {
      "type": "quote",
      "data": {
        "text": "Configuration is the foundation of stable production deployments",
        "caption": "Cloudempiere DevOps Guide"
      }
    }
  ]
}
""";

// Parse to markdown
String markdown = EditorJsParser.parseToMarkdown(editorJsContent);

System.out.println(markdown);
// Output:
// # Configuration Best Practices
//
// This guide covers best practices for Cloudempiere configuration.
//
// - Use environment variables for sensitive data
// - Enable audit logging for compliance
// - Regular database backups (daily minimum)
//
// ```bash
// # Example configuration
// DATABASE_URL=jdbc:postgresql://db:5432/idempiere
// DATABASE_USER=admin
// LOG_LEVEL=INFO
// ```
//
// > Configuration is the foundation of stable production deployments
// > — Cloudempiere DevOps Guide

// For similarity analysis, convert to plain text
String plainText = EditorJsParser.markdownToPlainText(markdown);
System.out.println(plainText);
// Output:
// Configuration Best Practices
//
// This guide covers best practices for Cloudempiere configuration.
//
// Use environment variables for sensitive data
// Enable audit logging for compliance
// Regular database backups (daily minimum)
//
// Example configuration
// DATABASE_URL=jdbc:postgresql://db:5432/idempiere
// DATABASE_USER=admin
// LOG_LEVEL=INFO
//
// Configuration is the foundation of stable production deployments
// Cloudempiere DevOps Guide
```

## Example 4: Similarity Analysis Deep Dive

```java
String userNewContent = "How to integrate with OAuth2 for SSO";

// Load KB
KnowledgeBaseContextProvider provider =
    (KnowledgeBaseContextProvider) AIContextProviderRegistry
        .getInstance()
        .getProvider("KNOWLEDGE_BASE");

ContextParameters params = ContextParameters.forKnowledgeBase("SECURITY");
JSONObject contextJson = provider.extractContext(ctx, 0, params);

// Parse hierarchy (simplified)
KnowledgeBaseHierarchy hierarchy = loadHierarchy(contextJson);

// Analyze similarity
SimilarityResult result = KnowledgeBaseSimilarityAnalyzer
    .analyzeSimilarity(userNewContent, hierarchy);

// Check for issues
if (result.isHasDuplicate()) {
    System.out.println("⚠️  DUPLICATE CONTENT DETECTED!");
    System.out.println("\nPotential duplicates:");
    for (SimilarityResult.SimilarEntry similar : result.getTopSimilar(3)) {
        System.out.println(String.format(
            "  • %s (%.0f%% match) - %s",
            similar.getEntry().getTitle(),
            similar.getSimilarityScore() * 100,
            similar.getReason()
        ));
    }
} else if (result.isHasSimilar()) {
    System.out.println("📚 Similar entries found:");
    for (SimilarityResult.SimilarEntry similar : result.getTopSimilar(5)) {
        System.out.println(String.format(
            "  • %s (%.0f%% match)",
            similar.getEntry().getTitle(),
            similar.getSimilarityScore() * 100
        ));
    }
} else {
    System.out.println("✅ No similar content found - safe to add");
}

// Expected output might be:
// 📚 Similar entries found:
//  • LDAP Authentication Setup (68% match)
//  • Single Sign-On Overview (65% match)
//  • OAuth2 Concepts (62% match)
```

## Example 5: Full Integration in Dialog/Form

```java
public class KnowledgeBaseContentDialog extends AbstractADWindow {

    private KnowledgeBaseAgent kbAgent;
    private PlacementRecommendation lastRecommendation;

    public void onAddNewContent() {
        String selectedKbType = getSelectedKbType();  // User selects KB
        String userContent = getEditorContent();       // From text editor
        String userTitle = getTitleField().getText(); // From title field

        try {
            // Analyze placement
            showLoadingMessage("Analyzing knowledge base structure...");
            lastRecommendation = kbAgent.analyzePlacement(
                Env.getCtx(),
                selectedKbType,
                userContent,
                userTitle
            );

            // Display recommendation
            displayRecommendationPanel(lastRecommendation);

        } catch (Exception e) {
            showError("Failed to analyze placement: " + e.getMessage());
        }
    }

    private void displayRecommendationPanel(PlacementRecommendation rec) {
        String actionText = switch(rec.getAction()) {
            case CREATE_NEW -> "Create new entry";
            case EXTEND_EXISTING -> "Add to existing entry #" + rec.getTargetEntryId();
            case UPDATE_EXISTING -> "Update existing entry #" + rec.getTargetEntryId();
            case DUPLICATE_WARNING -> "⚠️  Potential duplicate detected";
            default -> "Manual review needed";
        };

        StringBuilder panel = new StringBuilder();
        panel.append("═══════════════════════════════════════════\n");
        panel.append("AI Placement Recommendation\n");
        panel.append("═══════════════════════════════════════════\n\n");

        panel.append("🎯 Action: ").append(actionText).append("\n");
        panel.append("✅ Confidence: ").append(rec.getConfidencePercent()).append("%\n");

        if (rec.isHighConfidence()) {
            panel.append("   🟢 High confidence - recommended to follow\n");
        } else {
            panel.append("   🟡 Low confidence - review before proceeding\n");
        }

        panel.append("\n📝 Reasoning:\n");
        panel.append(wrapText(rec.getReasoning(), 70)).append("\n");

        if (!rec.getBestPractices().isEmpty()) {
            panel.append("\n✨ Best Practices:\n");
            for (String practice : rec.getBestPractices()) {
                panel.append("  • ").append(practice).append("\n");
            }
        }

        if (rec.getWarningMessage() != null) {
            panel.append("\n⚠️  WARNING: ").append(rec.getWarningMessage()).append("\n");
        }

        // Show dialog with option to approve/modify
        int result = showRecommendationDialog(
            panel.toString(),
            new String[]{"Approve & Save", "Modify & Save", "Cancel"}
        );

        if (result == 0) {
            // User approved
            saveToKnowledgeBase(lastRecommendation, userContent, userTitle);
        } else if (result == 1) {
            // User wants to modify
            showModificationPanel(lastRecommendation);
        }
    }

    private void saveToKnowledgeBase(PlacementRecommendation rec,
                                     String content,
                                     String title) {
        try {
            if (rec.getAction() == PlacementRecommendation.Action.CREATE_NEW) {
                // Insert new k_entry
                int parentId = rec.getSuggestedParentId();
                insertNewKEntry(title, content, parentId);
                showMessage("Entry created successfully!");

            } else if (rec.getAction() == PlacementRecommendation.Action.EXTEND_EXISTING) {
                // Append to existing entry
                int targetId = rec.getTargetEntryId();
                appendToKEntry(targetId, content);
                showMessage("Content added to existing entry!");
            }

            // Refresh KB view
            refreshKnowledgeBaseView();

        } catch (Exception e) {
            showError("Failed to save: " + e.getMessage());
        }
    }
}
```

## Example 6: Recommendation Confidence Matrix

```java
// Different confidence scenarios:

// SCENARIO 1: High confidence - obvious placement
PlacementRecommendation rec1 = kbAgent.analyzePlacement(ctx, "DEPLOYMENT",
    "How to Deploy Cloudempiere on AWS", "AWS Deployment");

// Expected: 90%+ confidence, CREATE_NEW under Deployment → Cloud
System.out.println("Scenario 1 Confidence: " + rec1.getConfidencePercent() + "%");
System.out.println("Action: " + rec1.getAction());
System.out.println("Reasoning: Clear topic, fits existing structure");

// SCENARIO 2: Medium confidence - somewhat related
rec = kbAgent.analyzePlacement(ctx, "DEPLOYMENT",
    "Database optimization for Cloudempiere", "DB Optimization");

// Expected: 60-70% confidence, possibly EXTEND_EXISTING or CREATE_NEW
System.out.println("\nScenario 2 Confidence: " + rec.getConfidencePercent() + "%");

// SCENARIO 3: Low confidence - ambiguous placement
rec = kbAgent.analyzePlacement(ctx, "GENERAL",
    "New feature overview", "Feature Overview");

// Expected: 40-50% confidence, recommend user review
System.out.println("\nScenario 3 Confidence: " + rec.getConfidencePercent() + "%");
if (!rec.isHighConfidence()) {
    System.out.println("Recommendation: Manual review suggested");
}

// SCENARIO 4: Duplicate warning
rec = kbAgent.analyzePlacement(ctx, "DEPLOYMENT",
    "Complete Docker deployment guide with all steps...",
    "Docker Setup");

// Expected: DUPLICATE_WARNING action, low confidence
System.out.println("\nScenario 4 Action: " + rec.getAction());
if (rec.getAction() == PlacementRecommendation.Action.DUPLICATE_WARNING) {
    System.out.println("⚠️  Found duplicate: " + rec.getWarningMessage());
}
```

## Example 7: Error Handling

```java
private PlacementRecommendation safeAnalyzePlacement(
    Properties ctx,
    String kType,
    String content,
    String title
) {
    try {
        // Main flow
        return kbAgent.analyzePlacement(ctx, kType, content, title);

    } catch (IllegalStateException e) {
        // Context provider not registered
        log.warning("KB context provider not available: " + e.getMessage());
        return createDefaultRecommendation();

    } catch (Exception e) {
        // AI call failed, network error, etc.
        log.log(Level.SEVERE, "KB analysis failed", e);

        // Return safe default
        PlacementRecommendation rec = new PlacementRecommendation();
        rec.setAction(PlacementRecommendation.Action.CREATE_NEW);
        rec.setConfidence(0.3); // Low confidence due to error
        rec.setReasoning("Unable to analyze KB structure. " +
                        "Manual review recommended: " + e.getMessage());
        return rec;
    }
}

private PlacementRecommendation createDefaultRecommendation() {
    PlacementRecommendation rec = new PlacementRecommendation();
    rec.setAction(PlacementRecommendation.Action.CREATE_NEW);
    rec.setSuggestedParentId(0);  // Root level
    rec.setConfidence(0.5);
    rec.setReasoning("Default recommendation: create at root level");
    return rec;
}
```

---

**Version:** 1.0
**Last Updated:** November 2024
