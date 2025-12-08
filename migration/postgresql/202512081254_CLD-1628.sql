-- CLD-1628
SELECT register_migration_script('202512081254_CLD-1628.sql') FROM dual;

-- Dec 8, 2025, 12:54:16 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,Description,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800299,'AWS Bedrock','Ollama',800124,'OLL',0,0,'Y',TO_TIMESTAMP('2025-12-08 12:54:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-08 12:54:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','fb695c4a-1354-4714-976e-b878526ff9d2')
;

-- Dec 8, 2025, 12:54:24 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,Description,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800300,'Ollama',NULL,800124,'OLL',0,0,'Y',TO_TIMESTAMP('2025-12-08 12:54:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-08 12:54:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','489a5164-ed32-4515-866c-53fe8e9e18e6')
;

