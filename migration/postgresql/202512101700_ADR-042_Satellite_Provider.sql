-- CLD-1601 (ADR-042: Satellite AI Provider Integration)
-- Adds SAT provider type and Endpoint column for satellite service configuration
SELECT register_migration_script('202512101700_ADR-042_Satellite_Provider.sql') FROM dual;

-- Dec 10, 2025, 5:00:00 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,Help,PrintName,EntityType,AD_Element_UU) VALUES (800750,0,0,'Y',TO_TIMESTAMP('2025-12-10 17:00:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-10 17:00:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Endpoint','Endpoint','Service endpoint URL','The URL endpoint for the AI service. For satellite providers, this is the base URL of the Quarkus Satellite service (e.g., http://localhost:8090).','Endpoint','MM02','a1b2c3d4-e5f6-7890-abcd-ef1234567890')
;

-- Dec 10, 2025, 5:00:01 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803550,0,'Endpoint','Service endpoint URL','The URL endpoint for the AI service. For satellite providers, this is the base URL of the Quarkus Satellite service (e.g., http://localhost:8090).',800202,'Endpoint',255,'N','N','N','N','N',0,'N',10,0,0,'Y',TO_TIMESTAMP('2025-12-10 17:00:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-10 17:00:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800750,'Y','N','MM02','N','N','N','Y','b2c3d4e5-f6a7-8901-bcde-f23456789012','Y',0,'N','N','N','N')
;

-- Dec 10, 2025, 5:00:02 PM CET
ALTER TABLE AIG_Provider ADD COLUMN Endpoint VARCHAR(255)
;

-- Dec 10, 2025, 5:00:03 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,DisplayLogic) VALUES (802850,'Endpoint','Service endpoint URL','The URL endpoint for the AI service. For satellite providers, this is the base URL of the Quarkus Satellite service (e.g., http://localhost:8090).',800210,803550,'Y',255,65,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-10 17:00:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-10 17:00:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','c3d4e5f6-a7b8-9012-cdef-345678901234','Y',55,2,'@AIGProviderType@=SAT')
;

-- Dec 10, 2025, 5:00:04 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,Description,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800310,'Quarkus Satellite','AI requests routed through Quarkus Satellite Service (Java 17+, LangChain4j 1.x)',800124,'SAT',0,0,'Y',TO_TIMESTAMP('2025-12-10 17:00:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-10 17:00:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','d4e5f6a7-b8c9-0123-def4-567890123456')
;

-- Update X_AIG_Provider constant comment to include SAT
-- Note: The Java model class needs manual regeneration or update
