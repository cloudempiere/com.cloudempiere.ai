# Architecture Overview

> **Last Updated:** 2025-12-22
> **Version:** v0.31.1

This document provides a visual and comprehensive overview of the com.cloudempiere.ai plugin architecture.

---

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Component Diagram](#component-diagram)
3. [Layer Architecture](#layer-architecture)
4. [Data Flow](#data-flow)
5. [Security Model](#security-model)
6. [Package Structure](#package-structure)
7. [Database Schema](#database-schema)
8. [Integration Points](#integration-points)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           iDempiere WebUI (ZK Framework)                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ AI Chat      │  │ Chart        │  │ Window       │  │ Report       │    │
│  │ Widget       │  │ Assistant    │  │ Assistant    │  │ Assistant    │    │
│  │ (ZK)         │  │ (Planned)    │  │ (Planned)    │  │ (Planned)    │    │
│  └──────┬───────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│         │                                                                    │
├─────────┼────────────────────────────────────────────────────────────────────┤
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        AI Service Layer                              │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │    │
│  │  │ AIService   │  │ Chat Access │  │ Language    │  │ Streaming  │ │    │
│  │  │ (Main)      │  │ Service     │  │ Service     │  │ Handler    │ │    │
│  │  └──────┬──────┘  └─────────────┘  └─────────────┘  └────────────┘ │    │
│  └─────────┼───────────────────────────────────────────────────────────┘    │
│            │                                                                 │
├────────────┼─────────────────────────────────────────────────────────────────┤
│            ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Guardrails & Security                           │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │    │
│  │  │ InputGuard  │  │ OutputGuard │  │ CostGuard   │  │ Execution  │ │    │
│  │  │ (PII/Inject)│  │ (Sensitive) │  │ (Budget)    │  │ Guard      │ │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     LangChain4j Agent Layer                          │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │    │
│  │  │ ERPAgent    │  │ ERP Tools   │  │ Chat Memory │  │ RAG        │ │    │
│  │  │ (AiServices)│  │ (@Tool)     │  │ (Window)    │  │ Service    │ │    │
│  │  └──────┬──────┘  └──────┬──────┘  └─────────────┘  └────────────┘ │    │
│  └─────────┼────────────────┼──────────────────────────────────────────┘    │
│            │                │                                                │
├────────────┼────────────────┼────────────────────────────────────────────────┤
│            ▼                ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     Provider Layer (LangChain4j)                     │    │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────────────┐│    │
│  │  │ Anthropic │  │ Bedrock   │  │ Ollama    │  │ AI Hub            ││    │
│  │  │ (Claude)  │  │ (AWS)     │  │ (Local)   │  │ (Java 17 bridge)  ││    │
│  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────────┬─────────┘│    │
│  └────────┼──────────────┼──────────────┼──────────────────┼──────────┘    │
│           │              │              │                  │                │
└───────────┼──────────────┼──────────────┼──────────────────┼────────────────┘
            ▼              ▼              ▼                  ▼
     ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────────────┐
     │ Anthropic │  │ AWS       │  │ Local     │  │ iDempiere AI Hub  │
     │ API       │  │ Bedrock   │  │ Ollama    │  │ Service (Java 17) │
     └───────────┘  └───────────┘  └───────────┘  └───────────────────┘
```

---

## Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              com.cloudempiere.ai                            │
│                              (OSGi Plugin Bundle)                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         UI LAYER (component/)                        │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ AIChatWidget       │  │ AIChatStreaming    │  │ AIChatMessage │  │   │
│  │  │ - Chat panel UI    │  │ Message            │  │ - User/AI msg │  │   │
│  │  │ - Input handling   │  │ - Progressive      │  │ - Tool calls  │  │   │
│  │  │ - Thread mgmt      │  │   rendering        │  │ - Zoom links  │  │   │
│  │  │ - Cancel support   │  │ - Table builder    │  │               │  │   │
│  │  └────────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       SERVICE LAYER (service/)                       │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ ChatAccessService  │  │ LanguageService    │  │ EmbeddingStore│  │   │
│  │  │ - Owner/Role check │  │ - Detection        │  │ Provider      │  │   │
│  │  │ - Share validation │  │ - Session mgmt     │  │ - pgvector    │  │   │
│  │  │ - Multi-tenant     │  │ - Explicit request │  │ - Ollama      │  │   │
│  │  └────────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    LANGCHAIN4J LAYER (langchain4j/)                  │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ AIService          │  │ ERPTools           │  │ ThreadAware   │  │   │
│  │  │ - Main entry point │  │ - queryDatabase    │  │ ChatMemory    │  │   │
│  │  │ - Streaming mgmt   │  │ - lookupRecord     │  │ - Per-user    │  │   │
│  │  │ - Context building │  │ - searchRecords    │  │ - Per-thread  │  │   │
│  │  │ - Error handling   │  │ - getTableMetadata │  │ - Persistence │  │   │
│  │  └────────────────────┘  │ - listTables       │  └───────────────┘  │   │
│  │                          │ - getBusinessPartner│                     │   │
│  │  ┌────────────────────┐  │ - getProduct       │  ┌───────────────┐  │   │
│  │  │ LangChain4jProvider│  │ - getOrder         │  │ RagService    │  │   │
│  │  │ Factory            │  └────────────────────┘  │ - Content     │  │   │
│  │  │ - Creates models   │                          │   retriever   │  │   │
│  │  │ - Caches instances │                          │ - Embedding   │  │   │
│  │  └────────────────────┘                          └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      GUARDRAILS LAYER (guardrails/)                  │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ InputGuard         │  │ OutputGuard        │  │ CostGuard     │  │   │
│  │  │ - PII detection    │  │ - Credential leak  │  │ - Budget      │  │   │
│  │  │ - Injection block  │  │ - Schema exposure  │  │ - Rate limit  │  │   │
│  │  │ - Base64 decode    │  │ - Redaction        │  │ - Per-user    │  │   │
│  │  └────────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  │  ┌────────────────────┐  ┌────────────────────┐                     │   │
│  │  │ ExecutionGuard     │  │ AIMetricsListener  │                     │   │
│  │  │ - Risk level       │  │ - Token tracking   │                     │   │
│  │  │ - Approval flow    │  │ - Cost calculation │                     │   │
│  │  │ - Action limits    │  │ - Latency metrics  │                     │   │
│  │  └────────────────────┘  └────────────────────┘                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      SECURITY LAYER (database/)                      │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ SecureDatabase     │  │ BoundaryEnforcement│  │ DataAccess    │  │   │
│  │  │ QueryExecutor      │  │ Filter             │  │ Validator     │  │   │
│  │  │ - SQL rewriting    │  │ - AD_Client_ID     │  │ - Table perms │  │   │
│  │  │ - Role filtering   │  │ - AD_Org_ID        │  │ - Column perms│  │   │
│  │  │ - Injection block  │  │ - Access tier      │  │ - Role check  │  │   │
│  │  └────────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        MODEL LAYER (model/)                          │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │ MAIProvider        │  │ MAIChat            │  │ MAIChatEntry  │  │   │
│  │  │ - Provider config  │  │ - Chat session     │  │ - Messages    │  │   │
│  │  │ - Credentials      │  │ - Owner/sharing    │  │ - Role/content│  │   │
│  │  │ - Model selection  │  │ - Thread root      │  │ - Thread ref  │  │   │
│  │  └────────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  │  ┌────────────────────┐  ┌────────────────────┐                     │   │
│  │  │ MAIUsageMetrics    │  │ MAIBudget          │                     │   │
│  │  │ - Token usage      │  │ - Daily limit      │                     │   │
│  │  │ - Cost tracking    │  │ - Monthly limit    │                     │   │
│  │  │ - Latency          │  │ - Per-agent limit  │                     │   │
│  │  └────────────────────┘  └────────────────────┘                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Layer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  ZK Components, Event Handlers, UI State Management             │
│  Files: component/AIChatWidget.java, AIChatStreamingMessage.java│
├─────────────────────────────────────────────────────────────────┤
│                     APPLICATION LAYER                           │
│  Business Logic, Service Orchestration, Workflow                │
│  Files: service/*.java, langchain4j/AIService.java              │
├─────────────────────────────────────────────────────────────────┤
│                       AGENT LAYER                               │
│  LangChain4j AiServices, Tools, Memory, RAG                     │
│  Files: langchain4j/ERPTools.java, ThreadAwareChatMemory.java   │
├─────────────────────────────────────────────────────────────────┤
│                     GUARDRAILS LAYER                            │
│  Input/Output Validation, Cost Control, Risk Assessment         │
│  Files: guardrails/*.java                                       │
├─────────────────────────────────────────────────────────────────┤
│                      SECURITY LAYER                             │
│  Multi-Tenant, RBAC, Query Filtering, Audit                     │
│  Files: database/SecureDatabaseQueryExecutor.java, boundaries/* │
├─────────────────────────────────────────────────────────────────┤
│                      PROVIDER LAYER                             │
│  LangChain4j ChatLanguageModel implementations                  │
│  Files: langchain4j/LangChain4jProviderFactory.java             │
├─────────────────────────────────────────────────────────────────┤
│                        DATA LAYER                               │
│  iDempiere Models, Database Access, Persistence                 │
│  Files: model/M*.java, X_*.java                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Chat Message Flow

```
User Input                              AI Response
    │                                       ▲
    ▼                                       │
┌───────────────┐                   ┌───────────────┐
│ AIChatWidget  │                   │ AIChatWidget  │
│ sendMessage() │                   │ appendChunk() │
└───────┬───────┘                   └───────▲───────┘
        │                                   │
        ▼                                   │
┌───────────────┐                   ┌───────────────┐
│ InputGuard    │                   │ OutputGuard   │
│ validate()    │                   │ validate()    │
└───────┬───────┘                   └───────▲───────┘
        │                                   │
        ▼                                   │
┌───────────────┐                   ┌───────────────┐
│ CostGuard     │                   │ Streaming     │
│ checkBudget() │                   │ Handler       │
└───────┬───────┘                   └───────▲───────┘
        │                                   │
        ▼                                   │
┌───────────────┐                   ┌───────────────┐
│ AIService     │──────────────────▶│ LangChain4j   │
│ chatStreaming │                   │ Agent         │
└───────┬───────┘                   └───────▲───────┘
        │                                   │
        ▼                                   │
┌───────────────┐     Tool Calls    ┌───────────────┐
│ ERPTools      │◀──────────────────│ LLM Provider  │
│ @Tool methods │──────────────────▶│ (Streaming)   │
└───────┬───────┘                   └───────────────┘
        │
        ▼
┌───────────────┐
│ SecureDB      │
│ QueryExecutor │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ iDempiere DB  │
│ (PostgreSQL)  │
└───────────────┘
```

### Tool Execution Flow

```
LLM decides to call tool
         │
         ▼
┌─────────────────────┐
│ LangChain4j         │
│ Tool Router         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ ERPTools.java       │
│ @Tool("queryDatabase")
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐     ┌─────────────────────┐
│ SecureDatabase      │────▶│ AccessSqlParser     │
│ QueryExecutor       │     │ (iDempiere RBAC)    │
└──────────┬──────────┘     └─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ SQL Rewriting       │
│ + AD_Client_ID=?    │
│ + AD_Org_ID IN (?)  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Execute Query       │
│ (PreparedStatement) │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Audit Log           │
│ AIG_QueryAudit      │
└─────────────────────┘
```

---

## Security Model

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SECURITY LAYERS                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Layer 1: INPUT VALIDATION                                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ InputGuard                                                       │    │
│  │ ├── PII Detection (SSN, Credit Cards, Emails)                   │    │
│  │ ├── Prompt Injection Patterns                                    │    │
│  │ ├── Base64 Encoded Injection                                     │    │
│  │ └── Zero-Width Character Detection                               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 2: COST & RATE CONTROL                                           │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ CostGuard                                                        │    │
│  │ ├── Per-User Rate Limits (10/min, 100/hr, 500/day)              │    │
│  │ ├── Per-Tenant Rate Limits (1000/hr, 5000/day)                  │    │
│  │ ├── Budget Enforcement (daily, monthly)                          │    │
│  │ └── Token Tracking (input/output)                                │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 3: MULTI-TENANT ISOLATION                                        │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ BoundaryEnforcementFilter                                        │    │
│  │ ├── AD_Client_ID (Tenant) Filtering                             │    │
│  │ ├── AD_Org_ID (Organization) Filtering                          │    │
│  │ ├── Access Tier (TENANT, TENANT_DICTIONARY, SERVICE_PROVIDER)   │    │
│  │ └── Automatic WHERE clause injection                             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 4: ROLE-BASED ACCESS CONTROL                                     │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ DataAccessValidator                                              │    │
│  │ ├── AD_Table_Access (table-level permissions)                   │    │
│  │ ├── AD_Column_Access (column-level permissions)                 │    │
│  │ ├── AD_Record_Access (record-level permissions)                 │    │
│  │ └── iDempiere Role Integration                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 5: QUERY EXECUTION SECURITY                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ SecureDatabaseQueryExecutor                                      │    │
│  │ ├── SQL Injection Prevention                                     │    │
│  │ ├── Dangerous Keyword Blocking (DROP, ALTER, etc.)              │    │
│  │ ├── System Table Protection (AD_User, AD_Password)              │    │
│  │ └── Dual-Identity Model (AI User + Caller's Role)               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 6: OUTPUT VALIDATION                                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ OutputGuard                                                      │    │
│  │ ├── Credential Leak Prevention (API keys, passwords)            │    │
│  │ ├── Schema Exposure Prevention                                   │    │
│  │ ├── PII Redaction (emails, phones)                              │    │
│  │ └── Connection String Detection                                  │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  Layer 7: AUDIT & MONITORING                                            │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ AIMetricsListener + AIG_UsageMetrics                            │    │
│  │ ├── Query Audit Trail (who, what, when)                         │    │
│  │ ├── Token Usage Tracking                                         │    │
│  │ ├── Cost Attribution                                             │    │
│  │ └── Security Violation Logging                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
src/com/cloudempiere/ai/
│
├── component/                    # ZK UI Components
│   ├── AIChatWidget.java         # Main chat panel (2500+ lines)
│   ├── AIChatStreamingMessage.java # Streaming message renderer
│   ├── AIChatMessage.java        # Static message display
│   └── markdown/                 # Markdown rendering
│       ├── MarkdownRenderer.java
│       ├── TableRenderer.java
│       └── ZoomLinkProcessor.java
│
├── service/                      # Application Services
│   ├── ChatAccessService.java    # Ownership/sharing validation
│   ├── LanguageService.java      # Language detection
│   └── EmbeddingStoreProvider.java # Vector store management
│
├── provider/                     # (Legacy) Custom Providers
│   ├── IAIProvider.java          # Legacy interface (deprecated)
│   ├── dto/                      # Legacy DTOs (to be deprecated)
│   │   ├── AIRequest.java
│   │   ├── AIResponse.java
│   │   └── ...
│   └── langchain4j/              # LangChain4j Integration
│       ├── AIService.java        # Main entry point (1200+ lines)
│       ├── ERPTools.java         # @Tool annotated methods
│       ├── LangChain4jProviderFactory.java
│       ├── ThreadAwareChatMemory.java
│       ├── RagService.java
│       └── wrappers/             # Provider wrappers
│           ├── BedrockChatModelWrapper.java
│           └── MockAIHubChatModel.java
│
├── guardrails/                   # Security Guards
│   ├── InputGuard.java           # Input validation
│   ├── OutputGuard.java          # Output validation
│   ├── CostGuard.java            # Budget/rate limiting
│   ├── ExecutionGuard.java       # Risk assessment
│   └── GuardResult.java          # Guard response DTO
│
├── database/                     # Database Security
│   ├── SecureDatabaseQueryExecutor.java
│   └── dto/
│       ├── SecureQueryRequest.java
│       └── SecureQueryResult.java
│
├── boundaries/                   # Domain Boundaries
│   ├── BoundaryEnforcementFilter.java
│   ├── DataAccessValidator.java
│   ├── CostBoundaryMonitor.java
│   └── AgentBoundaryRegistry.java
│
├── context/                      # Context Providers
│   ├── IAIContextProvider.java
│   ├── AIContextProviderRegistry.java
│   └── impl/
│       ├── WindowContextProvider.java
│       └── ChartContextProvider.java
│
├── model/                        # iDempiere Models
│   ├── MAIProvider.java          # Provider configuration
│   ├── MAIChat.java              # Chat session
│   ├── MAIChatEntry.java         # Chat messages
│   ├── MAIUsageMetrics.java      # Usage tracking
│   ├── MAIBudget.java            # Budget management
│   └── X_*.java                  # Generated base classes
│
├── process/                      # iDempiere Processes
│   └── TestAIProvider.java       # Provider testing
│
├── routing/                      # (Legacy) Custom Routing
│   ├── ConversationContextManager.java  # (to be deprecated)
│   ├── PromptAnalyzer.java              # (to be deprecated)
│   └── EntityExtractor.java             # (to be deprecated)
│
└── Activator.java                # OSGi Bundle Activator
```

---

## Database Schema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI PLUGIN TABLES                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────┐       ┌─────────────────────────┐          │
│  │     AIG_Provider        │       │      CM_Chat            │          │
│  ├─────────────────────────┤       ├─────────────────────────┤          │
│  │ AIG_Provider_ID (PK)    │       │ CM_Chat_ID (PK)         │          │
│  │ AD_Client_ID            │       │ AD_Client_ID            │          │
│  │ AD_Org_ID               │       │ AD_Org_ID               │          │
│  │ Name                    │       │ AD_User_ID (Owner)      │          │
│  │ AIGProviderType         │       │ SharedWithRole_ID       │          │
│  │ APIKey (encrypted)      │       │ ShareType               │          │
│  │ ModelName               │       │ Description             │          │
│  │ BaseURL                 │       │ IsActive                │          │
│  │ AD_User_ID (AI User)    │       └───────────┬─────────────┘          │
│  │ IsActive                │                   │                         │
│  └─────────────────────────┘                   │ 1:N                     │
│              │                                  │                         │
│              │ FK                               ▼                         │
│              │                   ┌─────────────────────────┐             │
│              │                   │     CM_ChatEntry        │             │
│              │                   ├─────────────────────────┤             │
│              │                   │ CM_ChatEntry_ID (PK)    │             │
│              │                   │ CM_Chat_ID (FK)         │             │
│              │                   │ AD_Client_ID            │             │
│              │                   │ ChatEntryType (U/A/T/P) │             │
│              │                   │ CharacterData (content) │             │
│              │                   │ ThreadRootId            │             │
│              │                   │ Created                 │             │
│              │                   └─────────────────────────┘             │
│              │                                                           │
│              ▼                                                           │
│  ┌─────────────────────────┐       ┌─────────────────────────┐          │
│  │   AIG_UsageMetrics      │       │      AIG_Budget         │          │
│  ├─────────────────────────┤       ├─────────────────────────┤          │
│  │ AIG_UsageMetrics_ID(PK) │       │ AIG_Budget_ID (PK)      │          │
│  │ AD_Client_ID            │       │ AD_Client_ID            │          │
│  │ AD_User_ID              │       │ AD_User_ID              │          │
│  │ AIG_Provider_ID (FK)    │       │ BudgetType (D/M/A)      │          │
│  │ InputTokens             │       │ MaxTokens               │          │
│  │ OutputTokens            │       │ MaxCostUSD              │          │
│  │ CostUSD                 │       │ CurrentUsage            │          │
│  │ LatencyMS               │       │ ResetDate               │          │
│  │ Created                 │       │ IsActive                │          │
│  └─────────────────────────┘       └─────────────────────────┘          │
│                                                                          │
│  ┌─────────────────────────┐       ┌─────────────────────────┐          │
│  │   AIG_QueryAudit        │       │  AIG_SecurityViolation  │          │
│  ├─────────────────────────┤       ├─────────────────────────┤          │
│  │ AIG_QueryAudit_ID (PK)  │       │ (Planned - ADR-048)     │          │
│  │ AD_Client_ID            │       │ ViolationType           │          │
│  │ AD_User_ID (Initiator)  │       │ Severity                │          │
│  │ AI_User_ID              │       │ BlockedQuery            │          │
│  │ SQLQuery                │       │ Details                 │          │
│  │ TablesAccessed          │       │ IPAddress               │          │
│  │ RowsReturned            │       │ Created                 │          │
│  │ ExecutionTimeMS         │       └─────────────────────────┘          │
│  │ Created                 │                                             │
│  └─────────────────────────┘                                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Integration Points

### iDempiere Integration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    iDempiere Integration Points                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  OSGi Services                                                           │
│  ├── IAIProviderFactory (Service Ranking: 100)                          │
│  ├── IChatAccessService                                                  │
│  └── ILanguageService                                                    │
│                                                                          │
│  iDempiere APIs Used                                                     │
│  ├── Env.getCtx() - Context propagation                                 │
│  ├── Env.getAD_Client_ID() - Tenant identification                      │
│  ├── Env.getAD_User_ID() - User identification                          │
│  ├── MRole - Role-based access control                                  │
│  ├── AccessSqlParser - SQL permission filtering                         │
│  ├── DB.prepareStatement() - Database access                            │
│  └── CCache - Caching infrastructure                                    │
│                                                                          │
│  ZK Framework                                                            │
│  ├── Executions.schedule() - UI thread scheduling                       │
│  ├── Desktop - Session management                                        │
│  ├── Component - UI component hierarchy                                  │
│  └── Event - User interaction handling                                   │
│                                                                          │
│  Extension Points                                                        │
│  ├── AD_Form - Custom forms (AI Chat Panel)                             │
│  ├── AD_Process - iDempiere processes (TestAIProvider)                  │
│  └── AD_Table/AD_Column - Data model extensions                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### External Integrations

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      External Integrations                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  AI Providers (via LangChain4j 0.35.0)                                  │
│  ├── Anthropic Claude (AnthropicChatModel)                              │
│  │   └── Models: claude-3-opus, claude-3-sonnet, claude-3-haiku,        │
│  │              claude-3.5-sonnet, claude-4-sonnet                       │
│  ├── AWS Bedrock (BedrockChatModel)                                     │
│  │   └── Models: Claude, Nova, Mistral, Llama                           │
│  ├── OpenAI (OpenAiChatModel)                                           │
│  │   └── Models: gpt-4o, gpt-4-turbo                                    │
│  ├── Ollama (OllamaChatModel) - Local                                   │
│  │   └── Models: llama3.2, mistral, etc.                                │
│  └── AI Hub Provider (MockAIHubChatModel)                               │
│      └── Bridge to Java 17 service for advanced features                │
│                                                                          │
│  Embedding Providers                                                     │
│  ├── Ollama (OllamaEmbeddingModel)                                      │
│  │   └── Models: nomic-embed-text, all-minilm                           │
│  └── pgvector (PostgreSQL extension)                                    │
│      └── Vector similarity search                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Related Documentation

| Document | Description |
|----------|-------------|
| [ADR-001](adr/001-initial-architecture.md) | Initial Architecture Standards |
| [ADR-002](adr/002-langchain4j-strategic-adoption.md) | LangChain4j Adoption |
| [ADR-006](adr/006-data-model-architecture.md) | Data Model Architecture |
| [ADR-007](adr/007-database-security-model.md) | Database Security Model |
| [ADR-010](adr/010-agent-orchestration-architecture.md) | Agent Orchestration |
| [ADR-014](adr/014-guardrails-and-safety.md) | Guardrails and Safety |
| [ADR-048](adr/048-comprehensive-security-strategy.md) | Security Strategy |
| [DEPRECATION_ROADMAP.md](DEPRECATION_ROADMAP.md) | Code Migration Plan |
| [CRITICAL_ISSUES.md](CRITICAL_ISSUES.md) | Known Issues |
