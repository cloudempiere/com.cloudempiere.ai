package com.cloudempiere.ai.kb.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.kb.boundary.KbDomainBoundary;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Knowledge Base domain tools for AI agents.
 *
 * <p>This class provides LangChain4j-annotated tools that KB agents can use
 * to search, retrieve, and analyze knowledge base articles. All queries are validated against
 * {@link KbDomainBoundary} and executed via {@link SecureDatabaseQueryExecutor}
 * for security.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Tools Layer (Knowledge Base)</p>
 * <p><b>Security:</b> All queries validated against KbDomainBoundary</p>
 * <p><b>Execution:</b> Queries run via SecureDatabaseQueryExecutor (role-based access)</p>
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
@Component(service = KbTools.class, immediate = true)
public class KbTools {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    /**
     * Search knowledge base articles by keyword or phrase.
     *
     * @param searchTerm the search keyword or phrase
     * @return list of matching articles as JSON
     */
    @Tool("Search knowledge base articles by keyword or phrase in title, summary, or description")
    public String searchArticles(@P("search keyword or phrase") String searchTerm) {
        KbDomainBoundary.validateReadTable("K_Entry");

        String sql =
            "SELECT ke.K_Entry_ID, ke.Name, ke.Summary, ke.DescriptionURL, " +
            "       ke.Rating, ke.ValidFrom, ke.ValidTo, " +
            "       kc.Name AS CategoryName, kt.Name AS TypeName, " +
            "       u.Name AS AuthorName, ke.IsPublic " +
            "FROM K_Entry ke " +
            "LEFT JOIN K_Category kc ON ke.K_Category_ID = kc.K_Category_ID " +
            "LEFT JOIN K_Type kt ON ke.K_Type_ID = kt.K_Type_ID " +
            "LEFT JOIN AD_User u ON ke.AD_User_ID = u.AD_User_ID " +
            "WHERE (LOWER(ke.Name) LIKE ? " +
            "   OR LOWER(ke.Summary) LIKE ? " +
            "   OR LOWER(ke.TextMsg) LIKE ?) " +
            "  AND ke.IsActive='Y' " +
            "ORDER BY ke.Rating DESC, ke.Updated DESC " +
            "LIMIT 20";

        String searchPattern = "%" + searchTerm.toLowerCase() + "%";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{searchPattern, searchPattern, searchPattern})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get knowledge base article details by ID.
     *
     * @param entryId the K_Entry_ID
     * @return article details as JSON
     */
    @Tool("Get knowledge base article details including full content, metadata, and comments")
    public String getArticle(@P("K_Entry_ID") int entryId) {
        KbDomainBoundary.validateReadTable("K_Entry");

        String sql =
            "SELECT ke.K_Entry_ID, ke.Name, ke.Summary, ke.TextMsg, ke.DescriptionURL, " +
            "       ke.Rating, ke.ValidFrom, ke.ValidTo, " +
            "       kc.K_Category_ID, kc.Name AS CategoryName, " +
            "       kt.K_Type_ID, kt.Name AS TypeName, " +
            "       ks.K_Source_ID, ks.Name AS SourceName, " +
            "       u.AD_User_ID, u.Name AS AuthorName, " +
            "       ke.IsPublic, ke.IsActive, ke.Created, ke.Updated " +
            "FROM K_Entry ke " +
            "LEFT JOIN K_Category kc ON ke.K_Category_ID = kc.K_Category_ID " +
            "LEFT JOIN K_Type kt ON ke.K_Type_ID = kt.K_Type_ID " +
            "LEFT JOIN K_Source ks ON ke.K_Source_ID = ks.K_Source_ID " +
            "LEFT JOIN AD_User u ON ke.AD_User_ID = u.AD_User_ID " +
            "WHERE ke.K_Entry_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{entryId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get comments for a knowledge base article.
     *
     * @param entryId the K_Entry_ID
     * @return list of comments as JSON
     */
    @Tool("Get all comments for a knowledge base article")
    public String getArticleComments(@P("K_Entry_ID") int entryId) {
        KbDomainBoundary.validateReadTable("K_Comment");

        String sql =
            "SELECT kc.K_Comment_ID, kc.TextMsg, kc.Rating, " +
            "       u.Name AS CommentByName, kc.Created " +
            "FROM K_Comment kc " +
            "LEFT JOIN AD_User u ON kc.AD_User_ID = u.AD_User_ID " +
            "WHERE kc.K_Entry_ID = ? " +
            "ORDER BY kc.Created DESC";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{entryId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get related articles for a knowledge base entry.
     *
     * @param entryId the K_Entry_ID
     * @param categoryId optional category filter
     * @return list of related articles as JSON
     */
    @Tool("Get related knowledge base articles based on category or keywords")
    public String getRelatedArticles(
        @P("K_Entry_ID") int entryId,
        @P("K_Category_ID (optional)") Integer categoryId
    ) {
        KbDomainBoundary.validateReadTable("K_Entry");

        StringBuilder sql = new StringBuilder(
            "SELECT ke2.K_Entry_ID, ke2.Name, ke2.Summary, ke2.Rating, " +
            "       kc.Name AS CategoryName " +
            "FROM K_Entry ke1 " +
            "INNER JOIN K_Entry ke2 ON (ke1.K_Category_ID = ke2.K_Category_ID " +
            "   OR ke1.K_Type_ID = ke2.K_Type_ID) " +
            "LEFT JOIN K_Category kc ON ke2.K_Category_ID = kc.K_Category_ID " +
            "WHERE ke1.K_Entry_ID = ? " +
            "  AND ke2.K_Entry_ID != ? " +
            "  AND ke2.IsActive='Y'"
        );

        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(entryId);
        params.add(entryId);

        if (categoryId != null) {
            sql.append(" AND ke2.K_Category_ID = ?");
            params.add(categoryId);
        }

        sql.append(" ORDER BY ke2.Rating DESC LIMIT 10");

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql.toString())
            .parameters(params.toArray())
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get knowledge base categories.
     *
     * @return list of categories as JSON
     */
    @Tool("Get available knowledge base categories")
    public String getCategories() {
        KbDomainBoundary.validateReadTable("K_Category");

        String sql =
            "SELECT kc.K_Category_ID, kc.Name, kc.Description, " +
            "       COUNT(ke.K_Entry_ID) AS ArticleCount " +
            "FROM K_Category kc " +
            "LEFT JOIN K_Entry ke ON kc.K_Category_ID = ke.K_Category_ID AND ke.IsActive='Y' " +
            "WHERE kc.IsActive='Y' " +
            "GROUP BY kc.K_Category_ID, kc.Name, kc.Description " +
            "ORDER BY kc.Name";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get top-rated knowledge base articles.
     *
     * @param limit number of articles to return (max 50)
     * @return list of top-rated articles as JSON
     */
    @Tool("Get top-rated knowledge base articles based on user ratings")
    public String getTopRatedArticles(@P("limit (max 50)") Integer limit) {
        KbDomainBoundary.validateReadTable("K_Entry");

        int resultLimit = (limit != null && limit > 0 && limit <= 50) ? limit : 10;

        String sql =
            "SELECT ke.K_Entry_ID, ke.Name, ke.Summary, ke.Rating, " +
            "       kc.Name AS CategoryName, kt.Name AS TypeName, " +
            "       u.Name AS AuthorName " +
            "FROM K_Entry ke " +
            "LEFT JOIN K_Category kc ON ke.K_Category_ID = kc.K_Category_ID " +
            "LEFT JOIN K_Type kt ON ke.K_Type_ID = kt.K_Type_ID " +
            "LEFT JOIN AD_User u ON ke.AD_User_ID = u.AD_User_ID " +
            "WHERE ke.IsActive='Y' AND ke.Rating > 0 " +
            "ORDER BY ke.Rating DESC, ke.Updated DESC " +
            "LIMIT ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{resultLimit})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }
}
