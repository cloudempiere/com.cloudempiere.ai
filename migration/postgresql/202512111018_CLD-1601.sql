-- CLD-1601
SELECT register_migration_script('202512111018_CLD-1601.sql') FROM dual;

-- Dec 11, 2025, 10:18:45 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800301,'Quarkus Satellite',800124,'SAT',0,0,'Y',TO_TIMESTAMP('2025-12-11 10:18:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 10:18:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','79110fc1-1c18-4d93-b977-7b21862e7d13')
;

-- Dec 11, 2025, 10:24:40 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803559,0,'URL','URL','The URL defines an online address for this Business Partner.',800202,'URL',120,'N','N','N','N','N',0,'N',40,0,0,'Y',TO_TIMESTAMP('2025-12-11 10:24:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 10:24:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),983,'Y','N','MM02','N','N','N','Y','f1afbdf3-d39e-436c-93d9-293c57a13da6','Y',0,'N','N','N','N','N')
;

-- Dec 11, 2025, 10:24:45 AM CET
ALTER TABLE AIG_Provider ADD COLUMN URL VARCHAR(120) DEFAULT NULL 
;

-- Dec 11, 2025, 10:25:28 AM CET
UPDATE AD_Column SET MandatoryLogic='@AIGProviderType@=SAT',Updated=TO_TIMESTAMP('2025-12-11 10:25:28','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803559
;

-- Dec 11, 2025, 10:25:31 AM CET
INSERT INTO t_alter_column values('aig_provider','URL','VARCHAR(120)',null,'NULL')
;

-- Dec 11, 2025, 10:26:08 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802865,'URL','URL','The URL defines an online address for this Business Partner.',800210,803559,'Y',120,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-11 10:26:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 10:26:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','c39c1359-f331-4c88-840f-d36e339073cb','Y',90,5)
;

-- Dec 11, 2025, 10:26:34 AM CET
UPDATE AD_Field SET Name='URL', Description='URL', Help='The URL defines an online address for this Business Partner.', DisplayLogic='@AIGProviderType@=SAT', SeqNo=100, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-11 10:26:34','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802865
;

