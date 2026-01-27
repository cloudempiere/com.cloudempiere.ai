-- CLD-1636: Chat Ownership and Sharing Model (ADR-036)
SELECT register_migration_script('202512041100_CLD-1636.sql') FROM dual;

-- ============================================================================
-- AIG_ChatOwnership Table - Manages chat ownership and sharing permissions
-- Per ADR-036: Hybrid approach using ConfidentialType + explicit ownership grants
-- ============================================================================

-- Create sequence for AIG_ChatOwnership
CREATE SEQUENCE AIG_ChatOwnership_Seq INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000;

-- Create the AIG_ChatOwnership table
CREATE TABLE AIG_ChatOwnership (
    AIG_ChatOwnership_ID    NUMERIC(10,0) NOT NULL,
    AIG_ChatOwnership_UU    VARCHAR(36) DEFAULT NULL,
    AD_Client_ID            NUMERIC(10,0) NOT NULL,
    AD_Org_ID               NUMERIC(10,0) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' NOT NULL CHECK (IsActive IN ('Y','N')),
    Created                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CreatedBy               NUMERIC(10,0) NOT NULL,
    Updated                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedBy               NUMERIC(10,0) NOT NULL,
    -- Core ownership fields
    CM_Chat_ID              NUMERIC(10,0) NOT NULL,
    AD_User_ID              NUMERIC(10,0) DEFAULT NULL,
    AD_Role_ID              NUMERIC(10,0) DEFAULT NULL,
    -- OwnershipType: O=Owner, R=Read, W=Write
    OwnershipType           CHAR(1) NOT NULL DEFAULT 'R' CHECK (OwnershipType IN ('O','R','W')),
    SharedBy_User_ID        NUMERIC(10,0) DEFAULT NULL,
    ValidFrom               TIMESTAMP DEFAULT NULL,
    ValidTo                 TIMESTAMP DEFAULT NULL,
    -- Constraints
    CONSTRAINT AIG_ChatOwnership_Key PRIMARY KEY (AIG_ChatOwnership_ID),
    CONSTRAINT AIG_ChatOwnership_UU_idx UNIQUE (AIG_ChatOwnership_UU)
);

-- Add foreign key constraints
ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT ADClient_AIG_ChatOwnership
    FOREIGN KEY (AD_Client_ID) REFERENCES AD_Client(AD_Client_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT ADOrg_AIG_ChatOwnership
    FOREIGN KEY (AD_Org_ID) REFERENCES AD_Org(AD_Org_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT CreatedBy_AIG_ChatOwnership
    FOREIGN KEY (CreatedBy) REFERENCES AD_User(AD_User_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT UpdatedBy_AIG_ChatOwnership
    FOREIGN KEY (UpdatedBy) REFERENCES AD_User(AD_User_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT CMChat_AIG_ChatOwnership
    FOREIGN KEY (CM_Chat_ID) REFERENCES CM_Chat(CM_Chat_ID) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT ADUser_AIG_ChatOwnership
    FOREIGN KEY (AD_User_ID) REFERENCES AD_User(AD_User_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT ADRole_AIG_ChatOwnership
    FOREIGN KEY (AD_Role_ID) REFERENCES AD_Role(AD_Role_ID) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE AIG_ChatOwnership ADD CONSTRAINT SharedBy_AIG_ChatOwnership
    FOREIGN KEY (SharedBy_User_ID) REFERENCES AD_User(AD_User_ID) DEFERRABLE INITIALLY DEFERRED;

-- Create indexes for efficient queries
CREATE INDEX AIG_ChatOwnership_Chat_IDX ON AIG_ChatOwnership(CM_Chat_ID);
CREATE INDEX AIG_ChatOwnership_User_IDX ON AIG_ChatOwnership(AD_User_ID);
CREATE INDEX AIG_ChatOwnership_Role_IDX ON AIG_ChatOwnership(AD_Role_ID);
CREATE INDEX AIG_ChatOwnership_Client_IDX ON AIG_ChatOwnership(AD_Client_ID);

-- Add comment
COMMENT ON TABLE AIG_ChatOwnership IS 'AI Chat Ownership and Sharing - ADR-036. Defines explicit ownership grants for AI chat conversations.';
COMMENT ON COLUMN AIG_ChatOwnership.OwnershipType IS 'O=Owner (full control), R=Read (view only), W=Write (view and add messages)';
COMMENT ON COLUMN AIG_ChatOwnership.SharedBy_User_ID IS 'User who granted this access (audit trail)';
COMMENT ON COLUMN AIG_ChatOwnership.ValidFrom IS 'Optional: Access valid from this timestamp';
COMMENT ON COLUMN AIG_ChatOwnership.ValidTo IS 'Optional: Access valid until this timestamp (time-bound sharing)';


