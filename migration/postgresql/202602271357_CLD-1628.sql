-- CLD-1628
SELECT register_migration_script('202602271357_CLD-1628.sql') FROM dual;

-- Feb 27, 2026, 1:57:35 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800225,'AI Usage Metrics','AIG_UsageMetrics',0,'6',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:57:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:57:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','ac18a3eb-9b05-4a2c-9d8b-187fea233305','N','N','N','N','N','N','N')
;

-- Feb 27, 2026, 1:57:35 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_UsageMetrics',1000000,'N','N','Table AIG_UsageMetrics','Y','Y',0,0,TO_TIMESTAMP('2026-02-27 13:57:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:57:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800629,'Y',1000000,1,200000,'0e824de4-1170-4b1f-9d57-38e6baca3603')
;

-- Feb 27, 2026, 1:57:35 PM CET
CREATE SEQUENCE AIG_USAGEMETRICS_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Feb 27, 2026, 1:58:08 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800837,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_UsageMetrics_ID','AI Usage Metrics','AI Usage Metrics','MM02','ac17307b-ca20-4e10-a882-0c5db1be431d')
;

-- Feb 27, 2026, 1:58:08 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803932,0.0,'AI Usage Metrics',800225,'AIG_UsageMetrics_ID',10,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800837,'N','N','MM02','N','4fd7d798-1c76-4f64-aa0d-a33e8ea5b65a','N')
;

-- Feb 27, 2026, 1:58:08 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803933,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800225,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','f6ba0e2a-613b-4965-aa39-c893c3cdc799','N','D')
;

-- Feb 27, 2026, 1:58:08 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803934,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800225,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','98c272be-5716-49ba-aca4-9d48cca29e8e','N','D')
;

-- Feb 27, 2026, 1:58:08 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803935,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800225,'IsActive','Y',1,'N','N','N','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','00bca81d-3723-4932-b22d-254637e8db2a','N')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803936,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800225,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','80e322f2-0daf-406c-ac86-3d6888a482be','N')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803937,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800225,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','cd321174-78ea-4b57-bbd5-ae899f7bf747','N','D')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803938,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800225,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','6463a8ed-5786-4c87-bf52-3d29840db04f','N')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803939,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800225,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','c8c91e51-a49c-4082-a0bd-59db06da4d6f','N','D')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800838,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_UsageMetrics_UU','AIG_UsageMetrics_UU','AIG_UsageMetrics_UU','MM02','768db774-eccc-445e-81dd-5a9e7d72dd75')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803940,0.0,'AIG_UsageMetrics_UU',800225,'AIG_UsageMetrics_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800838,'N','N','MM02','N','3d2452ef-b75b-4507-a8f5-fc9549e00171','N')
;

-- Feb 27, 2026, 1:58:09 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803941,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800225,'AD_User_ID',10,'N','N','Y','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'N','N','MM02','N','2ed0e20a-9ff6-4049-b8e1-7bd49db59688','N','N')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803942,0.0,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800225,'AD_Role_ID',10,'N','N','N','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),123,'N','N','MM02','N','40a0cfc5-9e19-403b-99fb-5d73aa03e87b','N','C')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800839,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'agentname','agentname','agentname','MM02','5b9e01d6-b40e-4130-882a-5a584e52ed5e')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803943,0.0,'agentname',800225,'agentname',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800839,'Y','N','MM02','N','5247dbbb-25cb-4383-aa57-7916fb08fd85','N')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800840,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'agenttype','agenttype','agenttype','MM02','4fece76e-ab6f-4180-8730-ecb5015fbd19')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803944,0.0,'agenttype',800225,'agenttype',60,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800840,'Y','N','MM02','N','0265d182-4838-44aa-8423-f1842cc9ead2','N')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800841,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'sessionid','sessionid','sessionid','MM02','f6e0220f-1c4a-47e5-b4e8-c4c6c4d63a4b')
;

-- Feb 27, 2026, 1:58:10 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803945,0.0,'sessionid',800225,'sessionid',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800841,'Y','N','MM02','N','2e1ea4fd-71dd-45bd-aa49-8f51d7cedf11','N')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803946,0.0,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800225,'AIG_Provider_ID',10,'N','N','N','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800692,'N','N','MM02','N','6538261b-dc9b-4c50-99cb-c398eebf58f5','N','N')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,SeqNoSelection,IsToolbarButton,FKConstraintType) VALUES (803947,0.0,'Model Name',800225,'ModelName',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800716,'Y','Y','MM02','N','ee6de200-9892-4b12-9f81-03b2d65b59ed',10,'N','N')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800842,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'inputtokens','inputtokens','inputtokens','MM02','ac9c0b6b-d82a-4d22-b822-5f6b424649c2')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803948,0.0,'inputtokens',800225,'inputtokens',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800842,'Y','N','MM02','N','65ad3bf0-3964-451e-99ce-046a4e1832cf','N')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800843,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'outputtokens','outputtokens','outputtokens','MM02','0b3e03e6-7a12-47be-b4e1-f6cba97ef261')
;

-- Feb 27, 2026, 1:58:11 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803949,0.0,'outputtokens',800225,'outputtokens',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800843,'Y','N','MM02','N','1b3aab6f-eef7-47dc-b9be-cdecbee6797b','N')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800844,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'totaltokens','totaltokens','totaltokens','MM02','7cea6e55-96b7-480a-b8e2-26605e1f6c27')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803950,0.0,'totaltokens',800225,'totaltokens',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800844,'Y','N','MM02','N','36197b42-fba9-4308-923a-da012707573c','N')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800845,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'costusd','costusd','costusd','MM02','714f1530-6b28-404a-8f1d-9e7b6717d547')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803951,0.0,'costusd',800225,'costusd',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800845,'Y','N','MM02','N','49d03adc-6bb3-448e-aaa6-2ba72c15e8f9','N')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800846,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'latencyms','latencyms','latencyms','MM02','dc4ac640-f913-44c8-9705-d3f0182c2eb9')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803952,0.0,'latencyms',800225,'latencyms',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800846,'Y','N','MM02','N','473600bf-f62c-4d08-b3a9-6c772f449742','N')
;

-- Feb 27, 2026, 1:58:12 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800847,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'requesttimestamp','requesttimestamp','requesttimestamp','MM02','2c9f37e1-de61-439f-a03c-e5dc2e4d7319')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803953,0.0,'requesttimestamp',800225,'requesttimestamp',29,'N','N','N','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800847,'Y','N','MM02','N','efe66173-0e5e-461a-9d82-09aecf3e54c9','N')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803954,0.0,'Request Type',800225,'RequestType',60,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),3063,'Y','N','MM02','N','fd775bfa-e7aa-4450-84aa-eeb57fa59845','N')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800848,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'requesthash','requesthash','requesthash','MM02','0e9d9cb2-97e7-41be-800a-93fe452fd0e8')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803955,0.0,'requesthash',800225,'requesthash',64,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800848,'Y','N','MM02','N','203ea3e5-8d12-4582-96f6-2f62e6014a9b','N')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800849,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'successflag','successflag','successflag','MM02','24721550-ee12-4bda-8e22-9cc1e7538348')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803956,0.0,'successflag',800225,'successflag',1,'N','N','N','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800849,'Y','N','MM02','N','eda7f840-9f92-4839-a01b-3c6085413ea9','N')
;

-- Feb 27, 2026, 1:58:13 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803957,0.0,'Error Message',800225,'ErrorMessage',2000,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:13','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800723,'Y','N','MM02','N','c4b89ae9-989f-4edf-b21b-881236257639','N','N')
;

-- Feb 27, 2026, 1:58:47 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800083,'AI Usage Metrics',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','d958ee5e-0b65-46dc-9b7c-338ff2866489')
;

-- Feb 27, 2026, 1:58:48 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800231,'AI Usage Metrics',800083,10,'Y',800225,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_UsageMetrics.Created DESC','N',0,'N','MM02','Y','N','068bd573-f0c4-4ca5-bab1-d96a0a3fd078')
;

-- Feb 27, 2026, 1:58:48 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803080,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800231,803933,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','a0b42dc8-0e86-4f87-933e-49480176f3a5','Y',10,2)
;

-- Feb 27, 2026, 1:58:48 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (803081,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800231,803934,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','69b447be-8344-4a1e-a404-af19475bcc3e','Y','N',4,2)
;

-- Feb 27, 2026, 1:58:48 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803082,'AI Usage Metrics',800231,803932,'N',10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','bea7049e-dac4-42cb-8e71-8a2f6ecfc8a1','N',2)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803083,'AIG_UsageMetrics_UU',800231,803940,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:48','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','23970832-0c9f-4230-955f-c18ad33068cd','N',2)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803084,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800231,803941,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','509e0ce4-0f0b-4748-9721-5af21f3d8f55','Y',20,2)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803085,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800231,803942,'Y',10,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','dae26834-9ef8-40da-a013-15d68f6ad655','Y',30,2)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803086,'agentname',800231,803943,'Y',100,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ebd59c2c-5f5d-4ad1-bccb-8d6adb75759e','Y',40,5)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803087,'agenttype',800231,803944,'Y',60,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0e525769-0601-47ee-8453-37774c991c88','Y',50,5)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803088,'sessionid',800231,803945,'Y',100,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','782a6e0f-afbb-4f5f-8b9b-0ce83f741d0a','Y',60,5)
;

-- Feb 27, 2026, 1:58:49 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803089,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800231,803946,'Y',10,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a57c0070-e4bb-41dc-bb3a-4b64bbee1206','Y',70,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803090,'Model Name',800231,803947,'Y',100,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7b71f7e3-7fee-4a3b-a029-8c5988a961c3','Y',80,5)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803091,'inputtokens',800231,803948,'Y',10,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','2393ceff-9ada-42cd-bdf6-56309b4be82d','Y',90,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803092,'outputtokens',800231,803949,'Y',10,110,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','624e0e89-8843-404a-be0c-c2a2576b6d96','Y',100,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803093,'totaltokens',800231,803950,'Y',10,120,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e22f8754-7f5b-4d98-8b3e-057afb65d9e2','Y',110,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803094,'costusd',800231,803951,'Y',10,130,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ce36d2b4-4d77-47da-bb3d-2af2b27eb397','Y',120,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803095,'latencyms',800231,803952,'Y',10,140,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b5c1b830-5f21-42c3-9442-c7dae591b292','Y',130,2)
;

-- Feb 27, 2026, 1:58:50 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803096,'requesttimestamp',800231,803953,'Y',29,150,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','71b68c64-e131-4f84-a59c-c8f48114e238','Y',140,2)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803097,'Request Type',800231,803954,'Y',60,160,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:50','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5c1c0634-72db-47c8-9644-9123c0de5f93','Y',150,5)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803098,'requesthash',800231,803955,'Y',64,170,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','d35ea793-2ec0-429d-93b9-5bbc6914d1ec','Y',160,5)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803099,'successflag',800231,803956,'Y',1,180,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','8834e087-0823-48ba-a48a-644ca481fddc','Y',170,2,2)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (803100,'Error Message',800231,803957,'Y',2000,190,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','63710eb8-c90c-4c7b-a1ca-85fbf49e0d63','Y',180,5,3)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803101,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800231,803935,'Y',1,200,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7e30ea2c-a500-44ef-a51b-de38ba353e7d','Y',190,2,2)
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,"action",AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800145,'AI Usage Metrics','W',800083,0,0,'Y',TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','1b387cc7-9140-4290-8a13-faf88f8f5f31')
;

-- Feb 27, 2026, 1:58:51 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', statement_timestamp(), 1134855, statement_timestamp(), 1134855,t.AD_Tree_ID, 800145, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800145)
;

-- Feb 27, 2026, 1:58:51 PM CET
UPDATE AD_Table SET AD_Window_ID=800083,Updated=TO_TIMESTAMP('2026-02-27 13:58:51','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800225
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='Role', Description='Responsibility Role', Help='The Role determines security and access a user who has this Role will have in the System.', IsDisplayed='Y', SeqNo=30, XPosition=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803085
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803101
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803084
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='agentname', Description=NULL, Help=NULL, SeqNo=60, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803086
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='agenttype', Description=NULL, Help=NULL, SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803087
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='sessionid', Description=NULL, Help=NULL, SeqNo=80, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803088
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='AI Provider', Description='Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)', Help=NULL, SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803089
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='Model Name', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=100, XPosition=4, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803090
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='inputtokens', Description=NULL, Help=NULL, SeqNo=110, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803091
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='requesttimestamp', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=120, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803096
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='outputtokens', Description=NULL, Help=NULL, SeqNo=130, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803092
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='successflag', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=140, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803099
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='totaltokens', Description=NULL, Help=NULL, SeqNo=150, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803093
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='Request Type', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=160, XPosition=4, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803097
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='costusd', Description=NULL, Help=NULL, SeqNo=170, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803094
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='requesthash', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=180, XPosition=4, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803098
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='latencyms', Description=NULL, Help=NULL, SeqNo=190, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803095
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='Error Message', Description=NULL, Help=NULL, SeqNo=200, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803100
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='AI Usage Metrics', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803082
;

-- Feb 27, 2026, 2:00:06 PM CET
UPDATE AD_Field SET Name='AIG_UsageMetrics_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:00:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803083
;

-- Feb 27, 2026, 2:01:31 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800226,'AI Budget','AIG_Budget',0,'6',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','8bc26497-b0f1-4924-93df-a6fcdf95cdbc','N','N','N','N','N','N','N')
;

-- Feb 27, 2026, 2:01:31 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_Budget',1000000,'N','N','Table AIG_Budget','Y','Y',0,0,TO_TIMESTAMP('2026-02-27 14:01:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800630,'Y',1000000,1,200000,'6e8ef424-6860-4305-b29b-722bf7926951')
;

-- Feb 27, 2026, 2:01:31 PM CET
CREATE SEQUENCE AIG_BUDGET_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Feb 27, 2026, 2:01:43 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800850,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Budget_ID','AI Budget','AI Budget','MM02','1054e650-ea50-4628-afd7-0a6bbe998432')
;

-- Feb 27, 2026, 2:01:43 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803958,0.0,'AI Budget',800226,'AIG_Budget_ID',10,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800850,'N','N','MM02','N','ab39ce9d-2e0b-47ac-a2d7-ba22bbe3e918','N')
;

-- Feb 27, 2026, 2:01:43 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803959,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800226,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','777d6a67-5e68-47fc-a4f0-59da3a8a6d2d','N','D')
;

-- Feb 27, 2026, 2:01:43 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803960,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800226,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','0612b421-63ce-4f4d-912b-220f742dffe6','N','D')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803961,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800226,'IsActive','Y',1,'N','N','N','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','48e67b43-8922-4240-94e8-a06abaa454a3','N')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803962,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800226,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','804881d2-124d-4cb7-b2e0-2265b0580b6a','N')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803963,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800226,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','6afed121-7753-4289-9d27-d1d4dbf4fe71','N','D')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803964,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800226,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','21693acc-6a4a-4f3c-ac2c-f63553da7f3c','N')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803965,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800226,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','e955a9d7-e71f-48fd-942b-56048a573fa0','N','D')
;

-- Feb 27, 2026, 2:01:44 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800851,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Budget_UU','AIG_Budget_UU','AIG_Budget_UU','MM02','90f40557-778a-4d94-b7ad-c72085ccfdab')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803966,0.0,'AIG_Budget_UU',800226,'AIG_Budget_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800851,'N','N','MM02','N','a9cd1186-caf8-4ac8-b108-73bcb37c4b97','N')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803967,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800226,'AD_User_ID',10,'N','N','N','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'N','N','MM02','N','1480ac86-d567-44d3-b296-34c52dda7ebd','N','N')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803968,0.0,'agentname',800226,'agentname',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800839,'Y','N','MM02','N','4a924e48-2854-49af-9d2f-c29f8b7fcc5d','N','N')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800852,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'budgetscope','budgetscope','budgetscope','MM02','6039d869-59a8-4771-b5bd-5b439db4c6f0')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803969,0.0,'budgetscope',800226,'budgetscope',60,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800852,'Y','N','MM02','N','aded435d-5b3c-4f61-a536-20d5ac6eee3e','N')
;

-- Feb 27, 2026, 2:01:45 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800853,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'dailylimitusd','dailylimitusd','dailylimitusd','MM02','1a2a4b43-3e1a-4375-aab2-f07d1a5be388')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803970,0.0,'dailylimitusd',800226,'dailylimitusd',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800853,'Y','N','MM02','N','d6cf2edc-07db-4ee0-9496-2860ddea5d33','N')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800854,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'monthlylimitusd','monthlylimitusd','monthlylimitusd','MM02','785a29d2-730a-4af6-aa5c-42d3ef763742')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803971,0.0,'monthlylimitusd',800226,'monthlylimitusd',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800854,'Y','N','MM02','N','a3f54613-31e9-455d-bd69-39361d7bcd78','N')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800855,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'tokenlimitperrequest','tokenlimitperrequest','tokenlimitperrequest','MM02','f5867dfa-1096-4703-91d0-dbc5a25c3d71')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803972,0.0,'tokenlimitperrequest',800226,'tokenlimitperrequest',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800855,'Y','N','MM02','N','29e78f82-cf24-4b54-a21b-7b36652016d4','N')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800856,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'requestsperminute','requestsperminute','requestsperminute','MM02','bd7ed798-1087-41d4-9f8c-ebe796fc2ded')
;

-- Feb 27, 2026, 2:01:46 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803973,0.0,'requestsperminute',800226,'requestsperminute',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800856,'Y','N','MM02','N','79abcfbb-32b9-4ea5-aa4b-4ec9ad5e4838','N')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800857,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'currentdailyusd','currentdailyusd','currentdailyusd','MM02','d0da1ffc-c1e2-455e-b6d9-c057c7533048')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803974,0.0,'currentdailyusd',800226,'currentdailyusd',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:46','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800857,'Y','N','MM02','N','2c7d304e-6532-48b4-8cc6-f5fda5d1d760','N')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800858,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'currentmonthlyusd','currentmonthlyusd','currentmonthlyusd','MM02','630bcf70-0744-4993-919d-d9e203862009')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803975,0.0,'currentmonthlyusd',800226,'currentmonthlyusd',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800858,'Y','N','MM02','N','a5550517-2779-4a38-b5a1-bb6b056ef56d','N')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800859,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'lastresetdaily','lastresetdaily','lastresetdaily','MM02','8e30228b-9aa0-409d-985a-a50b4d321921')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803976,0.0,'lastresetdaily',800226,'lastresetdaily',29,'N','N','N','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800859,'Y','N','MM02','N','43aa2169-4893-417c-af13-c1a2a92b7fe5','N')
;

-- Feb 27, 2026, 2:01:47 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800860,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'lastresetmonthly','lastresetmonthly','lastresetmonthly','MM02','a146b6b0-3e92-450d-a124-8b07a3f98f16')
;

-- Feb 27, 2026, 2:01:48 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803977,0.0,'lastresetmonthly',800226,'lastresetmonthly',29,'N','N','N','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:01:47','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800860,'Y','N','MM02','N','6bec604d-1dc6-450e-8eec-8e62a5e0da65','N')
;

-- Feb 27, 2026, 2:02:07 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800084,'AI Budget',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','452ba1f4-e121-4abc-bcac-12f4a5efc1ea')
;

-- Feb 27, 2026, 2:02:07 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800232,'AI Budget',800084,10,'Y',800226,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_Budget.Created DESC','N',0,'N','MM02','Y','N','04ff8e5e-c3de-475a-b05f-368900702774')
;

-- Feb 27, 2026, 2:02:07 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803102,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800232,803959,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','f35ee7a3-b488-416b-ba7e-54f1d49ab379','Y',10,2)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (803103,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800232,803960,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','32cc1a7a-b6c0-4448-a591-c8a05d0dc7a2','Y','N',4,2)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803104,'AI Budget',800232,803958,'N',10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','d7b1e198-7890-4d13-b02d-37acc1012c7c','N',2)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (803105,'AIG_Budget_UU',800232,803966,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','c2b2b447-6453-42bb-98b6-6b45aed523cc','N',2)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803106,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800232,803967,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','157383fd-a045-4fb3-bd7e-a2a0b0a2bc04','Y',20,2)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803107,'agentname',800232,803968,'Y',100,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7634f08f-28a2-4292-b471-53f5bff8bbc7','Y',30,5)
;

-- Feb 27, 2026, 2:02:08 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803108,'budgetscope',800232,803969,'Y',60,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','8ba08160-0e43-46e7-9e24-bdd54716b47a','Y',40,5)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803109,'dailylimitusd',800232,803970,'Y',10,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0d72ad68-5366-4c73-853b-157328ccf62f','Y',50,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803110,'monthlylimitusd',800232,803971,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','087e4909-f33c-4203-845f-729b64179d58','Y',60,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803111,'tokenlimitperrequest',800232,803972,'Y',10,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e7c0c338-5421-4d62-a207-4f2655194fc1','Y',70,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803112,'requestsperminute',800232,803973,'Y',10,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','62907f74-c166-42d4-beff-bd8ee104b0e1','Y',80,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803113,'currentdailyusd',800232,803974,'Y',10,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','857e2087-79b8-4286-9ff0-d10ff2e5e815','Y',90,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803114,'currentmonthlyusd',800232,803975,'Y',10,110,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a7d8e454-7afa-4869-a65b-c901ab83923b','Y',100,2)
;

-- Feb 27, 2026, 2:02:09 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803115,'lastresetdaily',800232,803976,'Y',29,120,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','20bde066-ece4-4b13-9071-3b1c8144c773','Y',110,2)
;

-- Feb 27, 2026, 2:02:10 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (803116,'lastresetmonthly',800232,803977,'Y',29,130,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:09','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','3c5079a7-feec-4e62-9072-18c5da9eb1e0','Y',120,2)
;

-- Feb 27, 2026, 2:02:10 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (803117,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800232,803961,'Y',1,140,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','fd3bd4f5-d798-425b-aa8b-74daa925515b','Y',130,2,2)
;

-- Feb 27, 2026, 2:02:10 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,"action",AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800146,'AI Budget','W',800084,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:02:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:02:10','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','94bbe59e-5e37-4ee9-b43b-f9edae8a0b59')
;

-- Feb 27, 2026, 2:02:10 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', statement_timestamp(), 1134855, statement_timestamp(), 1134855,t.AD_Tree_ID, 800146, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800146)
;

-- Feb 27, 2026, 2:02:10 PM CET
UPDATE AD_Table SET AD_Window_ID=800084,Updated=TO_TIMESTAMP('2026-02-27 14:02:10','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800226
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803117
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='agentname', Description=NULL, Help=NULL, SeqNo=50, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803107
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='lastresetdaily', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803115
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='budgetscope', Description=NULL, Help=NULL, SeqNo=70, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803108
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='lastresetmonthly', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=80, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803116
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='dailylimitusd', Description=NULL, Help=NULL, SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803109
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='monthlylimitusd', Description=NULL, Help=NULL, SeqNo=100, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803110
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='tokenlimitperrequest', Description=NULL, Help=NULL, SeqNo=110, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803111
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='requestsperminute', Description=NULL, Help=NULL, SeqNo=120, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803112
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='currentdailyusd', Description=NULL, Help=NULL, SeqNo=130, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803113
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='currentmonthlyusd', Description=NULL, Help=NULL, SeqNo=140, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803114
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='AI Budget', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803104
;

-- Feb 27, 2026, 2:02:42 PM CET
UPDATE AD_Field SET Name='AIG_Budget_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=803105
;

