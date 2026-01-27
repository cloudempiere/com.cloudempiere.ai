-- CLD-1601
SELECT register_migration_script('202511171548_CLD-1601.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Nov 17, 2025, 3:48:08 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800292,'AWS Bedrock',800124,'ABE',0,0,'Y',TO_TIMESTAMP('2025-11-17 15:48:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-17 15:48:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','d5dd3144-b3a3-41e4-80bd-252abc5d30e4')
;

