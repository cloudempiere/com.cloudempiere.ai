package com.cloudempiere.ai.provider.dto;

import java.util.HashMap;
import java.util.Map;

import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.util.Env;

/**
 * Represents a reference to an iDempiere record that can be zoomed to.
 *
 * <p>This DTO captures metadata about a record reference found in AI responses,
 * enabling clickable drill-down navigation to the actual record in iDempiere.
 *
 * <p>References can be created:
 * <ul>
 *   <li>From LLM-provided structured output (preferred)</li>
 *   <li>From pattern extraction (e.g., "SO-1234" → C_Order lookup)</li>
 *   <li>From explicit zoom link syntax [[Table:ID|Display]]</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * RecordReference ref = RecordReference.builder()
 *     .tableName("C_Order")
 *     .recordId(1234)
 *     .displayValue("SO-1234")
 *     .build();
 *
 * if (ref.isAccessible(roleId)) {
 *     MQuery query = ref.toMQuery();
 *     AEnv.zoom(query);
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @see com.cloudempiere.ai.util.RecordReferenceExtractor
 * @see com.cloudempiere.ai.util.ZoomLinkProcessor
 */
public class RecordReference {

	/** Table name (e.g., "C_Order") */
	private final String tableName;

	/** Record ID */
	private final int recordId;

	/** Display value shown to user (e.g., "SO-1234") */
	private final String displayValue;

	/** Column name for MQuery (e.g., "C_Order_ID") - derived if not provided */
	private final String columnName;

	/** Optional: specific AD_Window_ID for direct window targeting */
	private final Integer windowId;

	/** Start position in original text (for replacement) */
	private final int startIndex;

	/** End position in original text (for replacement) */
	private final int endIndex;

	/** Optional: AD_Table_ID (cached for performance) */
	private Integer tableId;

	/** Optional: additional metadata */
	private final Map<String, Object> metadata;

	/**
	 * Private constructor - use Builder
	 */
	private RecordReference(Builder builder) {
		this.tableName = builder.tableName;
		this.recordId = builder.recordId;
		this.displayValue = builder.displayValue;
		this.columnName = builder.columnName != null ? builder.columnName : (tableName + "_ID");
		this.windowId = builder.windowId;
		this.startIndex = builder.startIndex;
		this.endIndex = builder.endIndex;
		this.tableId = builder.tableId;
		this.metadata = builder.metadata != null ? new HashMap<>(builder.metadata) : new HashMap<>();
	}

	/**
	 * Create a new builder
	 * @return Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create MQuery for zoom operation.
	 *
	 * <p>The returned query can be used with AEnv.zoom(query) or AEnv.zoom(windowId, query)
	 *
	 * @return MQuery configured for zooming to this record
	 */
	public MQuery toMQuery() {
		MQuery query = new MQuery(tableName);
		query.addRestriction(columnName, MQuery.EQUAL, recordId);
		query.setZoomTableName(tableName);
		query.setZoomColumnName(columnName);
		query.setZoomValue(recordId);
		if (windowId != null && windowId > 0) {
			query.setZoomWindowID(windowId);
		}
		return query;
	}

	/**
	 * Check if user has table-level access to this record.
	 *
	 * <p>Note: This checks table access, not record-level security.
	 * Full record access is validated when the window opens.
	 *
	 * @param AD_Role_ID role to check
	 * @return true if role can access this table
	 */
	public boolean isAccessible(int AD_Role_ID) {
		MRole role = MRole.get(Env.getCtx(), AD_Role_ID);
		int tblId = getTableId();
		if (tblId <= 0) {
			return false;
		}
		return role.isTableAccess(tblId, false);
	}

	/**
	 * Get AD_Table_ID, looking up from tableName if needed
	 * @return AD_Table_ID or 0 if table not found
	 */
	public int getTableId() {
		if (tableId == null) {
			tableId = MTable.getTable_ID(tableName, null);
		}
		return tableId != null ? tableId : 0;
	}

	// Getters

	public String getTableName() {
		return tableName;
	}

	public int getRecordId() {
		return recordId;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public String getColumnName() {
		return columnName;
	}

	public Integer getWindowId() {
		return windowId;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public Map<String, Object> getMetadata() {
		return new HashMap<>(metadata);
	}

	/**
	 * Get metadata value
	 * @param key metadata key
	 * @return value or null
	 */
	public Object getMetadata(String key) {
		return metadata.get(key);
	}

	@Override
	public String toString() {
		return "RecordReference[" + tableName + ":" + recordId + "|" + displayValue +
			   " @" + startIndex + "-" + endIndex + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		RecordReference other = (RecordReference) obj;
		return recordId == other.recordId &&
			   tableName != null && tableName.equals(other.tableName);
	}

	@Override
	public int hashCode() {
		int result = tableName != null ? tableName.hashCode() : 0;
		result = 31 * result + recordId;
		return result;
	}

	/**
	 * Builder for RecordReference
	 */
	public static class Builder {
		private String tableName;
		private int recordId;
		private String displayValue;
		private String columnName;
		private Integer windowId;
		private int startIndex = -1;
		private int endIndex = -1;
		private Integer tableId;
		private Map<String, Object> metadata;

		public Builder tableName(String tableName) {
			this.tableName = tableName;
			return this;
		}

		public Builder recordId(int recordId) {
			this.recordId = recordId;
			return this;
		}

		public Builder displayValue(String displayValue) {
			this.displayValue = displayValue;
			return this;
		}

		public Builder columnName(String columnName) {
			this.columnName = columnName;
			return this;
		}

		public Builder windowId(Integer windowId) {
			this.windowId = windowId;
			return this;
		}

		public Builder startIndex(int startIndex) {
			this.startIndex = startIndex;
			return this;
		}

		public Builder endIndex(int endIndex) {
			this.endIndex = endIndex;
			return this;
		}

		public Builder tableId(Integer tableId) {
			this.tableId = tableId;
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		public Builder addMetadata(String key, Object value) {
			if (this.metadata == null) {
				this.metadata = new HashMap<>();
			}
			this.metadata.put(key, value);
			return this;
		}

		/**
		 * Build the RecordReference
		 * @return RecordReference instance
		 * @throws IllegalArgumentException if required fields are missing
		 */
		public RecordReference build() {
			if (tableName == null || tableName.isEmpty()) {
				throw new IllegalArgumentException("tableName is required");
			}
			if (recordId <= 0) {
				throw new IllegalArgumentException("recordId must be positive");
			}
			if (displayValue == null || displayValue.isEmpty()) {
				// Default display value
				displayValue = tableName + "#" + recordId;
			}
			return new RecordReference(this);
		}
	}
}
