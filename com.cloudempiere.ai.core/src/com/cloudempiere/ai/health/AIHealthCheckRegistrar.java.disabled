package com.cloudempiere.ai.health;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.cloudempiere.core.health.CheckResult;
import com.cloudempiere.core.health.IHealthService;
import com.cloudempiere.core.health.IPrerequisiteCheck;
import com.cloudempiere.core.health.PrerequisiteTier;

/**
 * Registers AI-specific health checks with the core health service.
 *
 * <p>This component has an OPTIONAL reference to IHealthService from
 * com.cloudempiere.core.base. When available, it:
 * <ul>
 *   <li>Registers AI-specific health checks</li>
 *   <li>Notifies AIPluginHealthService to delegate to core</li>
 * </ul>
 *
 * <p>When core is not available, AIPluginHealthService operates standalone.
 *
 * @author CloudEmpiere
 */
@Component(immediate = true)
public class AIHealthCheckRegistrar {

	private static final CLogger log = CLogger.getCLogger(AIHealthCheckRegistrar.class);

	private volatile IHealthService healthService;
	private volatile boolean registered = false;

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC,
		unbind = "unbindHealthService"
	)
	public void bindHealthService(IHealthService service) {
		this.healthService = service;
		log.info("Core IHealthService bound - registering AI health checks");

		// Notify AIPluginHealthService to delegate
		AIPluginHealthService.setCoreHealthService(service);

		registerAIChecks();
	}

	public void unbindHealthService(IHealthService service) {
		if (this.healthService == service) {
			log.info("Core IHealthService unbound");
			unregisterAIChecks();
			AIPluginHealthService.setCoreHealthService(null);
			this.healthService = null;
		}
	}

	@Activate
	public void activate() {
		log.fine("AIHealthCheckRegistrar activated");
	}

	@Deactivate
	public void deactivate() {
		log.fine("AIHealthCheckRegistrar deactivated");
		unregisterAIChecks();
	}

	private void registerAIChecks() {
		if (healthService == null || registered) {
			return;
		}

		try {
			// Register AI-specific checks
			healthService.registerCheck(new AIProviderTableCheck());
			healthService.registerCheck(new AIActiveProviderCheck());
			healthService.registerCheck(new AIPgVectorCheck());

			// Register AI features
			healthService.registerFeature("rag", "pgvector Extension");
			healthService.registerFeature("chat", "Active AI Provider");

			registered = true;
			log.info("Registered AI health checks with core service");

		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to register AI health checks", e);
		}
	}

	private void unregisterAIChecks() {
		if (healthService == null || !registered) {
			return;
		}

		try {
			healthService.unregisterCheck("ai.table.AIG_Provider");
			healthService.unregisterCheck("ai.provider.active");
			healthService.unregisterCheck("ai.extension.pgvector");
			registered = false;
		} catch (Exception e) {
			log.log(Level.FINE, "Error unregistering AI checks", e);
		}
	}

	// =========================================================================
	// AI-specific health checks
	// =========================================================================

	private static class AIProviderTableCheck implements IPrerequisiteCheck {
		@Override
		public String getCheckId() {
			return "ai.table.AIG_Provider";
		}

		@Override
		public String getCheckName() {
			return "AI Provider Table";
		}

		@Override
		public PrerequisiteTier getTier() {
			return PrerequisiteTier.CRITICAL;
		}

		@Override
		public String getDescription() {
			return "Verify AIG_Provider table exists for AI provider configuration";
		}

		@Override
		public CheckResult execute() {
			if (!Adempiere.isStarted()) {
				return CheckResult.unavailable(getCheckName(), getTier(), "Database not available");
			}

			String sql = "SELECT 1 FROM AIG_Provider WHERE 1=0";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
				pstmt.executeQuery();
				return CheckResult.passed(getCheckName(), getTier());
			} catch (Exception e) {
				return CheckResult.failed(getCheckName(), getTier(),
						"AIG_Provider table not found",
						"Deploy AI plugin 2Pack or run migration scripts");
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
					return CheckResult.passed(getCheckName(), getTier(),
							"pgvector " + rs.getString(1) + " installed");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"pgvector extension not installed - RAG features disabled",
						"Run: CREATE EXTENSION IF NOT EXISTS vector;");
			} catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().contains("pg_extension")) {
					return CheckResult.skipped(getCheckName(), getTier(),
							"Not a PostgreSQL database - pgvector not applicable");
				}
				return CheckResult.failed(getCheckName(), getTier(),
						"Error checking pgvector: " + e.getMessage(), e);
			}
		}
	}
}
