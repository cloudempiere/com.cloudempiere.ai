-- CLD-1606: Create AIG_Prompt_Config table for configurable AI chat prompts
-- Minimal implementation - can be migrated to AIG_Task_Type architecture later
SELECT register_migration_script('202511241200_CLD-1606.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Nov 24, 2025, 1:47:02 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,Description,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800204,'Prompt Configuration','Configuration table for storing AI prompts','AIG_Prompt_Config',0,'6',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:47:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:47:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','26e7d622-0849-45c1-accd-64d6d492e729','N','N','N','N','N','N','N')
;

-- Nov 24, 2025, 1:47:02 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_Prompt_Config',1000000,'N','N','Table AIG_Prompt_Config','Y','Y',0,0,TO_TIMESTAMP('2025-11-24 13:47:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:47:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800263,'Y',1000000,1,200000,'1c435b84-a76c-4511-ac42-23e845d26cb0')
;

-- Nov 24, 2025, 1:47:02 PM CET
CREATE SEQUENCE AIG_PROMPT_CONFIG_SQ INCREMENT BY 1 MINVALUE 1000000 MAXVALUE 2147483647 START WITH 1000000
;

-- Nov 24, 2025, 1:48:52 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803481,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800204,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','2662cebf-e73b-45ba-a716-2d17b980860b','N','D')
;

-- Nov 24, 2025, 1:48:53 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803482,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800204,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','31ea54e2-f5ac-4063-af38-86e953eb6441','N','D')
;

-- Nov 24, 2025, 1:48:54 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800707,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Prompt_Config_ID','Prompt Configuration','Prompt Configuration','MM02','f0007276-a976-4ff8-a624-5c4755ea902e')
;

-- Nov 24, 2025, 1:48:54 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803483,0.0,'Prompt Configuration',800204,'AIG_Prompt_Config_ID',10,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800707,'N','N','MM02','N','f352529a-ed88-4f9b-918f-eaf14fa6d6e8','N')
;

-- Nov 24, 2025, 1:48:54 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800708,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Prompt_Config_UU','AIG_Prompt_Config_UU','AIG_Prompt_Config_UU','MM02','f8ef37b2-a14a-4e94-9648-a832974b1d60')
;

-- Nov 24, 2025, 1:48:55 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803484,0.0,'AIG_Prompt_Config_UU',800204,'AIG_Prompt_Config_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:54','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800708,'N','N','MM02','N','dda65363-12c8-4c49-8cf4-cde4e356fa0c','N')
;

-- Nov 24, 2025, 1:48:55 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,SeqNoSelection,IsToolbarButton) VALUES (803485,0.0,'Name','Alphanumeric identifier of the entity','The name of an entity (record) is used as an default search option in addition to the search key. The name is up to 60 characters in length.',800204,'Name',60,'N','N','Y','N','Y','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),469,'Y','Y','MM02','N','29e0cb64-5486-4efc-b546-bf900e3e9f38',10,'N')
;

-- Nov 24, 2025, 1:48:56 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,SeqNoSelection,IsToolbarButton) VALUES (803486,0.0,'Description','Optional short description of the record','A description is limited to 255 characters.',800204,'Description',255,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),275,'Y','Y','MM02','N','17b2d177-aaa5-45e1-938d-0d7682f1da62',20,'N')
;

-- Nov 24, 2025, 1:48:56 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800709,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'promptkey','promptkey','promptkey','MM02','4dfc9d6b-9909-4990-abab-9ebbfeda27d1')
;

-- Nov 24, 2025, 1:48:56 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803487,0.0,'promptkey',800204,'promptkey',40,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800709,'Y','N','MM02','N','8fdd1298-b7a0-43aa-a89f-51325635c1a7','N')
;

-- Nov 24, 2025, 1:48:57 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800710,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'prompttext','prompttext','prompttext','MM02','fd722c69-6a6c-4f17-919d-d83d0e5d9678')
;

-- Nov 24, 2025, 1:48:57 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803488,0.0,'prompttext',800204,'prompttext',2147483647,'N','N','Y','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800710,'Y','N','MM02','N','ceea1553-1122-46d4-bf21-fbd8adb4cd9b','N')
;

-- Nov 24, 2025, 1:48:58 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803489,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800204,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','d5b38063-eb0e-4c65-9c33-c4498c681979','N')
;

-- Nov 24, 2025, 1:48:58 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803490,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800204,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','8d3f5242-92e7-4af4-b2ee-ebf76b05f71c','N','D')
;

-- Nov 24, 2025, 1:48:59 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803491,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800204,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','17e68058-6f06-4381-864b-7a45b68c1aaf','N')
;

-- Nov 24, 2025, 1:48:59 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803492,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800204,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','833f0726-f2fe-41bc-9b3b-dd24c889ca30','N','D')
;

-- Nov 24, 2025, 1:48:59 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803493,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800204,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:48:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:48:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','273de897-21c6-475b-85cf-8de6582640a2','N')
;

-- Nov 24, 2025, 1:51:33 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800711,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:51:32','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:51:32','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIGPromptKey','Prompt Key','Prompt Key','U','6a3c7fe1-372e-4d28-9ae5-1a39cbbd4015')
;

-- Nov 24, 2025, 1:51:43 PM CET
ALTER TABLE AIG_Prompt_Config RENAME COLUMN promptkey TO AIGPromptKey
;

-- Nov 24, 2025, 1:51:43 PM CET
UPDATE AD_Column SET Name='Prompt Key', Description=NULL, Help=NULL, ColumnName='AIGPromptKey', AD_Element_ID=800711, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-24 13:51:43','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803487
;

-- Nov 24, 2025, 1:51:56 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='4dfc9d6b-9909-4990-abab-9ebbfeda27d1'
;

-- Nov 24, 2025, 1:52:37 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800712,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:52:36','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:52:36','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIGPromptText','Prompt Text','Prompt Text','U','e2b93605-1f55-4ac3-ba68-aca89bcae2b0')
;

-- Nov 24, 2025, 1:52:59 PM CET
ALTER TABLE AIG_Prompt_Config RENAME COLUMN prompttext TO AIGPromptText
;

-- Nov 24, 2025, 1:52:59 PM CET
UPDATE AD_Column SET Name='Prompt Text', Description=NULL, Help=NULL, ColumnName='AIGPromptText', AD_Element_ID=800712, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-24 13:52:59','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803488
;

-- Nov 24, 2025, 1:53:37 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='fd722c69-6a6c-4f17-919d-d83d0e5d9678'
;

-- Nov 24, 2025, 1:54:37 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800074,'Prompt Configuration',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','69d26e62-7937-4f50-a773-8cec7c23b14f')
;

-- Nov 24, 2025, 1:54:38 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800212,'Prompt Configuration',800074,10,'Y',800204,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_Prompt_Config.Name','N',0,'N','MM02','Y','N','5c57194a-8928-4fcd-8f02-56fd1a351b0a')
;

-- Nov 24, 2025, 1:54:38 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802781,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800212,803481,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','030c4992-a4d6-4dc7-8084-9a32c40af692','Y',10,2)
;

-- Nov 24, 2025, 1:54:38 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (802782,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800212,803482,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','623048b9-aad3-4fed-8f6f-5cc4fcde98d6','Y','N',4,2)
;

-- Nov 24, 2025, 1:54:39 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802783,'Name','Alphanumeric identifier of the entity','The name of an entity (record) is used as an default search option in addition to the search key. The name is up to 60 characters in length.',800212,803485,'Y',60,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','adc66aff-4590-4d35-ac93-5fa085f6631f','Y',20,5)
;

-- Nov 24, 2025, 1:54:39 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802784,'Description','Optional short description of the record','A description is limited to 255 characters.',800212,803486,'Y',255,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','f723d454-5ac2-4147-92b8-2c3ac527f532','Y',30,5)
;

-- Nov 24, 2025, 1:54:39 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802785,'Prompt Configuration',800212,803483,'N',10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','1e7083fd-a2cd-44d7-977e-d702046b91dc','N',2)
;

-- Nov 24, 2025, 1:54:39 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802786,'AIG_Prompt_Config_UU',800212,803484,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','18e4eaf5-b5cb-4b6f-bce5-78a206e36f24','N',2)
;

-- Nov 24, 2025, 1:54:40 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802787,'Prompt Key',800212,803487,'Y',40,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','cd69fa5f-29b1-4d52-89b7-c3d578603ff3','Y',40,2)
;

-- Nov 24, 2025, 1:54:40 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802788,'Prompt Text',800212,803488,'Y',2147483647,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b53ff404-20de-4850-b4a8-c8f9eb2b65e9','Y',50,5,3)
;

-- Nov 24, 2025, 1:54:40 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802789,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800212,803493,'Y',1,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','4f21cec8-092f-4f1d-b07e-7188d7ad0b29','Y',60,2,2)
;

-- Nov 24, 2025, 1:54:40 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,Action,AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800129,'Prompt Configuration','W',800074,0,0,'Y',TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','2ba91dbf-e3d2-4f93-9ddb-9d2ac2c439d8')
;

-- Nov 24, 2025, 1:54:40 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', getDate(), 1134855, getDate(), 1134855,t.AD_Tree_ID, 800129, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800129)
;

-- Nov 24, 2025, 1:54:40 PM CET
UPDATE AD_Table SET AD_Window_ID=800074,Updated=TO_TIMESTAMP('2025-11-24 13:54:40','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800204
;

-- Create physical table (Oracle syntax with CLOB)
CREATE TABLE AIG_Prompt_Config (
    AD_Client_ID NUMBER(10) NOT NULL,
    AD_Org_ID NUMBER(10) NOT NULL,
    AIG_Prompt_Config_ID NUMBER(10) NOT NULL,
    AIG_Prompt_Config_UU VARCHAR2(36 CHAR) DEFAULT NULL,
    Name VARCHAR2(60 CHAR) NOT NULL,
    Description VARCHAR2(255 CHAR) DEFAULT NULL,
    AIGPromptKey VARCHAR2(40 CHAR) NOT NULL,
    AIGPromptText CLOB NOT NULL,
    Created DATE NOT NULL,
    CreatedBy NUMBER(10) NOT NULL,
    Updated DATE NOT NULL,
    UpdatedBy NUMBER(10) NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')) NOT NULL,
    CONSTRAINT AIG_Prompt_Config_Key PRIMARY KEY (AIG_Prompt_Config_ID),
    CONSTRAINT AIG_Prompt_Config_UU_idx UNIQUE (AIG_Prompt_Config_UU),
    CONSTRAINT AIG_Prompt_Config_PromptKey_idx UNIQUE (PromptKey, AD_Client_ID)
);

-- Add foreign key constraints
ALTER TABLE AIG_Prompt_Config ADD CONSTRAINT ADClient_AIGPromptConfig FOREIGN KEY (AD_Client_ID) REFERENCES ad_client(ad_client_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE AIG_Prompt_Config ADD CONSTRAINT ADOrg_AIGPromptConfig FOREIGN KEY (AD_Org_ID) REFERENCES ad_org(ad_org_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE AIG_Prompt_Config ADD CONSTRAINT CreatedBy_AIGPromptConfig FOREIGN KEY (CreatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE AIG_Prompt_Config ADD CONSTRAINT UpdatedBy_AIGPromptConfig FOREIGN KEY (UpdatedBy) REFERENCES ad_user(ad_user_id) DEFERRABLE INITIALLY DEFERRED;

-- Add index on PromptKey for fast lookup
CREATE INDEX idx_aig_prompt_config_key ON AIG_Prompt_Config(PromptKey);

-- Insert default SYSTEM prompt
INSERT INTO AIG_Prompt_Config (AIG_Prompt_Config_ID, AD_Client_ID, AD_Org_ID, AIG_Prompt_Config_UU, Name, Description, PromptKey, PromptText, Created, CreatedBy, Updated, UpdatedBy, IsActive)
VALUES (
    AIG_PROMPT_CONFIG_SQ.NEXTVAL,
    0,
    0,
    Generate_UUID(),
    'Chat System Prompt',
    'Main system instructions for AI chat assistant',
    'SYSTEM',
    'You are a helpful AI assistant for iDempiere ERP system. You have access to query the database to answer user questions.

## Database Access
When users ask questions that require data from the system, use the query_database function to retrieve the information.

**When to use database queries:**
- User asks about specific records (orders, products, customers, etc.)
- User wants to see lists or summaries of data
- User asks ''how many'', ''show me'', ''list'', ''find'', etc.
- Questions about current state of business data

**When NOT to use database queries:**
- General questions about iDempiere features or concepts
- How-to questions that don''t need current data
- Questions already answered by provided context

## Common iDempiere Tables

**Business Partners:**
- C_BPartner: Business partners (customers, vendors)
- AD_User: Users and contacts

**Sales & Orders:**
- C_Order: Sales and purchase orders
- C_OrderLine: Order lines/items
- C_Invoice: Invoices

**Products:**
- M_Product: Products and services
- M_Product_Category: Product categories

**Common Columns:**
- Most tables have: IsActive, Created, Updated
- Name, Value, Description are common descriptive fields
- DocumentNo is used for document numbers

## Guidelines
- Be conversational and helpful
- When showing query results, format them clearly (use tables or lists)
- If a query returns no results, suggest alternatives
- Keep responses concise but informative',
    TO_DATE('2025-11-24 12:00:06','YYYY-MM-DD HH24:MI:SS'),
    100,
    TO_DATE('2025-11-24 12:00:06','YYYY-MM-DD HH24:MI:SS'),
    100,
    'Y'
);
