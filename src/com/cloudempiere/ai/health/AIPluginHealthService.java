package com.cloudempiere.ai.health;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.model.MIssue;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Centralized health service for AI plugin prerequisites.
 *
 * <p>Implements tiered health checking with lazy initialization:
 * <ul>
 *   <li>Waits for server to be fully started before checking</li>
 *   <li>Caches results until explicit refresh</li>
 *   <li>Supports GRACEFUL (default) and STRICT modes</li>
 *   <li>Creates AD_Issue for admin notification on failures</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * AIPluginHealthService health = AIPluginHealthService.getInstance();
 * if (health != null &amp;&amp; health.isHealthy()) {
 *     // Safe to use AI features
 * }
 * </pre>
 *
 * @see ADR-050: Plugin Health and Prerequisite Verification
 * @see ADR-051: ZK UI Defensive Programming
 */
public class AIPluginHealthService {

	private static final CLogger log = CLogger.getCLogger(AIPluginHealthService.class);

	/** System property for strict mode */
	private static final String PROP_STRICT_MODE = "ai.plugin.mode";

	/** Singleton instance */
	private static volatile AIPluginHealthService instance;

	/** Lock for initialization */
	private static final Object INIT_LOCK = new Object();

	/** Whether initialization has been attempted */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/** Whether the plugin is healthy (all critical/required checks pass) */
	private final AtomicBoolean healthy = new AtomicBoolean(false);

	/** Last health check results by tier */
	private final AtomicReference<Map<PrerequisiteTier, List<CheckResult>>> lastResults =
			new AtomicReference<>(Collections.emptyMap());

	/** Registered prerequisite checks */
	private final List<IPrerequisiteCheck> checks = new ArrayList<>();

	/** User-friendly status message */
	private final AtomicReference<String> statusMessage = new AtomicReference<>("Not initialized");

	private AIPluginHealthService() {
		registerDefaultChecks();
	}

	/**
	 * Get the singleton instance.
	 * @return AIPluginHealthService instance (never null)
	 */
	public static AIPluginHealthService getInstance() {
		if (instance == null) {
			synchronized (INIT_LOCK) {
				if (instance == null) {
					instance = new AIPluginHealthService();
				}
			}
		}
		return instance;
	}

	/**
	 * Register default prerequisite checks.
	 */
	private void registerDefaultChecks() {
		// Dynamically discover all AIG_* tables from model package
		discoverAndRegisterTableChecks();

		// Required: Active provider check
		checks.add(new ActiveProviderCheck());

		// Optional: pgvector extension for vector embeddings
		checks.add(new PgVectorExtensionCheck());

		// External: AI provider services (checked asynchronously)
		checks.add(new OllamaServiceCheck());
	}

	/**
	 * Dynamically discover AIG_* tables from model interfaces and register checks.
	 * Scans com.cloudempiere.ai.model package for I_AIG_* interfaces.
	 */
	private void discoverAndRegisterTableChecks() {
		// Define tier classification for existing tables only
		// Tables are discovered dynamically from AD_Table, this map only defines criticality
		Map<String, PrerequisiteTier> tierMap = new LinkedHashMap<>();

		// Critical: Core provider infrastructure
		tierMap.put("AIG_Provider", PrerequisiteTier.CRITICAL);

		// Required: Essential functionality
		tierMap.put("AIG_Budget", PrerequisiteTier.REQUIRED);
		tierMap.put("AIG_UsageMetrics", PrerequisiteTier.REQUIRED);

		// Optional: Enhanced features and audit
		tierMap.put("AIG_ChatOwnership", PrerequisiteTier.OPTIONAL);
		tierMap.put("AIG_Embedding", PrerequisiteTier.OPTIONAL);
		tierMap.put("AIG_QueryAudit", PrerequisiteTier.OPTIONAL);
		tierMap.put("AIG_Prompt_Config", PrerequisiteTier.OPTIONAL);

		try {
			// Scan model package for I_AIG_* interfaces
			String packageName = "com.cloudempiere.ai.model";
			Class<?>[] modelClasses = getModelClasses(packageName);

			for (Class<?> modelClass : modelClasses) {
				String className = modelClass.getSimpleName();

				// Extract table name from interface (I_AIG_Provider -> AIG_Provider)
				if (className.startsWith("I_AIG_")) {
					String tableName = className.substring(2); // Remove "I_" prefix

					// Get tier from map, default to REQUIRED if not specified
					PrerequisiteTier tier = tierMap.getOrDefault(tableName, PrerequisiteTier.REQUIRED);

					String resolution = getResolutionForTable(tableName, tier);
					checks.add(new TableExistsCheck(tableName, tier, resolution));

					log.fine("Registered health check for table: " + tableName + " (" + tier + ")");
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Could not discover model classes, falling back to hardcoded list", e);
			// Fallback: Register only critical tables manually
			checks.add(new TableExistsCheck("AIG_Provider", PrerequisiteTier.CRITICAL,
					"Deploy AI plugin 2Pack or run migration scripts"));
		}
	}

	/**
	 * Get all model classes from a package by discovering AIG_* tables from database.
	 * This approach is truly dynamic - no hardcoded table list needed!
	 *
	 * Strategy:
	 * 1. Query AD_Table for all AIG_* tables registered in the Application Dictionary
	 * 2. Try to load corresponding I_AIG_* interface for each table
	 * 3. Only register health checks for tables that have model interfaces
	 */
	private Class<?>[] getModelClasses(String packageName) throws Exception {
		List<Class<?>> classes = new ArrayList<>();

		// Get classloader from this bundle
		ClassLoader classLoader = this.getClass().getClassLoader();

		try {
			// Query AD_Table for all AIG_* tables
			String sql = "SELECT TableName FROM AD_Table WHERE TableName LIKE 'AIG_%' AND IsActive = 'Y' ORDER BY TableName";

			List<String> tableNames = new ArrayList<>();
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
				 ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					tableNames.add(rs.getString(1));
				}
			}

			log.fine("Discovered " + tableNames.size() + " AIG_* tables from AD_Table");

			// Try to load model interface for each discovered table
			for (String tableName : tableNames) {
				try {
					Class<?> clazz = classLoader.loadClass(packageName + ".I_" + tableName);
					if (clazz.isInterface()) {
						classes.add(clazz);
						log.fine("Loaded model interface: I_" + tableName);
					}
				} catch (ClassNotFoundException e) {
					// Model interface not yet generated/implemented
					// This is expected during development when table exists but model isn't generated
					log.fine("Model interface not found for table: " + tableName + " - skipping health check");
				}
			}

		} catch (Exception e) {
			log.log(Level.WARNING, "Error discovering AIG_* tables from database", e);
		}

		return classes.toArray(new Class<?>[0]);
	}

	/**
	 * Get resolution steps for a table based on its tier.
	 */
	private String getResolutionForTable(String tableName, PrerequisiteTier tier) {
		switch (tier) {
			case CRITICAL:
				return "Deploy AI plugin 2Pack or run migration scripts";
			case REQUIRED:
				if (tableName.startsWith("AIG_Chat")) {
					return "Deploy AI plugin 2Pack for chat functionality";
				} else if (tableName.contains("Budget") || tableName.contains("Usage")) {
					return "Deploy AI plugin 2Pack for cost tracking";
				}
				return "Deploy AI plugin 2Pack or run migration scripts";
			case OPTIONAL:
				if (tableName.contains("Knowledge") || tableName.contains("Embedding")) {
					return "Deploy RAG tables for knowledge base functionality";
				}
				return "Deploy optional AI plugin tables";
			default:
				return "Check AI plugin deployment";
		}
	}

	/**
	 * Perform health check. Thread-safe, only runs once unless refresh() called.
	 */
	public void performHealthCheck() {
		if (!Adempiere.isStarted()) {
			log.fine("Server not started yet - deferring health check");
			statusMessage.set("Waiting for server startup");
			return;
		}

		if (initialized.get()) {
			log.fine("Health check already performed - use refresh() to re-check");
			return;
		}

		synchronized (INIT_LOCK) {
			if (initialized.get()) {
				return;
			}

			log.info("Performing AI plugin health check...");
			long startTime = System.currentTimeMillis();

			Map<PrerequisiteTier, List<CheckResult>> results = new EnumMap<>(PrerequisiteTier.class);
			for (PrerequisiteTier tier : PrerequisiteTier.values()) {
				results.put(tier, new ArrayList<>());
			}

			boolean allCriticalPassed = true;
			boolean allRequiredPassed = true;
			List<CheckResult> failures = new ArrayList<>();

			for (IPrerequisiteCheck check : checks) {
				try {
					CheckResult result = check.execute();
					results.get(check.getTier()).add(result);

					if (result.isFailed()) {
						failures.add(result);
						if (check.getTier() == PrerequisiteTier.CRITICAL) {
							allCriticalPassed = false;
						} else if (check.getTier() == PrerequisiteTier.REQUIRED) {
							allRequiredPassed = false;
						}
						log.warning("Health check failed: " + result);
					} else {
						log.fine("Health check passed: " + result.getCheckName());
					}
				} catch (Exception e) {
					CheckResult result = CheckResult.failed(
							check.getCheckName(), check.getTier(),
							"Check threw exception: " + e.getMessage(), e);
					results.get(check.getTier()).add(result);
					failures.add(result);
					log.log(Level.WARNING, "Health check exception: " + check.getCheckName(), e);
				}
			}

			lastResults.set(Collections.unmodifiableMap(results));

			boolean isStrictMode = "strict".equalsIgnoreCase(System.getProperty(PROP_STRICT_MODE));

			// Create AD_Issue for ANY failures (CRITICAL, REQUIRED, or OPTIONAL) to notify admins
			if (!failures.isEmpty()) {
				createAdminIssue(failures);
			}

			if (!allCriticalPassed) {
				healthy.set(false);
				statusMessage.set("AI features unavailable - critical prerequisites missing");
			} else if (!allRequiredPassed && isStrictMode) {
				healthy.set(false);
				statusMessage.set("AI features unavailable (strict mode) - required prerequisites missing");
			} else if (!allRequiredPassed) {
				healthy.set(true); // Graceful mode - continue with warnings
				statusMessage.set("AI features partially available - some prerequisites missing");
				log.warning("AI plugin running in degraded mode - some features unavailable");
			} else if (!failures.isEmpty()) {
				// All critical/required passed, but optional failures exist
				healthy.set(true);
				statusMessage.set("AI features available - optional features unavailable");
				log.info("AI plugin healthy but with optional prerequisites missing (e.g., pgvector)");
			} else {
				healthy.set(true);
				statusMessage.set("AI features available");
			}

			initialized.set(true);
			long duration = System.currentTimeMillis() - startTime;
			log.info("AI plugin health check completed in " + duration + "ms - healthy=" + healthy.get());
		}
	}

	/**
	 * Force re-check of all prerequisites.
	 * Thread-safe: synchronizes with performHealthCheck to prevent race conditions.
	 */
	public void refresh() {
		synchronized (INIT_LOCK) {
			initialized.set(false);
			healthy.set(false);
		}
		performHealthCheck();
	}

	/**
	 * @return true if plugin is healthy and ready to use
	 */
	public boolean isHealthy() {
		if (!initialized.get()) {
			performHealthCheck();
		}
		return healthy.get();
	}

	/**
	 * @return true if initialization has completed
	 */
	public boolean isInitialized() {
		return initialized.get();
	}

	/**
	 * @return User-friendly status message
	 */
	public String getStatusMessage() {
		return statusMessage.get();
	}

	/**
	 * @return Immutable map of last check results by tier
	 */
	public Map<PrerequisiteTier, List<CheckResult>> getLastResults() {
		return lastResults.get();
	}

	/**
	 * Get failures from last check.
	 */
	public List<CheckResult> getFailures() {
		List<CheckResult> failures = new ArrayList<>();
		for (List<CheckResult> tierResults : lastResults.get().values()) {
			for (CheckResult result : tierResults) {
				if (result.isFailed()) {
					failures.add(result);
				}
			}
		}
		return failures;
	}

	/**
	 * Register additional prerequisite check.
	 */
	public void registerCheck(IPrerequisiteCheck check) {
		synchronized (checks) {
			checks.add(check);
		}
	}

	/**
	 * Create AD_Issue for admin notification.
	 * Uses proper transaction management and multi-tenancy.
	 */
	private void createAdminIssue(List<CheckResult> failures) {
		String trxName = null;
		try {
			Properties ctx = Env.getCtx();
			int AD_Client_ID = Env.getAD_Client_ID(ctx);

			StringBuilder summary = new StringBuilder();
			summary.append("AI Plugin Prerequisites Not Met\n\n");
			summary.append("The following prerequisites failed:\n\n");

			for (CheckResult failure : failures) {
				summary.append("[").append(failure.getTier().getDisplayName()).append("] ");
				summary.append(failure.getCheckName()).append(": ");
				summary.append(failure.getMessage()).append("\n");
				if (failure.hasResolution()) {
					summary.append("  Resolution: ").append(failure.getResolutionSteps()).append("\n");
				}
			}

			summary.append("\nTo enable AI features, resolve the issues above and restart the server.");

			// Create issue with proper transaction
			trxName = org.compiere.util.Trx.createTrxName("AIHealth");
			org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, true);

			try {
				// Create context with proper client/org for MIssue (PO reads from ctx in constructor)
				Properties issueCtx = new Properties(ctx);
				Env.setContext(issueCtx, Env.AD_CLIENT_ID, AD_Client_ID);
				Env.setContext(issueCtx, Env.AD_ORG_ID, 0); // Client-level issue

				MIssue issue = new MIssue(issueCtx, 0, trxName);
				issue.setName("AI Plugin Prerequisites Failed");
				issue.setIssueSummary(summary.toString());
				issue.setSourceClassName(AIPluginHealthService.class.getName());
				issue.setSourceMethodName("performHealthCheck");
				issue.setLoggerName(log.getName());
				issue.setIsReproducible("Y");
				issue.saveEx();
				trx.commit();

				log.info("Created AD_Issue for AI plugin prerequisite failures: " + issue.getAD_Issue_ID());
			} catch (Exception e) {
				trx.rollback();
				throw e;
			} finally {
				trx.close();
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Could not create AD_Issue for prerequisite failures", e);
		}
	}

	/**
	 * Get health status as JSON map for monitoring endpoints.
	 * @return Map suitable for JSON serialization
	 */
	public Map<String, Object> getHealthAsJson() {
		if (!initialized.get()) {
			performHealthCheck();
		}

		Map<String, Object> json = new LinkedHashMap<>();
		json.put("status", healthy.get() ? "HEALTHY" : "UNHEALTHY");
		json.put("timestamp", System.currentTimeMillis());
		json.put("mode", "strict".equalsIgnoreCase(System.getProperty(PROP_STRICT_MODE)) ? "STRICT" : "GRACEFUL");
		json.put("message", statusMessage.get());

		// Build tiers section
		Map<String, Object> tiers = new LinkedHashMap<>();
		Map<PrerequisiteTier, List<CheckResult>> results = lastResults.get();

		for (PrerequisiteTier tier : PrerequisiteTier.values()) {
			Map<String, Object> tierData = new LinkedHashMap<>();
			List<CheckResult> tierResults = results.getOrDefault(tier, Collections.emptyList());

			boolean tierHealthy = tierResults.stream().noneMatch(CheckResult::isFailed);
			tierData.put("status", tierHealthy ? "HEALTHY" : "UNHEALTHY");

			Map<String, Object> checksMap = new LinkedHashMap<>();
			List<String> missing = new ArrayList<>();

			for (CheckResult result : tierResults) {
				checksMap.put(result.getCheckName(), result.isPassed());
				if (result.isFailed()) {
					missing.add(result.getCheckName());
				}
			}

			tierData.put("checks", checksMap);
			if (!missing.isEmpty()) {
				tierData.put("missing", missing);
			}

			tiers.put(tier.name().toLowerCase(), tierData);
		}

		json.put("tiers", tiers);

		// Add resolution summary if unhealthy
		if (!healthy.get()) {
			List<String> resolutions = new ArrayList<>();
			for (CheckResult failure : getFailures()) {
				if (failure.hasResolution()) {
					resolutions.add(failure.getResolutionSteps());
				}
			}
			if (!resolutions.isEmpty()) {
				json.put("resolutions", resolutions);
			}
		}

		return json;
	}

	/**
	 * Check if a specific feature is available.
	 * @param featureName Feature identifier (e.g., "rag", "embedding", "chat")
	 * @return true if the feature's prerequisites are met
	 */
	public boolean isFeatureAvailable(String featureName) {
		if (!isHealthy()) {
			return false;
		}

		switch (featureName.toLowerCase()) {
			case "rag":
			case "embedding":
				// RAG requires pgvector and embedding table
				return checkPgVectorAvailable() && checkTableExists("AIG_Embedding");
			case "chat":
				// Chat requires base tables and at least one provider
				return checkTableExists("AIG_Chat") && checkActiveProviderExists();
			default:
				return true;
		}
	}

	private boolean checkPgVectorAvailable() {
		String sql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
			 ResultSet rs = pstmt.executeQuery()) {
			return rs.next();
		} catch (Exception e) {
			return false;
		}
	}

	private boolean checkTableExists(String tableName) {
		String sql = "SELECT 1 FROM " + tableName + " WHERE 1=0";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.executeQuery();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean checkActiveProviderExists() {
		String sql = "SELECT 1 FROM AIG_Provider WHERE IsActive = 'Y'";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
			 ResultSet rs = pstmt.executeQuery()) {
			return rs.next();
		} catch (Exception e) {
			return false;
		}
	}

	// =========================================================================
	// Inner classes: Prerequisite checks
	// =========================================================================

	/**
	 * Check if a database table exists (both in AD_Table and physically).
	 */
	private static class TableExistsCheck implements IPrerequisiteCheck {
		private final String tableName;
		private final PrerequisiteTier tier;
		private final String resolution;

		TableExistsCheck(String tableName, PrerequisiteTier tier, String resolution) {
			this.tableName = tableName;
			this.tier = tier;
			this.resolution = resolution;
		}

		@Override
		public String getCheckId() {
			return "table." + tableName;
		}

		@Override
		public String getCheckName() {
			return "Table " + tableName;
		}

		@Override
		public PrerequisiteTier getTier() {
			return tier;
		}

		@Override
		public String getDescription() {
			return "Verify table " + tableName + " exists in database";
		}

		@Override
		public CheckResult execute() {
			if (!Adempiere.isStarted()) {
				return CheckResult.unavailable(getCheckName(), tier, "Database not available");
			}

			// Step 1: Check if table is registered in Application Dictionary
			String adSql = "SELECT COUNT(*) FROM AD_Table WHERE TableName = ? AND IsActive = 'Y'";
			try (PreparedStatement pstmt = DB.prepareStatement(adSql, null)) {
				pstmt.setString(1, tableName);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (!rs.next() || rs.getInt(1) == 0) {
						return CheckResult.failed(getCheckName(), tier,
								"Table " + tableName + " not registered in AD_Table", resolution);
					}
				}
			} catch (Exception e) {
				return CheckResult.failed(getCheckName(), tier,
						"Error checking AD_Table: " + e.getMessage(), e);
			}

			// Step 2: Check if physical table exists by attempting a simple query
			String physicalSql = "SELECT 1 FROM " + tableName + " WHERE 1=0";
			try (PreparedStatement pstmt = DB.prepareStatement(physicalSql, null)) {
				pstmt.executeQuery();
				// If we get here, the physical table exists
				return CheckResult.passed(getCheckName(), tier);
			} catch (Exception e) {
				// Physical table doesn't exist or query failed
				return CheckResult.failed(getCheckName(), tier,
						"Physical table " + tableName + " not found - run migration scripts", resolution);
			}
		}
	}

	/**
	 * Check if at least one AI provider is configured and active.
	 */
	private static class ActiveProviderCheck implements IPrerequisiteCheck {

		@Override
		public String getCheckId() {
			return "provider.active";
		}

		@Override
		public String getCheckName() {
			return "Active AI Provider";
		}

		@Override
		public PrerequisiteTier getTier() {
			return PrerequisiteTier.REQUIRED;
		}

		@Override
		public String getDescription() {
			return "Verify at least one AI provider is configured and active";
		}

		@Override
		public CheckResult execute() {
			if (!Adempiere.isStarted()) {
				return CheckResult.unavailable(getCheckName(), getTier(), "Database not available");
			}

			// First check if table exists
			String tableCheck = "SELECT 1 FROM AIG_Provider WHERE 1=0";
			try (PreparedStatement pstmt = DB.prepareStatement(tableCheck, null)) {
				pstmt.executeQuery();
			} catch (Exception e) {
				// Table doesn't exist - skip this check (table check will handle it)
				return CheckResult.skipped(getCheckName(), getTier(), "AIG_Provider table not available");
			}

			// Check for active provider
			String sql = "SELECT COUNT(*) FROM AIG_Provider WHERE IsActive = 'Y'";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
				 ResultSet rs = pstmt.executeQuery()) {
				if (rs.next() && rs.getInt(1) > 0) {
					return CheckResult.passed(getCheckName(), getTier(),
							rs.getInt(1) + " active provider(s) configured");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"No active AI providers configured",
						"Configure at least one AI provider in the AIG_Provider window");
			} catch (Exception e) {
				return CheckResult.failed(getCheckName(), getTier(),
						"Error checking providers: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Check if pgvector PostgreSQL extension is installed.
	 */
	private static class PgVectorExtensionCheck implements IPrerequisiteCheck {

		@Override
		public String getCheckId() {
			return "extension.pgvector";
		}

		@Override
		public String getCheckName() {
			return "pgvector Extension";
		}

		@Override
		public PrerequisiteTier getTier() {
			return PrerequisiteTier.OPTIONAL;
		}

		@Override
		public String getDescription() {
			return "Verify pgvector PostgreSQL extension is installed for vector embeddings";
		}

		@Override
		public CheckResult execute() {
			if (!Adempiere.isStarted()) {
				return CheckResult.unavailable(getCheckName(), getTier(), "Database not available");
			}

			String sql = "SELECT extversion FROM pg_extension WHERE extname = 'vector'";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
				 ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String version = rs.getString(1);
					return CheckResult.passed(getCheckName(), getTier(),
							"pgvector " + version + " installed");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"pgvector extension not installed - RAG features disabled",
						"Run: CREATE EXTENSION IF NOT EXISTS vector;");
			} catch (Exception e) {
				// Might be Oracle or other DB - pgvector is PostgreSQL only
				if (e.getMessage() != null && e.getMessage().contains("pg_extension")) {
					return CheckResult.skipped(getCheckName(), getTier(),
							"Not a PostgreSQL database - pgvector not applicable");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"Error checking pgvector: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Check if Ollama local LLM server is reachable.
	 */
	private static class OllamaServiceCheck implements IPrerequisiteCheck {

		private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
		private static final int TIMEOUT_MS = 3000;

		@Override
		public String getCheckId() {
			return "external.ollama";
		}

		@Override
		public String getCheckName() {
			return "Ollama Service";
		}

		@Override
		public PrerequisiteTier getTier() {
			return PrerequisiteTier.EXTERNAL;
		}

		@Override
		public String getDescription() {
			return "Check if Ollama local LLM server is reachable";
		}

		@Override
		public boolean requiresDatabase() {
			return false;
		}

		@Override
		public CheckResult execute() {
			String ollamaUrl = System.getProperty("ollama.base.url", DEFAULT_OLLAMA_URL);

			try {
				// Use async check with timeout
				CompletableFuture<CheckResult> future = CompletableFuture.supplyAsync(() -> {
					try {
						URL url = new URL(ollamaUrl + "/api/tags");
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setConnectTimeout(TIMEOUT_MS);
						conn.setReadTimeout(TIMEOUT_MS);
						conn.setRequestMethod("GET");

						int responseCode = conn.getResponseCode();
						conn.disconnect();

						if (responseCode == 200) {
							return CheckResult.passed(getCheckName(), getTier(),
									"Ollama reachable at " + ollamaUrl);
						} else {
							return CheckResult.failed(getCheckName(), getTier(),
									"Ollama returned HTTP " + responseCode,
									"Check Ollama server status: ollama serve");
						}
					} catch (Exception e) {
						return CheckResult.failed(getCheckName(), getTier(),
								"Cannot reach Ollama at " + ollamaUrl + ": " + e.getMessage(),
								"Start Ollama: ollama serve");
					}
				});

				return future.get(TIMEOUT_MS + 1000, TimeUnit.MILLISECONDS);

			} catch (Exception e) {
				return CheckResult.failed(getCheckName(), getTier(),
						"Ollama check timed out",
						"Start Ollama: ollama serve");
			}
		}
	}
}
