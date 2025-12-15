-- Add Mock AI Hub provider type for testing without real AI Hub deployment
-- This provider connects to OpenAI-compatible mock endpoint (port 8081)
-- Useful for testing, development, and CI/CD without AI Hub infrastructure
SELECT register_migration_script('202512142110_Mock_AIHub_Provider.sql') FROM dual;

-- Dec 14, 2025, 9:10:00 PM CET
-- Add Mock AI Hub to AIGProviderType reference list
INSERT INTO AD_Ref_List (
    AD_Ref_List_ID,
    Name,
    Description,
    AD_Reference_ID,
    Value,
    AD_Client_ID,
    AD_Org_ID,
    IsActive,
    Created,
    CreatedBy,
    Updated,
    UpdatedBy,
    EntityType,
    AD_Ref_List_UU
) VALUES (
    800301,
    'Mock AI Hub',
    'Mock AI Hub for testing (port 8081) - simulates iDempiere AI Hub without full deployment',
    800124,  -- AIGProviderType reference
    'MOA',   -- Mock AI Hub
    0,
    0,
    'Y',
    TO_TIMESTAMP('2025-12-14 21:10:00','YYYY-MM-DD HH24:MI:SS'),
    toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),
    TO_TIMESTAMP('2025-12-14 21:10:00','YYYY-MM-DD HH24:MI:SS'),
    toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),
    'MM02',
    'a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6'
)
;
