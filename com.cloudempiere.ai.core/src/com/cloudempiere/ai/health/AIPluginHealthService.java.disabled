package com.cloudempiere.ai.health;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.cloudempiere.core.health.CheckResult;
import com.cloudempiere.core.health.IHealthService;
import com.cloudempiere.core.health.IPrerequisiteCheck;
import com.cloudempiere.core.health.PrerequisiteTier;

/**
 * AI Plugin health service that delegates to the core health service when available.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Delegation to core IHealthService when available</li>
 *   <li>Standalone operation when core is not deployed</li>
 *   <li>AI-specific health checks (providers, pgvector, tables)</li>
 * </ul>
 *
 * @author CloudEmpiere
 * @see ADR-050: Plugin Health and Prerequisite Verification
 */
public class AIPluginHealthService {

	private static final CLogger log = CLogger.getCLogger(AIPluginHealthService.class);

	/** Singleton instance */
	private static volatile AIPluginHealthService instance;

	/** Lock for initialization */
	private static final Object INIT_LOCK = new Object();

	/** Reference to core health service (set by AIHealthCheckRegistrar) */
	private static volatile IHealthService coreHealthService;

	/** Whether standalone health check has been performed */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/** Whether the plugin is healthy (standalone mode) */
	private final AtomicBoolean healthy = new AtomicBoolean(false);

	/** Status message */
	private final AtomicReference<String> statusMessage = new AtomicReference<>("Not initialized");

	/** Standalone checks (used when core service not available) */
	private final List<IPrerequisiteCheck> standaloneChecks = new ArrayList<>();

	private AIPluginHealthService() {
		registerStandaloneChecks();
	}

	/**
	 * Get the singleton instance.
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
	 * Set the core health service reference.
	 * Called by AIHealthCheckRegistrar when core service becomes available.
	 */
	public static void setCoreHealthService(IHealthService service) {
		coreHealthService = service;
		log.info("Core health service " + (service != null ? "bound" : "unbound"));
	}

	/**
	 * Register standalone health checks (used when core not available).
	 */
	private void registerStandaloneChecks() {
		standaloneChecks.add(new AITableExistsCheck("AIG_Provider", PrerequisiteTier.CRITICAL));
		standaloneChecks.add(new AIActiveProviderCheck());
		standaloneChecks.add(new AIPgVectorCheck());
	}

	/**
	 * @return true if plugin is healthy
	 */
	public boolean isHealthy() {
		// Delegate to core if available
		if (coreHealthService != null) {
			return coreHealthService.isHealthy();
		}

		// Standalone mode
		if (!initialized.get()) {
			performStandaloneCheck();
		}
		return healthy.get();
	}

	/**
	 * @return true if initialized
	 */
	public boolean isInitialized() {
		if (coreHealthService != null) {
			return coreHealthService.isInitialized();
		}
		return initialized.get();
	}

	/**
	 * @return User-friendly status message
	 */
	public String getStatusMessage() {
		if (coreHealthService != null) {
			return coreHealthService.getStatusMessage();
		}
		return statusMessage.get();
	}

	/**
	 * Force refresh of health checks.
	 */
	public void refresh() {
		if (coreHealthService != null) {
			coreHealthService.refresh();
		} else {
			initialized.set(false);
			performStandaloneCheck();
		}
	}

	/**
	 * Get health as JSON map.
	 */
	public Map<String, Object> getHealthAsJson() {
		if (coreHealthService != null) {
			return coreHealthService.getHealthAsJson();
		}

		// Simple standalone response
		return Map.of(
			"status", healthy.get() ? "HEALTHY" : "UNHEALTHY",
			"message", statusMessage.get(),
			"mode", "standalone"
		);
	}

	/**
	 * Get failures from last check.
	 */
	public List<CheckResult> getFailures() {
		if (coreHealthService != null) {
			return coreHealthService.getFailures();
		}
		return Collections.emptyList();
	}

	/**
	 * Check if a specific feature is available.
	 */
	public boolean isFeatureAvailable(String featureName) {
		if (coreHealthService != null) {
			return coreHealthService.isFeatureAvailable(featureName);
		}

		// Standalone feature checks
		if (!isHealthy()) {
			return false;
		}

		switch (featureName.toLowerCase()) {
			case "rag":
			case "embedding":
				return checkPgVectorAvailable();
			default:
				return true;
		}
	}

	/**
	 * Perform standalone health check (when core not available).
	 */
	private void performStandaloneCheck() {
		if (!Adempiere.isStarted()) {
			statusMessage.set("Waiting for server startup");
			return;
		}

		synchronized (INIT_LOCK) {
			if (initialized.get()) {
				return;
			}

			log.info("Performing standalone AI health check...");

			boolean allCriticalPassed = true;
			boolean allRequiredPassed = true;

			for (IPrerequisiteCheck check : standaloneChecks) {
				try {
					CheckResult result = check.execute();
					if (result.isFailed()) {
						if (check.getTier() == PrerequisiteTier.CRITICAL) {
							allCriticalPassed = false;
						} else if (check.getTier() == PrerequisiteTier.REQUIRED) {
							allRequiredPassed = false;
						}
						log.warning("Standalone check failed: " + result);
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Check error: " + check.getCheckName(), e);
					if (check.getTier() == PrerequisiteTier.CRITICAL) {
						allCriticalPassed = false;
					}
				}
			}

			if (!allCriticalPassed) {
				healthy.set(false);
				statusMessage.set("AI features unavailable - critical prerequisites missing");
			} else if (!allRequiredPassed) {
				healthy.set(true);
				statusMessage.set("AI features partially available");
			} else {
				healthy.set(true);
				statusMessage.set("AI features available");
			}

			initialized.set(true);
			log.info("Standalone health check complete: healthy=" + healthy.get());
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

	// =========================================================================
	// Standalone check implementations
	// =========================================================================

	private static class AITableExistsCheck implements IPrerequisiteCheck {
		private final String tableName;
		private final PrerequisiteTier tier;

		AITableExistsCheck(String tableName, PrerequisiteTier tier) {
			this.tableName = tableName;
			this.tier = tier;
		}

		@Override
		public String getCheckId() {
			return "ai.table." + tableName;
		}

		@Override
		public String getCheckName() {
			return "AI Table " + tableName;
		}

		@Override
		public PrerequisiteTier getTier() {
			return tier;
		}

		@Override
		public CheckResult execute() {
			String sql = "SELECT 1 FROM " + tableName + " WHERE 1=0";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
				pstmt.executeQuery();
				return CheckResult.passed(getCheckName(), tier);
			} catch (Exception e) {
				return CheckResult.failed(getCheckName(), tier,
						"Table " + tableName + " not found",
						"Deploy AI plugin 2Pack");
			}
		}
	}

	private static class AIActiveProviderCheck implements IPrerequisiteCheck {
		@Override
		public String getCheckId() {
			return "ai.provider.active";
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
		public CheckResult execute() {
			String sql = "SELECT COUNT(*) FROM AIG_Provider WHERE IsActive = 'Y'";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
				 ResultSet rs = pstmt.executeQuery()) {
				if (rs.next() && rs.getInt(1) > 0) {
					return CheckResult.passed(getCheckName(), getTier(),
							rs.getInt(1) + " active provider(s)");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"No active AI providers",
						"Configure an AI provider");
			} catch (Exception e) {
				return CheckResult.skipped(getCheckName(), getTier(),
						"AIG_Provider table not available");
			}
		}
	}

	private static class AIPgVectorCheck implements IPrerequisiteCheck {
		@Override
		public String getCheckId() {
			return "ai.extension.pgvector";
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
		public CheckResult execute() {
			String sql = "SELECT extversion FROM pg_extension WHERE extname = 'vector'";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null);
				 ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return CheckResult.passed(getCheckName(), getTier(),
							"pgvector " + rs.getString(1));
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"pgvector not installed - RAG disabled",
						"CREATE EXTENSION IF NOT EXISTS vector;");
			} catch (Exception e) {
				return CheckResult.skipped(getCheckName(), getTier(), "Not PostgreSQL");
			}
		}
	}
}
