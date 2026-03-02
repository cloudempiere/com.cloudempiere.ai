-- CLD-1757
SELECT register_migration_script('202602271416_CLD-1757.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Feb 27, 2026, 2:16:20 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800227,'AI Model Pricing','AIG_ModelPricing',0,'6',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:16:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:16:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','571710e8-ceba-4142-be5d-1077cbd36907','N','N','N','N','N','N','N')
;

-- Feb 27, 2026, 2:16:20 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_ModelPricing',1000000,'N','N','Table AIG_ModelPricing','Y','Y',0,0,TO_TIMESTAMP('2026-02-27 14:16:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:16:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800631,'Y',1000000,1,200000,'08f22ef2-8402-4b28-a7df-d1cdb43df7fc')
;

-- Feb 27, 2026, 2:16:20 PM CET
CREATE SEQUENCE AIG_MODELPRICING_SQ INCREMENT BY 1 MINVALUE 1000000 MAXVALUE 2147483647 START WITH 1000000
;

-- Feb 27, 2026, 2:17:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,ReadOnlyLogic,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803978,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800227,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','1=1','MM02','N','4826b1ee-37c9-41e5-9d37-f348c4da4b91','N')
;

-- Feb 27, 2026, 2:17:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803979,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800227,'AD_Org_ID','@AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','d34f31de-a4cb-45b6-8875-b6093d57b4a9','N')
;

-- Feb 27, 2026, 2:17:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803980,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800227,'Created',7,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','13846f88-af9e-442f-9da1-c8bb71393498','N')
;

-- Feb 27, 2026, 2:17:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803981,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800227,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','210789a4-fdca-4520-b6bc-63b17dc064bd','N')
;

-- Feb 27, 2026, 2:17:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803982,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800227,'Updated',7,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','e141e743-e726-4fcb-8007-ebe46284b940','N')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803983,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800227,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','8bb74113-ac7c-42d7-ac92-a20c8bf3d344','N')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803984,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800227,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','57026d72-ade5-4d29-aaba-0486dd189aca','N')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800861,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_ModelPricing_ID','AI Model Pricing',NULL,'AI Model Pricing','MM02','9b2fe47c-57d2-4f07-b522-da989716dde2')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803985,0.0,'AI Model Pricing',800227,'AIG_ModelPricing_ID',22,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800861,'N','N','MM02','N','80b4cc66-7117-4903-b946-a0ca28472ece','N')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800862,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_ModelPricing_UU','AIG_ModelPricing_UU','AIG_ModelPricing_UU','MM02','9c12eccc-fb2c-46c3-89a7-542135515c96')
;

-- Feb 27, 2026, 2:17:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803986,0.0,'AIG_ModelPricing_UU',800227,'AIG_ModelPricing_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800862,'Y','N','MM02','N','ab956a7d-17ca-450b-a8cf-1722ae90f3e4','N')
;

-- Feb 27, 2026, 2:18:20 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803987,0,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800227,'AIG_Provider_ID','@AIG_Provider_ID@',10,'N','Y','N','N','N',0,'N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:18:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:18:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800692,'N','N','MM02','N','N','N','N','baf3a0b0-a83c-41e7-8f76-2b4d070b3afc','N',0,'N','N','N','N','N')
;

-- Feb 27, 2026, 2:20:02 PM CET
UPDATE AD_Column SET IsUpdateable='N', IsAllowLogging='Y',Updated=TO_TIMESTAMP('2026-02-27 14:20:02','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803987
;

-- Feb 27, 2026, 2:20:46 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803988,0,'Model Name',800227,'ModelName',100,'N','N','N','N','N',0,'N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:20:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:20:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800716,'Y','Y','MM02','N','N','N','Y','7dc255c4-718d-4c42-ab30-c28929e8d0b9','Y',10,'N','N','N','N','N')
;

-- Feb 27, 2026, 2:21:38 PM CET
UPDATE AD_Column SET Description='Substring match against model name',Updated=TO_TIMESTAMP('2026-02-27 14:21:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803988
;

-- Feb 27, 2026, 2:22:18 PM CET
UPDATE AD_Column SET Description='AI Provider - if empty, the pricing applies to any provider.', IsUpdateable='N',Updated=TO_TIMESTAMP('2026-02-27 14:22:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803987
;

-- Feb 27, 2026, 2:23:34 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800863,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:23:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:23:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'InputCostPerMToken','Input Cost / Million Token','Provider cost per million input tokens','Input Cost / Million Token','MM02','0efab4eb-ce9a-4dd8-8006-41468f8da9b0')
;

-- Feb 27, 2026, 2:25:00 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803989,0,'Input Cost / Million Token','Provider cost per million input tokens',800227,'InputCostPerMToken','0',63,'N','N','N','N','N',0,'N',12,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:25:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:25:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800863,'Y','N','MM02','N','N','N','Y','1113d445-fd50-4134-ba31-7a9bc2050428','N',0,'N','N','N','N')
;

-- Feb 27, 2026, 2:25:48 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,PrintName,EntityType,AD_Element_UU) VALUES (800864,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:25:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:25:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'OutputCostPerMToken','Output Cost / Million Token','Provider cost per million output tokens','Output Cost / Million Token','MM02','3823a9b2-8dac-4762-8252-b49829a1e9bd')
;

-- Feb 27, 2026, 2:26:12 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803990,0,'Output Cost / Million Token','Provider cost per million output tokens',800227,'OutputCostPerMToken','0',63,'N','N','N','N','N',0,'N',12,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:26:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:26:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800864,'Y','N','MM02','N','N','N','Y','7e4de783-6278-47e3-acab-c7f3230fbd23','N',0,'N','N','N','N')
;

-- Feb 27, 2026, 2:27:28 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803991,0,'Currency','The Currency for this record','Indicates the Currency to be used when processing or reporting on this record',800227,'C_Currency_ID',10,'N','N','Y','N','N',0,'N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:27:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:27:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),193,'Y','N','MM02','N','N','N','Y','0cdb30ff-b368-4ce5-88f0-ea7e4dac99e3','Y',0,'N','N','N','N','N')
;

-- Feb 27, 2026, 2:27:54 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803992,0,'Valid from','Valid from including this date (first day)','The Valid From date indicates the first day of a date range',800227,'ValidFrom','@#Date@',29,'N','N','Y','N','N',0,'N',15,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),617,'Y','N','MM02','N','N','N','Y','c561cf1c-e19c-440e-aa2e-863ee3623cc3','Y',0,'N','N','N','N','N')
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='ADClient_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803978
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='ADOrg_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803979
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET IsUpdateable='N', FKConstraintName='AIGProvider_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803987
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET FKConstraintName='CCurrency_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803991
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='CreatedBy_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803981
;

-- Feb 27, 2026, 2:28:09 PM CET
UPDATE AD_Column SET IsAllowCopy='N', FKConstraintName='UpdatedBy_AIGModelPricing', FKConstraintType='N',Updated=TO_TIMESTAMP('2026-02-27 14:28:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803983
;

-- Feb 27, 2026, 2:28:09 PM CET
CREATE TABLE AIG_ModelPricing (AD_Client_ID NUMBER(10) NOT NULL, AD_Org_ID NUMBER(10) NOT NULL, AIG_ModelPricing_ID NUMBER(10) NOT NULL, AIG_ModelPricing_UU VARCHAR2(36 CHAR) DEFAULT NULL , AIG_Provider_ID NUMBER(10) DEFAULT NULL , C_Currency_ID NUMBER(10) NOT NULL, Created DATE NOT NULL, CreatedBy NUMBER(10) NOT NULL, InputCostPerMToken NUMBER DEFAULT 0, IsActive CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')) NOT NULL, ModelName VARCHAR2(100 CHAR) DEFAULT NULL , OutputCostPerMToken NUMBER DEFAULT 0, Updated DATE NOT NULL, UpdatedBy NUMBER(10) NOT NULL, ValidFrom DATE NOT NULL, CONSTRAINT AIG_ModelPricing_Key PRIMARY KEY (AIG_ModelPricing_ID), CONSTRAINT AIG_ModelPricing_UU_idx UNIQUE (AIG_ModelPricing_UU))
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT ADClient_AIGModelPricing FOREIGN KEY (AD_Client_ID) REFERENCES ad_client(ad_client_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT ADOrg_AIGModelPricing FOREIGN KEY (AD_Org_ID) REFERENCES ad_org(ad_org_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT AIGProvider_AIGModelPricing FOREIGN KEY (AIG_Provider_ID) REFERENCES aig_provider(aig_provider_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT CCurrency_AIGModelPricing FOREIGN KEY (C_Currency_ID) REFERENCES c_currency(c_currency_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT CreatedBy_AIGModelPricing FOREIGN KEY (CreatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:09 PM CET
ALTER TABLE AIG_ModelPricing ADD CONSTRAINT UpdatedBy_AIGModelPricing FOREIGN KEY (UpdatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED
;

-- Feb 27, 2026, 2:28:55 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800085,'AI Model Pricing',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','6ebd6d9a-de91-46d7-b2f3-04424e3d0fde')
;

-- Feb 27, 2026, 2:28:56 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800233,'AI Model Pricing',800085,10,'Y',800227,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_ModelPricing.Created DESC','N',0,'N','MM02','Y','N','c7568bcd-36c7-433e-bc57-cb3a1a34ab94')
;

-- Feb 27, 2026, 2:28:56 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803118,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800233,803978,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','9ee5479d-22ed-40b2-8d99-bbcbb0778336','Y',10,2)
;

-- Feb 27, 2026, 2:28:56 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (803119,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800233,803979,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','6f671c79-db37-4d98-8bb3-72ded6516241','Y','N',4,2)
;

-- Feb 27, 2026, 2:28:56 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803120,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800233,803987,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','843b0ae9-e52a-4e8e-b125-47e5ceef0f2d','Y',20,2)
;

-- Feb 27, 2026, 2:28:56 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803121,'AI Model Pricing',800233,803985,'N',22,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','aa6fdbaa-5b26-4519-a492-426e5c963574','N',2)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803122,'AIG_ModelPricing_UU',800233,803986,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7903dfe0-64b3-462d-a07f-aa937c97cc9b','N',2)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803123,'Model Name',800233,803988,'Y',100,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','4a73b563-6363-4b4b-ab5a-0d0469559724','Y',30,5)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803124,'Input Cost / Million Token','Provider cost per million input tokens',800233,803989,'Y',63,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','42e27591-5147-42f4-b65b-f2cbcf561988','Y',40,5)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803125,'Output Cost / Million Token','Provider cost per million output tokens',800233,803990,'Y',63,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5a48747e-c543-4b7b-92a1-31d19f4ca0f2','Y',50,5)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803126,'Currency','The Currency for this record','Indicates the Currency to be used when processing or reporting on this record',800233,803991,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','31405558-034b-4bc7-b0fc-0e5df2122d9a','Y',60,2)
;

-- Feb 27, 2026, 2:28:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803127,'Valid from','Valid from including this date (first day)','The Valid From date indicates the first day of a date range',800233,803992,'Y',29,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','bce9b007-dd7d-471c-86c0-4fd249cd23bc','Y',70,2)
;

-- Feb 27, 2026, 2:28:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803128,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800233,803984,'Y',1,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','57c235ba-8b71-4023-825d-2621b28be25f','Y',80,2,2)
;

-- Feb 27, 2026, 2:28:58 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,Action,AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800147,'AI Model Pricing','W',800085,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:28:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:28:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','afc5044d-84a3-43f7-b995-3f681b65fbc1')
;

-- Feb 27, 2026, 2:28:58 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', getDate(), 1134855, getDate(), 1134855,t.AD_Tree_ID, 800147, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800147)
;

-- Feb 27, 2026, 2:28:58 PM CET
UPDATE AD_Table SET AD_Window_ID=800085,Updated=TO_TIMESTAMP('2026-02-27 14:28:58','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800227
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803128
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Model Name', Description=NULL, Help=NULL, SeqNo=50, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803123
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Valid from', Description='Valid from including this date (first day)', Help='The Valid From date indicates the first day of a date range', IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803127
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Input Cost / Million Token', Description='Provider cost per million input tokens', Help=NULL, SeqNo=70, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803124
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Currency', Description='The Currency for this record', Help='Indicates the Currency to be used when processing or reporting on this record', IsDisplayed='Y', SeqNo=80, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803126
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='Output Cost / Million Token', Description='Provider cost per million output tokens', Help=NULL, SeqNo=90, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803125
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='AI Model Pricing', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803121
;

-- Feb 27, 2026, 2:29:36 PM CET
UPDATE AD_Field SET Name='AIG_ModelPricing_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:29:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803122
;

-- Feb 27, 2026, 2:29:58 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800234,'AI Model Pricing',800073,30,'Y',800227,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_ModelPricing.Created DESC','N',1,'N','MM02','Y','N','77538801-5dd3-4847-ba78-3065b37770d0')
;

-- Feb 27, 2026, 2:29:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803129,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800234,803978,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','5be4a15c-7a3a-487a-8421-647d76d635e4','Y',10,2)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (803130,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800234,803979,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a176bfa1-51d2-4b5c-81e0-6a002da10a25','Y','N',4,2)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803131,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800234,803987,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','4e71f793-e7e1-45b5-bab7-2d34ee3f2d4a','Y',20,2)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803132,'AI Model Pricing',800234,803985,'N',22,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0e722a9f-9ebc-44ca-ab85-939e57275f52','N',2)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803133,'AIG_ModelPricing_UU',800234,803986,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','172d58e0-e64d-41c2-83f0-acbe6b43a578','N',2)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803134,'Model Name',800234,803988,'Y',100,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','34d77ca9-e49b-4a8e-b74f-3f9853b84b16','Y',30,5)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803135,'Input Cost / Million Token','Provider cost per million input tokens',800234,803989,'Y',63,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e17a4e39-e52a-4425-be0b-69f484afdbfd','Y',40,5)
;

-- Feb 27, 2026, 2:29:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803136,'Output Cost / Million Token','Provider cost per million output tokens',800234,803990,'Y',63,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','6114c0f9-f538-4c0a-b75e-d0decaba84f7','Y',50,5)
;

-- Feb 27, 2026, 2:30:00 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803137,'Currency','The Currency for this record','Indicates the Currency to be used when processing or reporting on this record',800234,803991,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:29:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','fc335fdb-2df4-4ded-b4ab-217d314aaabd','Y',60,2)
;

-- Feb 27, 2026, 2:30:00 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803138,'Valid from','Valid from including this date (first day)','The Valid From date indicates the first day of a date range',800234,803992,'Y',29,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:30:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:30:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e03ba2a5-9ae0-48a9-be43-e7748d6d2db0','Y',70,2)
;

-- Feb 27, 2026, 2:30:00 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803139,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800234,803984,'Y',1,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:30:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:30:00','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7fe62b9a-5087-4da4-8620-d07ea99f30aa','Y',80,2,2)
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803139
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Model Name', Description=NULL, Help=NULL, SeqNo=50, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803134
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Valid from', Description='Valid from including this date (first day)', Help='The Valid From date indicates the first day of a date range', IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803138
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Input Cost / Million Token', Description='Provider cost per million input tokens', Help=NULL, SeqNo=70, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803135
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Currency', Description='The Currency for this record', Help='Indicates the Currency to be used when processing or reporting on this record', IsDisplayed='Y', SeqNo=80, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803137
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='Output Cost / Million Token', Description='Provider cost per million output tokens', Help=NULL, SeqNo=90, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803136
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='AI Model Pricing', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803132
;

-- Feb 27, 2026, 2:30:30 PM CET
UPDATE AD_Field SET Name='AIG_ModelPricing_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:30:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803133
;

