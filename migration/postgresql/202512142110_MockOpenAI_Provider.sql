-- Add MockOpenAI provider type for iDempiere-CLI Chat API integration
-- This provider connects to the OpenAI-compatible API endpoint (ADR-048)
-- Useful for testing, development, and integration without external API costs
SELECT register_migration_script('202512142110_MockOpenAI_Provider.sql') FROM dual;

-- Dec 14, 2025, 9:10:00 PM CET
-- Add MockOpenAI to AIGProviderType reference list
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
    'Mock OpenAI',
    'OpenAI-compatible API endpoint (local iDempiere-CLI or mock server)',
    800124,  -- AIGProviderType reference
    'MOA',   -- Mock OpenAI
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
