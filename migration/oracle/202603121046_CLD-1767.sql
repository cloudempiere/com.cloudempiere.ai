-- CLD-1767
SELECT register_migration_script('202603121046_CLD-1767.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Mar 12, 2026, 10:46:36 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (804052,0,'Prompt Configuration',800221,'AIG_Prompt_Config_ID',10,'N','N','N','N','N',0,'N',19,0,0,'Y',TO_TIMESTAMP('2026-03-12 10:46:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:46:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800707,'Y','N','MM02','N','N','N','Y','14aec148-3589-4d5b-a572-d878ff3162d3','Y',0,'N','N','N','N','N')
;

-- Mar 12, 2026, 10:47:21 AM CET
UPDATE AD_Column SET FKConstraintName='AIGPromptConfig_AIGProviderAcc', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-03-12 10:47:21','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=804052
;

-- Mar 12, 2026, 10:47:21 AM CET
ALTER TABLE AIG_Provider_Access ADD AIG_Prompt_Config_ID NUMBER(10) DEFAULT NULL 
;

-- Mar 12, 2026, 10:47:21 AM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT AIGPromptConfig_AIGProviderAcc FOREIGN KEY (AIG_Prompt_Config_ID) REFERENCES aig_prompt_config(aig_prompt_config_id) DEFERRABLE INITIALLY DEFERRED
;

-- Mar 12, 2026, 10:47:48 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803174,'Prompt Configuration',800235,804052,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:47:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:47:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','2fb1163c-12c5-414a-828d-3a82fc7d4aa8','Y',60,2)
;

-- Mar 12, 2026, 10:48:29 AM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', IsDisplayed='Y', SeqNo=40, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:48:29','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803146
;

-- Mar 12, 2026, 10:48:29 AM CET
UPDATE AD_Field SET Name='AI Provider', Description='Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)', Help=NULL, IsDisplayed='Y', SeqNo=50, XPosition=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:48:29','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803147
;

-- Mar 12, 2026, 10:48:29 AM CET
UPDATE AD_Field SET Name='Prompt Configuration', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:48:29','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803174
;

-- Mar 12, 2026, 10:48:29 AM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=70, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:48:29','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803148
;

-- Mar 12, 2026, 10:49:37 AM CET
UPDATE AD_Column SET IsMandatory='N',Updated=TO_TIMESTAMP('2026-03-12 10:49:37','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803887
;

-- Mar 12, 2026, 10:49:40 AM CET
ALTER TABLE AIG_Provider_Access MODIFY AIG_Provider_ID NUMBER(10) DEFAULT NULL 
;

-- Mar 12, 2026, 10:49:40 AM CET
ALTER TABLE AIG_Provider_Access MODIFY AIG_Provider_ID NULL
;

-- Mar 12, 2026, 10:51:07 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (804053,0,'Default','Default value','The Default Checkbox indicates if this record will be used as a default value.',800204,'IsDefault','N',1,'N','N','Y','N','N',0,'N',20,0,0,'Y',TO_TIMESTAMP('2026-03-12 10:51:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:51:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),1103,'Y','N','MM02','N','N','N','Y','f7fdfaec-223c-4e18-8de9-38fb1a468db9','N',0,'N','N','N','N')
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AIG_Prompt_Config_ID NUMBER(10)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AIGPromptKey VARCHAR2(40 CHAR)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AD_Client_ID NUMBER(10)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AD_Org_ID NUMBER(10)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AIG_Prompt_Config_UU VARCHAR2(36 CHAR) DEFAULT NULL 
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY Name VARCHAR2(60 CHAR)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY Description VARCHAR2(1024 CHAR) DEFAULT NULL 
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY AIGPromptText CLOB
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY Created DATE DEFAULT SYSDATE
;

-- Mar 12, 2026, 10:51:14 AM CET
UPDATE AIG_Prompt_Config SET Created=SYSDATE WHERE Created IS NULL
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY CreatedBy NUMBER(10)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY Updated DATE DEFAULT SYSDATE
;

-- Mar 12, 2026, 10:51:14 AM CET
UPDATE AIG_Prompt_Config SET Updated=SYSDATE WHERE Updated IS NULL
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY UpdatedBy NUMBER(10)
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config MODIFY IsActive CHAR(1) DEFAULT 'Y'
;

-- Mar 12, 2026, 10:51:14 AM CET
UPDATE AIG_Prompt_Config SET IsActive='Y' WHERE IsActive IS NULL
;

-- Mar 12, 2026, 10:51:14 AM CET
ALTER TABLE AIG_Prompt_Config ADD IsDefault CHAR(1) DEFAULT 'N' CHECK (IsDefault IN ('Y','N')) NOT NULL
;

-- Mar 12, 2026, 10:52:02 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803175,'Default','Default value','The Default Checkbox indicates if this record will be used as a default value.',800212,804053,'Y',1,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:52:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:52:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','306c17a9-c97a-4968-95a4-0bb51836925d','Y',70,2,2)
;

-- Mar 12, 2026, 10:52:57 AM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=60, XPosition=5, ColumnSpan=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:52:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802789
;

-- Mar 12, 2026, 10:52:57 AM CET
UPDATE AD_Field SET Name='Default', Description='Default value', Help='The Default Checkbox indicates if this record will be used as a default value.', IsDisplayed='Y', SeqNo=70, XPosition=6, ColumnSpan=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:52:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803175
;

-- Mar 12, 2026, 10:52:57 AM CET
UPDATE AD_Field SET Name='Prompt Text', Description=NULL, Help=NULL, SeqNo=80, NumLines=20, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:52:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802788
;

-- Mar 12, 2026, 10:52:57 AM CET
UPDATE AD_Field SET Name='AIG_Prompt_Config_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:52:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802786
;

-- Mar 12, 2026, 10:52:57 AM CET
UPDATE AD_Field SET Name='Prompt Configuration', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:52:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802785
;

-- Mar 12, 2026, 10:54:49 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800899,0,0,'Y',TO_TIMESTAMP('2026-03-12 10:54:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:54:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIGStatus','Status','Lifecycle status of the record.','Status','MM02','f6086dea-c1db-45fa-8e1d-fa4e1c0616e3')
;

-- Mar 12, 2026, 10:55:38 AM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800142,'AIGStatus','L',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:55:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:55:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','055783fc-580e-4425-9255-d5b3483b9a19','N')
;

-- Mar 12, 2026, 10:55:56 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800346,'Draft',800142,'D',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:55:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:55:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','124c50bb-5108-4d98-b459-f5a4c2c3ed0a')
;

-- Mar 12, 2026, 10:56:07 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800347,'Active',800142,'A',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:56:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:56:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','19b0eca3-528e-44c4-88f8-16a44bf9ca08')
;

-- Mar 12, 2026, 10:56:26 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800348,'Archived',800142,'X',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:56:26','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:56:26','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','02a74bb1-7e84-4eb7-a06b-8ee8db1a2163')
;

-- Mar 12, 2026, 10:56:45 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (804054,0,'Status','Lifecycle status of the record.',800204,'AIGStatus','D',1,'N','N','Y','N','N',0,'N',17,800142,0,0,'Y',TO_TIMESTAMP('2026-03-12 10:56:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:56:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800899,'Y','N','MM02','N','N','N','Y','dc837335-cc73-4635-a6e1-299968cbf928','N',0,'N','N','N','N')
;

-- Mar 12, 2026, 10:56:54 AM CET
ALTER TABLE AIG_Prompt_Config ADD AIGStatus CHAR(1) DEFAULT 'D' NOT NULL
;

-- Mar 12, 2026, 10:57:07 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803176,'Status','Lifecycle status of the record.',800212,804054,'Y',1,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-03-12 10:57:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-12 10:57:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5c6bacb1-d70f-4d0f-97b7-3c7ed74a5c9c','Y',80,2)
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Status', Description='Lifecycle status of the record.', Help=NULL, IsDisplayed='Y', SeqNo=30, XPosition=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803176
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Name', Description='Alphanumeric identifier of the entity', Help='The name of an entity (record) is used as an default search option in addition to the search key. The name is up to 60 characters in length.', SeqNo=40, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802783
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Description', Description='Optional short description of the record', Help='A description is limited to 255 characters.', SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802784
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Prompt Key', Description=NULL, Help=NULL, SeqNo=60, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802787
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802789
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Default', Description='Default value', Help='The Default Checkbox indicates if this record will be used as a default value.', SeqNo=80, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803175
;

-- Mar 12, 2026, 10:57:38 AM CET
UPDATE AD_Field SET Name='Prompt Text', Description=NULL, Help=NULL, SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-03-12 10:57:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802788
;

