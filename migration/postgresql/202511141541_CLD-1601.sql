-- CLD-1601
SELECT register_migration_script('202511141541_CLD-1601.sql') FROM dual;

-- Nov 14, 2025, 3:41:02 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800694,0,0,'Y',TO_TIMESTAMP('2025-11-14 15:40:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-14 15:40:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIGProviderType','Provider Type','The type of the AI Provider','Provider Type','MM02','fc9bf006-5b67-4342-945b-4323761875fb')
;

-- Nov 14, 2025, 3:46:28 PM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800124,'AIGProviderType','L',0,0,'Y',TO_TIMESTAMP('2025-11-14 15:46:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-14 15:46:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','c6de2507-0b7d-43a0-aeaa-97996083cc36','N')
;

-- Nov 14, 2025, 3:48:38 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800291,'Claude AI',800124,'CLA',0,0,'Y',TO_TIMESTAMP('2025-11-14 15:48:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-14 15:48:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','7856e8ee-3ff1-4316-8c43-8cf12c402fde')
;

-- Nov 14, 2025, 3:53:31 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803457,0,'Provider Type','The type of the AI Provider',800202,'AIGProviderType',3,'N','N','Y','N','N',0,'N',17,800124,0,0,'Y',TO_TIMESTAMP('2025-11-14 15:53:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-14 15:53:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800694,'Y','N','MM02','N','N','N','Y','70fe0603-b31d-4267-82a8-ee483ce5f00c','N',0,'N','N','N','N')
;

-- Nov 14, 2025, 3:53:38 PM CET
ALTER TABLE AIG_Provider ADD COLUMN AIGProviderType VARCHAR(3) NOT NULL
;

-- Nov 14, 2025, 4:12:27 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802761,'Provider Type','The type of the AI Provider',800210,803457,'Y',3,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-14 16:12:27','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-14 16:12:27','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','565473fa-cf5d-4add-8b47-62868cc23131','Y',50,2)
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='Provider Type', Description='The type of the AI Provider', Help=NULL, IsDisplayed='Y', SeqNo=30, XPosition=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802761
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802760
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='Name', Description='Alphanumeric identifier of the entity', Help='The name of an entity (record) is used as an default search option in addition to the search key. The name is up to 60 characters in length.', SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802756
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='API Key', Description=NULL, Help=NULL, SeqNo=60, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802759
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='AI Provider', Description='Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)', Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802757
;

-- Nov 14, 2025, 4:12:42 PM CET
UPDATE AD_Field SET Name='AIG_Provider_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-14 16:12:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802758
;

-- Nov 14, 2025, 4:18:12 PM CET
UPDATE AD_Ref_List SET Name='Anthropic Claude', Value='ANT',Updated=TO_TIMESTAMP('2025-11-14 16:18:12','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Ref_List_ID=800291
;

