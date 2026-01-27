-- CLD-1628
SELECT register_migration_script('202512181034_CLD-1628.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Dec 18, 2025, 10:34:34 AM CET
UPDATE AD_Ref_List SET Name='iDempiere AI Hub',Updated=TO_TIMESTAMP('2025-12-18 10:34:34','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Ref_List_ID=800301
;

-- Dec 18, 2025, 10:36:24 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,Description,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800302,'Mock AI Hub','Mock AI Hub for testing (port 8081) - simulates iDempiere AI Hub without full deployment',800124,'MOA',0,0,'Y',TO_TIMESTAMP('2025-12-18 10:36:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-18 10:36:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','84d925f9-27cf-47be-b743-a8b0b12a7169')
;

-- Dec 18, 2025, 10:54:31 AM CET
UPDATE AD_Field SET Name='URL', Description='URL', Help='The URL defines an online address for this Business Partner.', DisplayLogic='@AIGProviderType@=SAT | @AIGProviderType@=MOA', SeqNo=100, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-18 10:54:31','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802865
;

