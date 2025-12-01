-- Migration script for AI Database Access feature (Oracle)
-- Ticket: CLD-1601
-- Purpose: Add database access infrastructure for AI agents
-- Author: Cloudempiere
-- Date: 2025-11-18

-- =====================================================================
-- 1. Add AD_User_ID to AIG_Provider table
-- =====================================================================

-- Add column
ALTER TABLE AIG_Provider ADD AD_User_ID NUMBER(10);

-- Add foreign key constraint
ALTER TABLE AIG_Provider
  ADD CONSTRAINT ADUser_AIGProvider
  FOREIGN KEY (AD_User_ID)
  REFERENCES AD_User(AD_User_ID);

-- Add comment
COMMENT ON COLUMN AIG_Provider.AD_User_ID IS 'System user representing this AI provider for audit trail purposes';

-- =====================================================================
-- 2. Create AIG_QueryAudit table
-- =====================================================================

CREATE TABLE AIG_QueryAudit (
    AIG_QueryAudit_ID       NUMBER(10) NOT NULL,
    AD_Client_ID            NUMBER(10) NOT NULL,
    AD_Org_ID               NUMBER(10) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')),
    Created                 DATE DEFAULT SYSDATE NOT NULL,
    CreatedBy               NUMBER(10) NOT NULL,
    Updated                 DATE DEFAULT SYSDATE NOT NULL,
    UpdatedBy               NUMBER(10) NOT NULL,
    AIG_QueryAudit_UU       VARCHAR2(36) DEFAULT SYS_GUID(),

    -- AI Provider reference
    AIG_Provider_ID         NUMBER(10) NOT NULL,

    -- User context (the logged-in user)
    AD_User_ID              NUMBER(10) NOT NULL,
    AD_Role_ID              NUMBER(10) NOT NULL,

    -- Query details
    QuerySQL                CLOB NOT NULL,
    SecuredSQL              CLOB,
    QueryPurpose            VARCHAR2(255),
    ContextType             VARCHAR2(60),

    -- Execution results
    Status                  VARCHAR2(60) NOT NULL,
    ErrorMessage            VARCHAR2(2000),
    RowCount                NUMBER(10),
    ExecutionTimeMs         NUMBER(10),

    -- Security metadata
    TablesAccessed          VARCHAR2(2000),
    PermissionDeniedReason  VARCHAR2(1000),

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
