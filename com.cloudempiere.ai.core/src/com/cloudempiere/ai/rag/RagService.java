/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;
import com.cloudempiere.ai.rag.dto.SearchResult;
import com.cloudempiere.ai.rag.embedding.IEmbeddingStoreProvider;
import com.cloudempiere.ai.rag.ingest.IKnowledgeIngestor;
import com.cloudempiere.ai.rag.ingest.IngestResult;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Core RAG service for knowledge retrieval.
 *
 * <p>Combines:
 * <ul>
 *   <li>EmbeddingStoreProvider for persistent vector storage</li>
 *   <li>EmbeddingModel for semantic search</li>
 *   <li>Metadata filtering for source types and multi-tenancy</li>
 *   <li>KnowledgeIngestors for populating the store</li>
 * </ul>
 *
 * <p>Reference: idempiere-cli RagService
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
@Component(
    service = IRagService.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
public class RagService implements IRagService {

    private static final CLogger log = CLogger.getCLogger(RagService.class);

    /** Default provider ID (until provider selection UI) */
    private static final int DEFAULT_PROVIDER_ID = 1000001;

    /** Default minimum similarity score */
    private static final double DEFAULT_MIN_SCORE = 0.7;

    /** Default max results */
    private static final int DEFAULT_MAX_RESULTS = 5;

    /** Embedding store provider */
    @Reference
    private IEmbeddingStoreProvider storeProvider;

    /** Registered knowledge ingestors */
    private final List<IKnowledgeIngestor> ingestors = new CopyOnWriteArrayList<>();

    /** Cached embedding model */
    private EmbeddingModel embeddingModel;

    /** Lock for model initialization */
    private final Object modelLock = new Object();

    @Activate
    public void activate() {
        log.info("RagService activated");
    }

    @Deactivate
    public void deactivate() {
        log.info("RagService deactivated");
        embeddingModel = null;
    }

    /**
     * Bind a knowledge ingestor (OSGi DS).
     */
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    public void bindIngestor(IKnowledgeIngestor ingestor) {
        ingestors.add(ingestor);
        log.info("Registered knowledge ingestor: " + ingestor.getSourceType());
    }

    /**
     * Unbind a knowledge ingestor (OSGi DS).
     */
    public void unbindIngestor(IKnowledgeIngestor ingestor) {
        ingestors.remove(ingestor);
        log.info("Unregistered knowledge ingestor: " + ingestor.getSourceType());
    }

    @Override
    public List<SearchResult> search(Properties ctx, String query, String sourceFilter, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        if (!isAvailable()) {
            log.warning("RAG service not available - embedding model or store not configured");
            return results;
        }

        try {
            EmbeddingModel model = getEmbeddingModel(ctx);
            if (model == null) {
                log.warning("No embedding model available");
                return results;
            }

            EmbeddingStore<TextSegment> store = storeProvider.getStore();

            // Generate query embedding
            Embedding queryEmbedding = model.embed(query).content();

            // Build filter
            Filter filter = buildFilter(ctx, sourceFilter);

            // Execute search
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(DEFAULT_MIN_SCORE)
                .filter(filter)
                .build();

            EmbeddingSearchResult<TextSegment> searchResult = store.search(request);

            // Convert to SearchResult
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                results.add(SearchResult.builder()
                    .id(match.embeddingId())
                    .title(extractTitle(segment))
                    .content(segment.text())
                    .sourceType(getMetadataString(segment, "source_type"))
                    .sourceId(getMetadataString(segment, "source_id"))
                    .score(match.score())
                    .metadata(segment.metadata().toMap())
                    .build());
            }

            log.fine("RAG search: query='" + truncate(query, 50) + "', filter=" + sourceFilter +
                    ", results=" + results.size());

        } catch (Exception e) {
            log.log(Level.SEVERE, "RAG search failed: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * Build metadata filter for search.
     */
    private Filter buildFilter(Properties ctx, String sourceFilter) {
        int clientId = Env.getAD_Client_ID(ctx);

        // Client filter for multi-tenancy
        Filter clientFilter = metadataKey("ad_client_id").isEqualTo(String.valueOf(clientId));

        // Source type filter (optional)
        if (sourceFilter != null && !sourceFilter.isEmpty()) {
            Filter sourceTypeFilter = metadataKey("source_type").isEqualTo(sourceFilter);
            return clientFilter.and(sourceTypeFilter);
        }

        return clientFilter;
    }

    /**
     * Extract title from segment metadata.
     */
    private String extractTitle(TextSegment segment) {
        String title = getMetadataString(segment, "title");
        if (title != null && !title.isEmpty()) {
            return title;
        }
        // Use first line of text as title
        String text = segment.text();
        int newlineIdx = text.indexOf('\n');
        if (newlineIdx > 0 && newlineIdx < 100) {
            return text.substring(0, newlineIdx);
        }
        return truncate(text, 60);
    }

    /**
     * Get metadata value as string.
     */
    private String getMetadataString(TextSegment segment, String key) {
        return segment.metadata().getString(key);
    }

    @Override
    public String findADEntity(Properties ctx, String entityName, String entityType) {
        // Search with specific source type
        String sourceFilter = "ad_metadata";
        String query = entityType + " " + entityName;

        List<SearchResult> results = search(ctx, query, sourceFilter, 3);

        if (results.isEmpty()) {
            return "No " + entityType + " found matching '" + entityName + "'";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" matching ").append(entityType).append("(s):\n\n");

        for (SearchResult result : results) {
            sb.append(result.toFormattedString()).append("\n\n");
        }

        return sb.toString();
    }

    @Override
    public String lookupGlossary(Properties ctx, String term) {
        List<SearchResult> results = search(ctx, term, "glossary", 3);

        if (results.isEmpty()) {
            return "No glossary entry found for '" + term + "'. " +
                   "This term may not be defined in the knowledge base.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Glossary: ").append(term).append("**\n\n");

        for (SearchResult result : results) {
            sb.append(result.getContent()).append("\n\n");
        }

        return sb.toString();
    }

    @Override
    public ContentRetriever getContentRetriever(Properties ctx) {
        if (!isAvailable()) {
            log.warning("Creating fallback ContentRetriever (no embeddings)");
            return query -> List.of();
        }

        EmbeddingModel model = getEmbeddingModel(ctx);
        EmbeddingStore<TextSegment> store = storeProvider.getStore();

        int clientId = Env.getAD_Client_ID(ctx);

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(model)
            .maxResults(DEFAULT_MAX_RESULTS)
            .minScore(DEFAULT_MIN_SCORE)
            .filter(metadataKey("ad_client_id").isEqualTo(String.valueOf(clientId)))
            .build();
    }

    @Override
    public void ingestAll(Properties ctx, boolean forceRefresh) {
        log.info("Starting full knowledge ingestion, forceRefresh=" + forceRefresh);
        long startTime = System.currentTimeMillis();
        int totalIngested = 0;

        for (IKnowledgeIngestor ingestor : ingestors) {
            try {
                if (forceRefresh || ingestor.needsRefresh(ctx)) {
                    IngestResult result = ingestor.ingest(ctx, forceRefresh);
                    totalIngested += result.getDocumentsAdded();
                    log.info("Ingested " + ingestor.getSourceType() + ": " + result);
                } else {
                    log.fine("Skipping " + ingestor.getSourceType() + " (up to date)");
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Ingestion failed for " + ingestor.getSourceType(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Knowledge ingestion complete: " + totalIngested + " documents in " + duration + "ms");
    }

    @Override
    public void ingestSource(Properties ctx, String sourceType, boolean forceRefresh) {
        IKnowledgeIngestor ingestor = findIngestor(sourceType);
        if (ingestor == null) {
            log.warning("No ingestor found for source type: " + sourceType);
            return;
        }

        try {
            IngestResult result = ingestor.ingest(ctx, forceRefresh);
            log.info("Ingested " + sourceType + ": " + result);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ingestion failed for " + sourceType, e);
        }
    }

    /**
     * Find ingestor by source type.
     */
    private IKnowledgeIngestor findIngestor(String sourceType) {
        return ingestors.stream()
            .filter(i -> i.getSourceType().equals(sourceType))
            .findFirst()
            .orElse(null);
    }

    @Override
    public Map<String, Object> getStats(Properties ctx) {
        Map<String, Object> stats = new HashMap<>();

        int clientId = Env.getAD_Client_ID(ctx);
        stats.put("clientId", clientId);
        stats.put("available", isAvailable());
        stats.put("registeredIngestors", ingestors.size());
        stats.put("sourceTypes", getSourceTypes());

        if (storeProvider != null) {
            stats.putAll(storeProvider.getStatistics(clientId));
        }

        return stats;
    }

    @Override
    public boolean isAvailable() {
        return storeProvider != null && storeProvider.isAvailable();
    }

    @Override
    public List<String> getSourceTypes() {
        List<String> types = new ArrayList<>();
        for (IKnowledgeIngestor ingestor : ingestors) {
            types.add(ingestor.getSourceType());
        }
        return types;
    }

    /**
     * Get or create embedding model.
     */
    private EmbeddingModel getEmbeddingModel(Properties ctx) {
        if (embeddingModel != null) {
            return embeddingModel;
        }

        synchronized (modelLock) {
            if (embeddingModel == null) {
                try {
                    MAIProvider provider = MAIProvider.get(ctx, DEFAULT_PROVIDER_ID, null);
                    if (provider != null) {
                        embeddingModel = LangChain4jProviderFactory.createEmbeddingModel(provider);
                        log.info("Created embedding model from provider: " + provider.getName());
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to create embedding model", e);
                }
            }
        }

        return embeddingModel;
    }

    /**
     * Truncate string for logging.
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
