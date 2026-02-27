-- CLD-1756
SELECT register_migration_script('202602271201_CLD-1756.sql') FROM dual;

-- Feb 27, 2026, 12:01:10 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800824,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:01:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:01:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIAccessLevel','AI Access Level','AI Access Level','MM02','59887fc0-a22e-40fb-8732-389ff5852fa0')
;

-- Feb 27, 2026, 12:02:20 PM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800136,'AIAccessLevel','L',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:02:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:02:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','c2d34d58-49a7-439e-909f-0f4f4b47c2af','N')
;

-- Feb 27, 2026, 12:02:33 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800330,'All',800136,'A',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:02:33','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:02:33','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','6c328462-cfb3-4fa6-a43a-0c737705bac6')
;

-- Feb 27, 2026, 12:03:05 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800331,'User/Role Access',800136,'R',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:03:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:03:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','7356a347-f6f4-41cb-83be-dfd3b8289f74')
;

-- Feb 27, 2026, 12:03:13 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800332,'None',800136,'N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:03:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:03:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','0c41e933-dd04-4d3a-b975-715076880538')
;

-- Feb 27, 2026, 12:03:55 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803875,0,'AI Access Level',156,'AIAccessLevel','N',1,'N','N','Y','N','N',0,'N',17,800136,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:03:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:03:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800824,'Y','N','MM02','N','N','N','Y','8394e3fc-5c44-4b45-ac92-d11df8a91a3e','Y',0,'N','N','N','N')
;

-- Feb 27, 2026, 12:04:00 PM CET
ALTER TABLE AD_Role ADD COLUMN AIAccessLevel CHAR(1) DEFAULT 'N' NOT NULL
;

-- Feb 27, 2026, 12:04:14 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803042,'AI Access Level',119,803875,'Y',1,540,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:04:14','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:04:14','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','db5d6e4f-bce2-4b27-9174-6ef5c3cef39a','Y',490,2)
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='AI Access Level', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=380, XPosition=3, ColumnSpan=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803042
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Payment', Description=NULL, Help=NULL, SeqNo=390, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50175
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Schedule', Description=NULL, Help=NULL, SeqNo=400, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50178
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Account', Description=NULL, Help=NULL, SeqNo=410, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50168
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Resource', Description=NULL, Help=NULL, SeqNo=420, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50177
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Asset', Description=NULL, Help=NULL, SeqNo=430, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50169
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info InOut', Description=NULL, Help=NULL, SeqNo=440, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50172
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Invoice', Description=NULL, Help=NULL, SeqNo=450, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50173
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Product', Description=NULL, Help=NULL, SeqNo=460, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50176
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info BPartner', Description=NULL, Help=NULL, SeqNo=470, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50170
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Info Order', Description=NULL, Help=NULL, SeqNo=480, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=50174
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Is Discount Up to Limit Price', Description=NULL, Help=NULL, SeqNo=490, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='b057cea2-179a-42a0-8f66-45927f5542a2'
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Change Limit Price', Description='Allow Change Limit Price on Documents', Help=NULL, SeqNo=500, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='75808dd2-2cb9-4413-a92d-5d332faafbcc'
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Allow Define PO List Price', Description='When selected, then user can define specific PO ListPrice on Purchase Product - List Price can be different from PO Price field', Help=NULL, SeqNo=510, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='3a13600a-85e4-470c-90a5-3a6f1c821850'
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Is Discount Allowed On Total', Description=NULL, Help=NULL, SeqNo=520, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='f4f44935-969d-4fd0-be4d-ca2f5b3cba49'
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Order Tax Validation', Description='Enables tax consistency check during order processing', Help='If the selected tax does not match the expected tax derived from the product tax category and business partner tax group, the system may block or warn upon completion, depending on role permissions.', SeqNo=530, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='fe06807c-c86f-4890-b986-286e5b24d43e'
;

-- Feb 27, 2026, 12:05:10 PM CET
UPDATE AD_Field SET Name='Invoice Tax Validation', Description='Enables tax consistency check during invoice processing', Help='If the selected tax does not match the expected tax derived from the product tax category and business partner tax group, the system may block or warn upon completion, depending on role permissions.', SeqNo=540, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:05:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='39cc7783-4646-4087-a3de-11439fd5aa49'
;

-- Feb 27, 2026, 12:05:53 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800221,'AI Provider Access','AIG_Provider_Access',0,'6',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:05:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:05:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','a418db4d-1f88-430c-888e-970a0ab74531','N','N','N','N','N','N','N')
;

-- Feb 27, 2026, 12:05:53 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_Provider_Access',1000000,'N','N','Table AIG_Provider_Access','Y','Y',0,0,TO_TIMESTAMP('2026-02-27 12:05:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:05:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800625,'Y',1000000,1,200000,'98d7a2f9-c54e-480d-ba44-5205de474612')
;

-- Feb 27, 2026, 12:05:53 PM CET
CREATE SEQUENCE AIG_PROVIDER_ACCESS_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Feb 27, 2026, 12:09:03 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,ReadOnlyLogic,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803876,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800221,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','1=1','MM02','N','101ff774-3f28-41be-8d06-3583ea55cc30','N')
;

-- Feb 27, 2026, 12:09:03 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803877,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800221,'AD_Org_ID','@AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','99129d97-632f-4554-a8d1-1385a12973b1','N')
;

-- Feb 27, 2026, 12:09:03 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803878,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800221,'Created',7,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','4d425c69-12b6-4f4f-96d1-7e554d0702d6','N')
;

-- Feb 27, 2026, 12:09:03 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803879,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800221,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','39717ca4-9011-4af2-8767-c1abc76f3060','N')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803880,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800221,'Updated',7,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','33109252-c767-442a-8a21-e5a709c21aa4','N')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803881,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800221,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','85f56b2a-9acf-4e4c-8e81-72c84d515d41','N')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803882,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800221,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','cc8014d6-46e2-4e7e-bbc8-319c50010b64','N')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800825,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Provider_Access_ID','AI Provider Access',NULL,'AI Provider Access','MM02','f9892b59-cc20-4ad7-b14a-03545b2200e4')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803883,0.0,'AI Provider Access',800221,'AIG_Provider_Access_ID',22,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800825,'N','N','MM02','N','04e23130-665b-46c1-8e40-52f746572ed8','N')
;

-- Feb 27, 2026, 12:09:04 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800826,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Provider_Access_UU','AIG_Provider_Access_UU','AIG_Provider_Access_UU','MM02','c7e1fd8c-8695-4b02-abe5-112a53a2c480')
;

-- Feb 27, 2026, 12:09:05 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803884,0.0,'AIG_Provider_Access_UU',800221,'AIG_Provider_Access_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800826,'Y','N','MM02','N','88167275-6509-497c-847b-f5067756b9de','N')
;

-- Feb 27, 2026, 12:09:05 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803885,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800221,'AD_User_ID',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:09:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:09:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'Y','N','MM02','N','5ece39c4-5b9e-4e20-8173-814f8a8bfac7','N')
;

-- Feb 27, 2026, 12:11:24 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803886,0,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800221,158,'AD_Role_ID','@AD_Role_ID@',10,'N','Y','N','N','N',0,'N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:11:24','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:11:24','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),123,'N','N','MM02','N','N','N','Y','a6fac04c-287b-4298-8e5d-ff44b9fa0ffa','Y',0,'N','N','C','N','N')
;

-- Feb 27, 2026, 12:12:06 PM CET
UPDATE AD_Column SET AD_Val_Rule_ID=toRecordId('AD_Val_Rule','d3b3ed10-096e-4310-b6ef-23e5dfb5c279'), IsMandatory='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803885
;

-- Feb 27, 2026, 12:12:35 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803887,0,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800221,'AIG_Provider_ID',22,'N','N','Y','N','N',0,'N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800692,'Y','N','MM02','N','N','N','Y','8e605c29-65af-4397-bb85-2540fc7e9ea0','Y',0,'N','N','N','N','N')
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='ADClient_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803876
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='ADOrg_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803877
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET IsUpdateable='N', FKConstraintName='ADRole_AIGProviderAccess', FKConstraintType='C',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803886
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET FKConstraintName='ADUser_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803885
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET FKConstraintName='AIGProvider_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803887
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='CreatedBy_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803879
;

-- Feb 27, 2026, 12:12:38 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='UpdatedBy_AIGProviderAccess', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 12:12:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803881
;

-- Feb 27, 2026, 12:12:38 PM CET
CREATE TABLE AIG_Provider_Access (AD_Client_ID NUMERIC(10) NOT NULL, AD_Org_ID NUMERIC(10) NOT NULL, AD_Role_ID NUMERIC(10) DEFAULT NULL , AD_User_ID NUMERIC(10) DEFAULT NULL , AIG_Provider_Access_ID NUMERIC(10) NOT NULL, AIG_Provider_Access_UU VARCHAR(36) DEFAULT NULL , AIG_Provider_ID NUMERIC(10) NOT NULL, Created TIMESTAMP NOT NULL, CreatedBy NUMERIC(10) NOT NULL, IsActive CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')) NOT NULL, Updated TIMESTAMP NOT NULL, UpdatedBy NUMERIC(10) NOT NULL, CONSTRAINT AIG_Provider_Access_Key PRIMARY KEY (AIG_Provider_Access_ID), CONSTRAINT AIG_Provider_Access_UU_idx UNIQUE (AIG_Provider_Access_UU))
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT ADClient_AIGProviderAccess FOREIGN KEY (AD_Client_ID) REFERENCES ad_client(ad_client_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT ADOrg_AIGProviderAccess FOREIGN KEY (AD_Org_ID) REFERENCES ad_org(ad_org_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT ADRole_AIGProviderAccess FOREIGN KEY (AD_Role_ID) REFERENCES ad_role(ad_role_id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT ADUser_AIGProviderAccess FOREIGN KEY (AD_User_ID) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT AIGProvider_AIGProviderAccess FOREIGN KEY (AIG_Provider_ID) REFERENCES aig_provider(aig_provider_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT CreatedBy_AIGProviderAccess FOREIGN KEY (CreatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:38 PM CET
ALTER TABLE AIG_Provider_Access ADD CONSTRAINT UpdatedBy_AIGProviderAccess FOREIGN KEY (UpdatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800227,'AI Provider Access',111,160,'Y',800221,0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_Provider_Access.Created DESC','N',1,'N','D','Y','N','0f8c9b9a-e01d-48fc-a5fb-878e98a5c6c0')
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803043,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800227,803876,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','97d5cecd-db5e-4e2b-acd0-709f3ad750f4','Y',10,2)
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (803044,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800227,803877,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','4a448406-5efc-454b-b804-0bfe7e556f4b','Y','N',4,2)
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803045,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800227,803886,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5a215631-6fdd-47ef-bdc3-48203543092e','Y',20,2)
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803046,'AI Provider Access',800227,803883,'N',22,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a25a7a54-91d9-4f3e-b6b4-4b4605fe4553','N',2)
;

-- Feb 27, 2026, 12:12:53 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803047,'AIG_Provider_Access_UU',800227,803884,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','4dc35626-338a-46e7-affa-92385634b320','N',2)
;

-- Feb 27, 2026, 12:12:54 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803048,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800227,803885,'Y',10,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','50433b1b-9d8e-432a-bb7b-1232c8b75a7e','Y',30,2)
;

-- Feb 27, 2026, 12:12:54 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803049,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800227,803887,'Y',22,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','9974dd2f-eddc-4c1e-b776-688f821c9c82','Y',40,2)
;

-- Feb 27, 2026, 12:12:54 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803050,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800227,803882,'Y',1,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 12:12:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 12:12:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','13e732cf-e27e-4472-8096-e0841fb7227e','Y',50,2,2)
;

-- Feb 27, 2026, 12:12:54 PM CET
UPDATE AD_Table SET AD_Window_ID=111,Updated=TO_TIMESTAMP('2026-02-27 12:12:54','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800221
;

-- Feb 27, 2026, 12:13:27 PM CET
UPDATE AD_Field SET Name='AI Provider', Description='Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)', Help=NULL, IsDisplayed='Y', SeqNo=40, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803049
;

-- Feb 27, 2026, 12:13:27 PM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803048
;

-- Feb 27, 2026, 12:13:27 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=60, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803050
;

-- Feb 27, 2026, 12:13:27 PM CET
UPDATE AD_Field SET Name='AI Provider Access', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803046
;

-- Feb 27, 2026, 12:13:27 PM CET
UPDATE AD_Field SET Name='AIG_Provider_Access_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803047
;

-- Feb 27, 2026, 12:13:43 PM CET
UPDATE AD_Field SET Name='Role', Description='Responsibility Role', Help='The Role determines security and access a user who has this Role will have in the System.', SeqNo=30, IsReadOnly='Y', Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 12:13:43','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803045
;

