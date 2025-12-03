# ADR-026: Vector Database Strategy for AI Infrastructure

**Status:** Proposed
**Date:** 2025-12-03
**Deciders:** CloudEmpiere AI Team
**Relates to:** ADR-012 (RAG-Based Context Retrieval), ADR-006 (Data Model Architecture)

---

## Context

### Problem Statement

The CloudEmpiere AI plugin requires vector storage for:
1. **RAG embeddings** - Document and conversation context retrieval (ADR-012)
2. **Semantic search** - Finding similar records, products, tickets
3. **AI Knowledge Base** - iDempiere documentation and wiki search (ADR-016, ADR-025)

ADR-012 currently specifies `InMemoryEmbeddingStore` for development, but production requires a **persistent, scalable vector database** that integrates with our AWS infrastructure.

### Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Current: InMemoryEmbeddingStore (ADR-012)                  │
│  ✅ Works for development                                    │
│  ❌ Not persistent (data lost on restart)                   │
│  ❌ Not scalable (single JVM memory limit)                  │
│  ❌ Not suitable for production                             │
└─────────────────────────────────────────────────────────────┘
```

### Production Requirements

| Requirement | Description | Priority |
|-------------|-------------|----------|
| **Persistence** | Vectors survive server restarts | Critical |
| **Scalability** | Support millions of embeddings | High |
| **AWS Integration** | Native AWS service preferred | High |
| **Cost Efficiency** | Reasonable for ERP workloads | High |
| **iDempiere Compatibility** | Works with PostgreSQL ecosystem | Medium |
| **Bedrock Integration** | Optional: AWS Bedrock Knowledge Bases | Low |

---

## Decision Drivers

1. **Existing Infrastructure** - iDempiere uses PostgreSQL (RDS or Aurora)
2. **AWS Ecosystem** - Production runs on AWS
3. **Cost Sensitivity** - ERP deployments are cost-conscious
4. **Operational Simplicity** - Minimize additional services
5. **Future Flexibility** - Support Bedrock Knowledge Bases if needed

---

## Considered Options

### Option 1: AWS RDS PostgreSQL with pgvector Extension

**Description:** Add pgvector extension to existing iDempiere PostgreSQL database.

```
┌─────────────────────────────────────────────────────────────┐
│  AWS RDS PostgreSQL (existing iDempiere database)           │
│  ├── iDempiere tables (AD_*, C_*, M_*, etc.)               │
│  └── Vector tables (AIG_Embedding)                          │
│      └── pgvector extension (CREATE EXTENSION vector;)      │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **Zero additional infrastructure** - Uses existing RDS instance
- ✅ **Single database** - Unified data model with iDempiere
- ✅ **Cost efficient** - No separate vector database costs
- ✅ **SQL integration** - Combine vector search with SQL queries
- ✅ **pgvector 0.8.0** - Latest version on RDS (Nov 2024)
- ✅ **HNSW indexing** - Fast approximate nearest neighbor search
- ✅ **Familiar tooling** - Standard PostgreSQL administration

**Cons:**
- ❌ **Shared resources** - Vector workloads compete with ERP queries
- ❌ **Moderate scale** - Best for <10M vectors
- ❌ **No Bedrock integration** - Cannot use as Bedrock Knowledge Base vector store
- ❌ **Manual setup** - Extension must be enabled per database

**Cost:** $0/month additional (uses existing RDS)

---

### Option 2: AWS Aurora PostgreSQL with pgvector Extension

**Description:** Migrate to Aurora PostgreSQL for enhanced vector capabilities.

```
┌─────────────────────────────────────────────────────────────┐
│  AWS Aurora PostgreSQL                                      │
│  ├── iDempiere tables                                       │
│  └── Vector tables with pgvector                            │
│      ├── Serverless v2 auto-scaling                         │
│      └── Up to 67x faster HNSW index builds (pgvector 0.7+) │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **Aurora performance** - 3x throughput vs standard PostgreSQL
- ✅ **Serverless scaling** - Automatic scale-up/down
- ✅ **67x faster index builds** - Binary quantization optimization
- ✅ **Bedrock compatible** - Can use with Bedrock Knowledge Bases
- ✅ **Read replicas** - Offload vector queries to replica
- ✅ **Single database** - Unified with iDempiere data

**Cons:**
- ❌ **Migration required** - Must migrate from RDS to Aurora
- ❌ **Higher base cost** - Aurora more expensive than RDS
- ❌ **Complexity** - Aurora-specific administration

**Cost:** ~$150-400/month (depending on ACU usage)

---

### Option 3: Amazon OpenSearch Serverless

**Description:** Dedicated vector search service separate from iDempiere database.

```
┌─────────────────────────────────────────────────────────────┐
│  AWS OpenSearch Serverless                                  │
│  ├── Vector collection (embeddings)                         │
│  ├── k-NN search with HNSW                                  │
│  └── Integrated with Bedrock Knowledge Bases                │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **Purpose-built** - Optimized for vector search
- ✅ **Serverless** - No capacity planning needed
- ✅ **High scale** - Billions of vectors supported
- ✅ **Bedrock native** - First-class Bedrock Knowledge Base integration
- ✅ **Hybrid search** - Vector + keyword (BM25) search
- ✅ **No iDempiere impact** - Separate from ERP database

**Cons:**
- ❌ **High minimum cost** - ~$976/month minimum (4 OCUs × $0.24/hour)
- ❌ **Separate service** - Additional infrastructure to manage
- ❌ **Data sync required** - Must replicate data from iDempiere
- ❌ **Different query language** - Not SQL

**Cost:** ~$976-2000/month minimum

---

### Option 4: Amazon S3 Vectors (NEW - GA 2025)

**Description:** Store vectors directly in S3 with native vector query support.

```
┌─────────────────────────────────────────────────────────────┐
│  Amazon S3 Vectors                                          │
│  ├── Vector buckets (up to 2B vectors per index)            │
│  ├── Pay-per-query pricing                                  │
│  └── Integrated with Bedrock Knowledge Bases (preview)      │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **90% cost savings** - AWS claims vs specialized vector DBs
- ✅ **Massive scale** - 2 billion vectors per index, 20 trillion per bucket
- ✅ **Pay-per-use** - No minimum monthly cost
- ✅ **Bedrock integration** - Works with Knowledge Bases
- ✅ **Simple storage** - No database administration

**Cons:**
- ❌ **New service** - Just reached GA, limited production experience
- ❌ **Query latency** - Higher than in-memory solutions (~sub-second)
- ❌ **Limited features** - No hybrid search, basic filtering only
- ❌ **Separate from iDempiere** - Cannot join with ERP data

**Cost:** ~$50-200/month for typical ERP usage (pay-per-query)

---

### Option 5: Hybrid - RDS pgvector + S3 Vectors

**Description:** Use RDS pgvector for operational vectors, S3 Vectors for Knowledge Base.

```
┌─────────────────────────────────────────────────────────────┐
│  Hybrid Architecture                                        │
│  ┌─────────────────────┐  ┌─────────────────────────────┐   │
│  │ RDS PostgreSQL      │  │ S3 Vectors                  │   │
│  │ + pgvector          │  │                             │   │
│  │                     │  │                             │   │
│  │ - Conversation ctx  │  │ - iDempiere docs (wiki)     │   │
│  │ - Window context    │  │ - Code templates            │   │
│  │ - Recent queries    │  │ - Large knowledge base      │   │
│  │                     │  │                             │   │
│  │ Fast, SQL joins     │  │ Cost-effective, massive     │   │
│  └─────────────────────┘  └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **Best of both worlds** - Operational speed + KB scale
- ✅ **Cost optimized** - Expensive vectors in S3, fast vectors in RDS
- ✅ **SQL joins available** - Operational vectors can join ERP data
- ✅ **Future-proof** - Can add Bedrock Knowledge Bases later

**Cons:**
- ❌ **Two systems** - More complexity
- ❌ **Routing logic** - Must decide which store to use

**Cost:** ~$50-150/month (S3 Vectors for KB only)

---

## Comparison Matrix

| Criteria | RDS pgvector | Aurora pgvector | OpenSearch Serverless | S3 Vectors | Hybrid |
|----------|--------------|-----------------|----------------------|------------|--------|
| **Monthly Cost** | $0 additional | $150-400 | $976+ | $50-200 | $50-150 |
| **Setup Complexity** | Low | Medium | Medium | Low | Medium |
| **Scale (vectors)** | ~10M | ~50M | Billions | Billions | Mixed |
| **Query Latency** | <100ms | <50ms | <100ms | <1000ms | Mixed |
| **SQL Integration** | ✅ Full | ✅ Full | ❌ None | ❌ None | ✅ Partial |
| **Bedrock KB** | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Operational Impact** | Medium | Low | None | None | Low |
| **Hybrid Search** | ❌ Manual | ❌ Manual | ✅ Native | ❌ No | ❌ Manual |

---

## Decision

### Recommended: Option 1 (RDS PostgreSQL with pgvector) for Phase 1

**Primary vector store:** Add pgvector to existing iDempiere RDS PostgreSQL instance.

### Rationale

1. **Zero additional cost** - Uses existing database infrastructure
2. **Zero new services** - No additional AWS services to manage
3. **SQL integration** - Can join vector search with iDempiere data
4. **Sufficient scale** - 10M vectors covers typical ERP usage
5. **Proven technology** - pgvector 0.8.0 is mature and well-documented
6. **Path to Aurora** - Can migrate to Aurora later if needed

### Future Path (Phase 2+)

**If requirements grow:**
- **Scale > 10M vectors** → Migrate to Aurora PostgreSQL
- **Bedrock Knowledge Bases** → Add S3 Vectors for documentation
- **High-volume semantic search** → Consider OpenSearch Serverless

---

## Implementation

### Phase 1: Enable pgvector (Days 1-2)

**1. Enable Extension:**
```sql
-- On RDS PostgreSQL 13.17+, 14.14+, 15.9+, 16.5+, or 17.1+
CREATE EXTENSION IF NOT EXISTS vector;
```

**2. Create Embedding Table:**
```sql
-- AIG_Embedding table for vector storage
CREATE TABLE AIG_Embedding (
    AIG_Embedding_ID    SERIAL PRIMARY KEY,
    AD_Client_ID        INTEGER NOT NULL,
    AD_Org_ID           INTEGER NOT NULL,
    AD_Table_ID         INTEGER,              -- Source table (optional)
    Record_ID           INTEGER,              -- Source record (optional)
    EmbeddingType       VARCHAR(50) NOT NULL, -- 'CONVERSATION', 'DOCUMENT', 'RECORD'
    ContentHash         VARCHAR(64),          -- SHA-256 of source content
    Embedding           vector(1536),         -- OpenAI dimension, adjust as needed
    Metadata            JSONB,                -- Additional context
    Created             TIMESTAMP DEFAULT NOW(),
    Updated             TIMESTAMP DEFAULT NOW(),
    IsActive            CHAR(1) DEFAULT 'Y'
);

-- HNSW index for fast similarity search
CREATE INDEX idx_aig_embedding_hnsw
ON AIG_Embedding
USING hnsw (Embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Partial index for active embeddings only
CREATE INDEX idx_aig_embedding_active
ON AIG_Embedding (AD_Client_ID, EmbeddingType)
WHERE IsActive = 'Y';
```

### Phase 2: LangChain4j Integration (Days 3-5)

**Update RAGContextManager to use pgvector:**

```java
package com.cloudempiere.ai.rag;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import javax.sql.DataSource;

/**
 * Production RAG context manager using pgvector
 * @see ADR-026 Vector Database Strategy
 */
public class PgVectorContextManager {

    private final PgVectorEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    public PgVectorContextManager(DataSource dataSource, EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = PgVectorEmbeddingStore.builder()
            .dataSource(dataSource)
            .table("aig_embedding")
            .dimension(1536)
            .createTable(false)  // Table created via migration
            .build();
    }

    public ContentRetriever getRetriever(int adClientId, String embeddingType) {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(10)
            .minScore(0.7)
            .filter(new PgVectorFilter()
                .eq("ad_client_id", adClientId)
                .eq("embedding_type", embeddingType))
            .build();
    }
}
```

**Add Maven dependency:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```

### Phase 3: Migration Scripts (Day 6)

**PostgreSQL Migration:**
```sql
-- Migration: 202512_001_pgvector_setup.sql

-- 1. Enable extension (requires rds_superuser)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create embedding table
CREATE TABLE IF NOT EXISTS AIG_Embedding (
    -- columns as above
);

-- 3. Create indexes
CREATE INDEX IF NOT EXISTS idx_aig_embedding_hnsw ...;

-- 4. Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON AIG_Embedding TO idempiere;
GRANT USAGE, SELECT ON SEQUENCE aig_embedding_aig_embedding_id_seq TO idempiere;
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        VECTOR DATABASE ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                    com.cloudempiere.ai (OSGi Plugin)                    │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │   │
│  │  │ RAGContextMgr   │  │ AIConversation  │  │ Knowledge Base Agent    │  │   │
│  │  │ (ADR-012)       │  │ Service         │  │ (ADR-016, ADR-025)      │  │   │
│  │  └────────┬────────┘  └────────┬────────┘  └───────────┬─────────────┘  │   │
│  │           │                    │                       │                │   │
│  │           └────────────────────┼───────────────────────┘                │   │
│  │                                │                                        │   │
│  │                                ▼                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │              PgVectorEmbeddingStore (LangChain4j)               │   │   │
│  │  │  - Conversation context embeddings                              │   │   │
│  │  │  - Window/chart context embeddings                              │   │   │
│  │  │  - Document embeddings (future)                                 │   │   │
│  │  └────────────────────────────┬────────────────────────────────────┘   │   │
│  └───────────────────────────────┼────────────────────────────────────────┘   │
│                                  │                                            │
│                                  ▼                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                   AWS RDS PostgreSQL (Existing)                         │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │  │
│  │  │  iDempiere Schema                │  AI Schema                   │   │  │
│  │  │  ├── AD_* (Dictionary)           │  ├── AIG_Provider            │   │  │
│  │  │  ├── C_* (Business Partner)      │  ├── AIG_Conversation        │   │  │
│  │  │  ├── M_* (Material Mgmt)         │  ├── AIG_Message             │   │  │
│  │  │  ├── S_* (Services)              │  └── AIG_Embedding ◄─────────┤   │  │
│  │  │  └── ...                         │      (pgvector extension)    │   │  │
│  │  │                                  │      - vector(1536) column   │   │  │
│  │  │                                  │      - HNSW index            │   │  │
│  │  └──────────────────────────────────┴──────────────────────────────┘   │  │
│  │                                                                         │  │
│  │  pgvector 0.8.0 Features:                                              │  │
│  │  ✅ HNSW indexing (fast ANN search)                                     │  │
│  │  ✅ Iterative index scans (prevents overfiltering)                      │  │
│  │  ✅ Cosine, L2, inner product distance                                  │  │
│  │  ✅ Up to 16,000 dimensions                                             │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                                │
│  FUTURE EXPANSION (Phase 2+):                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐   │  │
│  │  │ Aurora PostgreSQL │  │ S3 Vectors        │  │ Bedrock KB        │   │  │
│  │  │ (Scale >10M)      │  │ (Documentation)   │  │ (Managed RAG)     │   │  │
│  │  │ $150-400/mo       │  │ $50-200/mo        │  │ Integrated        │   │  │
│  │  └───────────────────┘  └───────────────────┘  └───────────────────┘   │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Cost Analysis

### Scenario: Typical iDempiere AI Deployment

**Assumptions:**
- 100,000 embeddings (conversations + context)
- 10,000 queries/day
- 1536 dimensions (OpenAI compatible)

| Option | Monthly Cost | Notes |
|--------|--------------|-------|
| **RDS pgvector** | **$0** | Uses existing RDS instance |
| Aurora pgvector | ~$200 | Minimum 0.5 ACU |
| OpenSearch Serverless | ~$976 | 4 OCU minimum |
| S3 Vectors | ~$75 | Pay-per-query |

**Recommendation:** Start with RDS pgvector ($0 additional cost), migrate to Aurora or S3 Vectors if scale requires.

---

## Consequences

### Positive

- ✅ **Zero additional cost** - Uses existing RDS infrastructure
- ✅ **SQL integration** - Join vectors with iDempiere data
- ✅ **Simple operations** - No new services to manage
- ✅ **Multi-tenant ready** - AD_Client_ID filtering built-in
- ✅ **LangChain4j compatible** - Native pgvector support

### Negative

- ❌ **Scale ceiling** - ~10M vectors before migration needed
- ❌ **No Bedrock KB** - Cannot use directly with Bedrock Knowledge Bases
- ❌ **Shared resources** - Vector queries impact ERP performance

### Neutral

- 🔄 **Migration path clear** - Aurora or S3 Vectors for Phase 2
- 🔄 **Extension dependency** - pgvector must be enabled

### Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Vector queries slow ERP** | Medium (30%) | Medium | Separate read replica for AI |
| **Scale exceeds 10M** | Low (10%) | Medium | Migrate to Aurora |
| **pgvector version lag** | Low (5%) | Low | RDS typically updates within months |
| **Extension not available** | Very Low (1%) | High | Use fallback InMemoryEmbeddingStore |

---

## Alternatives Rejected

### Pinecone / Weaviate / Milvus (External SaaS)

**Rejected because:**
- Additional vendor dependency
- Data leaves AWS
- Higher cost for comparable scale
- No SQL integration with iDempiere

### MemoryDB for Redis

**Rejected because:**
- Separate service to manage
- In-memory pricing expensive for large datasets
- No native Bedrock integration

### Neptune Analytics

**Rejected because:**
- Graph database, not optimal for pure vector search
- Higher complexity for simple embedding use cases
- More expensive than pgvector

---

## Success Criteria

**This ADR is considered successful if:**

1. ✅ pgvector extension enabled on RDS without issues
2. ✅ AIG_Embedding table created with HNSW index
3. ✅ LangChain4j PgVectorEmbeddingStore integration working
4. ✅ Query latency < 200ms for similarity search
5. ✅ No measurable impact on iDempiere ERP performance
6. ✅ Multi-tenant isolation via AD_Client_ID verified

---

## References

### AWS Documentation

- [Amazon RDS for PostgreSQL pgvector 0.8.0](https://aws.amazon.com/about-aws/whats-new/2024/11/amazon-rds-for-postgresql-pgvector-080/)
- [AWS Vector Database Comparison](https://docs.aws.amazon.com/prescriptive-guidance/latest/choosing-an-aws-vector-database-for-rag-use-cases/vector-db-comparison.html)
- [AWS Vector Database Options](https://docs.aws.amazon.com/prescriptive-guidance/latest/choosing-an-aws-vector-database-for-rag-use-cases/vector-db-options.html)
- [Amazon S3 Vectors GA](https://aws.amazon.com/blogs/aws/amazon-s3-vectors-now-generally-available-with-increased-scale-and-performance/)
- [Building AI-powered search with pgvector](https://aws.amazon.com/blogs/database/building-ai-powered-search-in-postgresql-using-amazon-sagemaker-and-pgvector/)

### Cost Analysis

- [OpenSearch Serverless Pricing Analysis](https://www.tecracer.com/blog/2024/04/rag-ai-llm-databases-on-aws-do-not-pay-for-oversized-go-serverless-instead.html)
- [S3 Vectors Pricing Deep Dive](https://murraycole.com/posts/aws-s3-vectors-pricing-deep-dive)
- [Bedrock Knowledge Base Pricing](https://repost.aws/questions/QUzV7T0_gXRZSsnAtg1JGx3Q/pricing-for-aws-bedrock-knowledge-bases)

### Technical Comparisons

- [pgvector vs OpenSearch Comprehensive Analysis](https://www.myscale.com/blog/comprehensive-analysis-pgvector-vs-opensearch-performance-vector-databases/)
- [pgvector vs OpenSearch 5 Differences](https://www.instaclustr.com/education/vector-database/pgvector-vs-opensearch-for-vector-databases-5-differences-and-how-to-choose/)
- [67x Faster HNSW Index with Aurora](https://aws.amazon.com/blogs/database/load-vector-embeddings-up-to-67x-faster-with-pgvector-and-amazon-aurora/)

### Internal ADRs

- [ADR-006](006-data-model-architecture.md) - Data Model Architecture
- [ADR-012](012-rag-based-context-retrieval.md) - RAG-Based Context Retrieval

---

**ADR-026 | Version 1.0 | 2025-12-03**
**Status: Proposed**
**Implementation: Phase 1 with RDS pgvector, Phase 2+ expansion options documented**
