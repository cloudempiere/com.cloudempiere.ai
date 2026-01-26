-- Migration script for AI Database Access feature
-- Ticket: CLD-1601
-- Purpose: Add database access infrastructure for AI agents
-- Author: Cloudempiere
-- Date: 2025-11-18
SELECT register_migration_script('202511181000_CLD-1601.sql') FROM dual;

-- =====================================================================
-- 1. Add AD_User_ID to AIG_Provider table
-- =====================================================================

-- Add column
ALTER TABLE AIG_Provider ADD COLUMN AD_User_ID NUMERIC(10);

-- Add foreign key constraint
ALTER TABLE AIG_Provider
  ADD CONSTRAINT ADUser_AIGProvider
  FOREIGN KEY (AD_User_ID)
  REFERENCES AD_User(AD_User_ID)
  DEFERRABLE INITIALLY DEFERRED;

-- Add comment
COMMENT ON COLUMN AIG_Provider.AD_User_ID IS 'System user representing this AI provider for audit trail purposes';

-- =====================================================================
-- 2. Create AIG_QueryAudit table
-- =====================================================================

CREATE TABLE AIG_QueryAudit (
    AIG_QueryAudit_ID       NUMERIC(10) NOT NULL,
    AD_Client_ID            NUMERIC(10) NOT NULL,
    AD_Org_ID               NUMERIC(10) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')),
    Created                 TIMESTAMP DEFAULT NOW() NOT NULL,
    CreatedBy               NUMERIC(10) NOT NULL,
    Updated                 TIMESTAMP DEFAULT NOW() NOT NULL,
    UpdatedBy               NUMERIC(10) NOT NULL,
    AIG_QueryAudit_UU       VARCHAR(36) DEFAULT uuid_generate_v4(),

    -- AI Provider reference
    AIG_Provider_ID         NUMERIC(10) NOT NULL,

    -- User context (the logged-in user)
    AD_User_ID              NUMERIC(10) NOT NULL,
    AD_Role_ID              NUMERIC(10) NOT NULL,

    -- Query details
    QuerySQL                TEXT NOT NULL,
    SecuredSQL              TEXT,
    QueryPurpose            VARCHAR(255),
    ContextType             VARCHAR(60),

    -- Execution results
    Status                  VARCHAR(60) NOT NULL,
    ErrorMessage            VARCHAR(2000),
    RowCount                NUMERIC(10),
    ExecutionTimeMs         NUMERIC(10),

    -- Security metadata
    TablesAccessed          VARCHAR(2000),
    PermissionDeniedReason  VARCHAR(1000),

    CONSTRAINT AIG_QueryAudit_Key PRIMARY KEY (AIG_QueryAudit_ID),
    CONSTRAINT AIG_QueryAudit_UU_idx UNIQUE (AIG_QueryAudit_UU),
    CONSTRAINT AIG_QueryAudit_Client FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID),
    CONSTRAINT AIG_QueryAudit_User FOREIGN KEY (AD_User_ID)
        REFERENCES AD_User(AD_User_ID),
    CONSTRAINT AIG_QueryAudit_Role FOREIGN KEY (AD_Role_ID)
        REFERENCES AD_Role(AD_Role_ID),
    CONSTRAINT AIG_QueryAudit_Provider FOREIGN KEY (AIG_Provider_ID)
        REFERENCES AIG_Provider(AIG_Provider_ID)
);

-- Add indexes for performance
CREATE INDEX AIG_QueryAudit_User_Idx ON AIG_QueryAudit(AD_User_ID, Created);
CREATE INDEX AIG_QueryAudit_Provider_Idx ON AIG_QueryAudit(AIG_Provider_ID, Created);
CREATE INDEX AIG_QueryAudit_Status_Idx ON AIG_QueryAudit(Status, Created);

-- Add comments
COMMENT ON TABLE AIG_QueryAudit IS 'Audit log for AI-executed database queries';
COMMENT ON COLUMN AIG_QueryAudit.AIG_Provider_ID IS 'AI provider that executed the query';
COMMENT ON COLUMN AIG_QueryAudit.AD_User_ID IS 'User who initiated the AI request';
COMMENT ON COLUMN AIG_QueryAudit.AD_Role_ID IS 'Role used for permission checks';
COMMENT ON COLUMN AIG_QueryAudit.QuerySQL IS 'Original SQL query';
COMMENT ON COLUMN AIG_QueryAudit.SecuredSQL IS 'SQL after security injection via MRole.addAccessSQL()';
COMMENT ON COLUMN AIG_QueryAudit.Status IS 'SUCCESS, PERMISSION_DENIED, or ERROR';

COMMIT;