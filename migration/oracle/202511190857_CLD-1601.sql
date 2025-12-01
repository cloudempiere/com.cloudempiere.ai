-- CLD-1601
SELECT register_migration_script('202511190857_CLD-1601.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Nov 19, 2025, 8:57:12 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803458,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800202,'AD_User_ID',10,'N','N','N','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2025-11-19 08:57:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 08:57:11','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'N','N','MM02','N','6bb87822-6288-4d9a-80c4-cafdfe42d67f','N','N')
;

-- Nov 19, 2025, 8:57:32 AM CET
UPDATE AD_Column SET IsUpdateable='Y',Updated=TO_TIMESTAMP('2025-11-19 08:57:32','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803458
;

-- Nov 19, 2025, 9:00:06 AM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800203,'AI Query Audit','AIG_QueryAudit',0,'6',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:00:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:00:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','cb43dedf-51be-47f6-9f52-f66880f23136','N','N','N','N','N','N','N')
;

-- Nov 19, 2025, 9:00:06 AM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_QueryAudit',1000000,'N','N','Table AIG_QueryAudit','Y','Y',0,0,TO_TIMESTAMP('2025-11-19 09:00:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:00:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800262,'Y',1000000,1,200000,'7a87fc02-3a47-4b29-b981-58398851ddef')
;

-- Nov 19, 2025, 9:00:06 AM CET
CREATE SEQUENCE AIG_QUERYAUDIT_SQ INCREMENT BY 1 MINVALUE 1000000 MAXVALUE 2147483647 START WITH 1000000
;

-- Nov 19, 2025, 9:01:01 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800695,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_QueryAudit_ID','AI Query Audit','AI Query Audit','MM02','c18d7b42-6df8-4a26-a3c3-06c16a0d8cc6')
;

-- Nov 19, 2025, 9:01:02 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803459,0.0,'AI Query Audit',800203,'AIG_QueryAudit_ID',10,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800695,'N','N','MM02','N','39652450-a51a-4ccb-b58f-28b441ed187c','N')
;

-- Nov 19, 2025, 9:01:02 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803460,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800203,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','5ce32cc2-de84-4636-9d30-d04c01a50357','N','D')
;

-- Nov 19, 2025, 9:01:02 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803461,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800203,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','051e4184-a986-4ff9-a7da-a1ccfc951813','N','D')
;

-- Nov 19, 2025, 9:01:02 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803462,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800203,'IsActive','Y',1,'N','N','N','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','fef0a6c2-184a-4149-a615-c06c658b3b9b','N')
;

-- Nov 19, 2025, 9:01:02 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803463,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800203,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','9247f326-5872-424e-b5fb-8e5dd8cacac3','N')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803464,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800203,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:02','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','7dc7a47f-7c33-4f96-8703-0d48ffd2b9d9','N','D')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803465,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800203,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','6437e3b9-6c6f-4f25-bbaf-cbc5945adf66','N')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803466,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800203,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','bbf06935-71ca-463d-911b-dbca23abfef3','N','D')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800696,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_QueryAudit_UU','AIG_QueryAudit_UU','AIG_QueryAudit_UU','MM02','a7a5d43d-72ab-46cc-b07f-75f55b463160')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803467,0.0,'AIG_QueryAudit_UU',800203,'AIG_QueryAudit_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800696,'N','N','MM02','N','e515f1bd-8ab1-4527-bba9-24e0d69ad77c','N')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803468,0.0,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800203,'AIG_Provider_ID',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800692,'N','N','MM02','N','f81a2171-f17a-404e-800e-9d13106d6242','N','N')
;

-- Nov 19, 2025, 9:01:03 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803469,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800203,'AD_User_ID',10,'N','N','Y','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'N','N','MM02','N','0486bde7-41c0-4cda-8db1-198a64e3647b','N','N')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803470,0.0,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800203,'AD_Role_ID',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:03','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),123,'N','N','MM02','N','13c47c24-a888-42c6-9cd8-e4c6b0f9a593','N','C')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800697,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'querysql','querysql','querysql','MM02','553514c6-7903-4e89-ae23-9ee026e3f595')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803471,0.0,'querysql',800203,'querysql',2147483647,'N','N','Y','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800697,'Y','N','MM02','N','a0bc8701-8619-4483-bc86-71fe60b9bd9e','N')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800698,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'securedsql','securedsql','securedsql','MM02','589de4a3-b952-4b33-a886-c09f8d7c49e5')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803472,0.0,'securedsql',800203,'securedsql',2147483647,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800698,'Y','N','MM02','N','d14f6f12-89d7-4d87-8093-f7b285040316','N')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800699,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'querypurpose','querypurpose','querypurpose','MM02','b6f8575b-aadc-4a27-af3f-ef170b7ab069')
;

-- Nov 19, 2025, 9:01:04 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803473,0.0,'querypurpose',800203,'querypurpose',255,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800699,'Y','N','MM02','N','87484be6-959c-42d2-bb70-cc218ef288ea','N')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800700,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'contexttype','contexttype','contexttype','MM02','f55db99b-5ab3-4f76-a0e4-40847f42c346')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803474,0.0,'contexttype',800203,'contexttype',60,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:04','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800700,'Y','N','MM02','N','c27829a1-78c9-4e43-8ab2-c19d7b5450bb','N')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803475,0.0,'Status','Status of the currently running check','Status of the currently running check',800203,'Status','WA',60,'N','N','Y','N','N','N',17,toRecordId('AD_Reference','bb0a6404-830e-4b59-b95d-371c04930bf7'),0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),3020,'N','N','MM02','N','48bf3a44-9a53-4d4d-8af8-ea186a6bdf81','N','N')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800701,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'errormessage','errormessage','errormessage','MM02','7f1fa120-90e1-4ac2-b8a5-714ae304431b')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803476,0.0,'errormessage',800203,'errormessage',2000,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800701,'Y','N','MM02','N','0643af03-ff82-403c-bd4f-2aaae093bba5','N')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800702,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'rowcount','rowcount','rowcount','MM02','aa3168fd-20af-4a58-a49c-482ee7095281')
;

-- Nov 19, 2025, 9:01:05 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803477,0.0,'rowcount',800203,'rowcount',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800702,'Y','N','MM02','N','d37a7f4d-32f0-48cb-aa9a-dcac4ca840b6','N')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800703,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'executiontimems','executiontimems','executiontimems','MM02','4f30e555-32b8-400a-8c3b-f7ca2d593076')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803478,0.0,'executiontimems',800203,'executiontimems',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:05','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800703,'Y','N','MM02','N','82159c2e-7ef0-4b34-9e85-1c595fc3d749','N')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800704,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'tablesaccessed','tablesaccessed','tablesaccessed','MM02','829081e1-e526-49a1-8280-03dcb0ba3055')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803479,0.0,'tablesaccessed',800203,'tablesaccessed',2000,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800704,'Y','N','MM02','N','6f455cf4-4912-43c8-b0f0-659c5af4a36b','N')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800705,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'permissiondeniedreason','permissiondeniedreason','permissiondeniedreason','MM02','fe8e2936-cadc-4f27-9311-3d39beb7306d')
;

-- Nov 19, 2025, 9:01:06 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803480,0.0,'permissiondeniedreason',800203,'permissiondeniedreason',1000,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:01:06','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800705,'Y','N','MM02','N','bef325c1-94e1-480a-b858-bb3e7d3b658c','N')
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_Element SET ColumnName='AIGContextType', Name='Context Type', PrintName='Context Type',Updated=TO_TIMESTAMP('2025-11-19 09:02:04','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800700
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_Column SET ColumnName='AIGContextType', Name='Context Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800700
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGContextType', Name='Context Type', Description=NULL, Help=NULL, AD_Element_ID=800700 WHERE UPPER(ColumnName)='AIGCONTEXTTYPE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGContextType', Name='Context Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800700 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGContextType', Name='Context Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800700 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_Field SET Name='Context Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800700) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:04 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Context Type', Name='Context Type' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800700)
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_Element SET ColumnName='AIGErrorMessage', Name='Error Message', PrintName='Error Message',Updated=TO_TIMESTAMP('2025-11-19 09:02:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800701
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_Column SET ColumnName='AIGErrorMessage', Name='Error Message', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800701
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGErrorMessage', Name='Error Message', Description=NULL, Help=NULL, AD_Element_ID=800701 WHERE UPPER(ColumnName)='AIGERRORMESSAGE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGErrorMessage', Name='Error Message', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800701 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGErrorMessage', Name='Error Message', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800701 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_Field SET Name='Error Message', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800701) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:02:42 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Error Message', Name='Error Message' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800701)
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_Element SET ColumnName='AIGPermissionDeniedReason', Name='Permission Denied Reason', PrintName='Permission Denied Reason',Updated=TO_TIMESTAMP('2025-11-19 09:03:31','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800705
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_Column SET ColumnName='AIGPermissionDeniedReason', Name='Permission Denied Reason', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800705
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGPermissionDeniedReason', Name='Permission Denied Reason', Description=NULL, Help=NULL, AD_Element_ID=800705 WHERE UPPER(ColumnName)='AIGPERMISSIONDENIEDREASON' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGPermissionDeniedReason', Name='Permission Denied Reason', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800705 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGPermissionDeniedReason', Name='Permission Denied Reason', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800705 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_Field SET Name='Permission Denied Reason', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800705) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:03:31 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Permission Denied Reason', Name='Permission Denied Reason' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800705)
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_Element SET ColumnName='ExecutionTimeMs', Name='Execution Time in ms', PrintName='Execution Time in ms',Updated=TO_TIMESTAMP('2025-11-19 09:04:31','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800703
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_Column SET ColumnName='ExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_Process_Para SET ColumnName='ExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, AD_Element_ID=800703 WHERE UPPER(ColumnName)='EXECUTIONTIMEMS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_Process_Para SET ColumnName='ExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_InfoColumn SET ColumnName='ExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_Field SET Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800703) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:04:31 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Execution Time in ms', Name='Execution Time in ms' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800703)
;

-- Nov 19, 2025, 9:04:49 AM CET
UPDATE AD_Element SET ColumnName='AIGExecutionTimeMs',Updated=TO_TIMESTAMP('2025-11-19 09:04:49','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800703
;

-- Nov 19, 2025, 9:04:49 AM CET
UPDATE AD_Column SET ColumnName='AIGExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703
;

-- Nov 19, 2025, 9:04:49 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, AD_Element_ID=800703 WHERE UPPER(ColumnName)='AIGEXECUTIONTIMEMS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:04:49 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:04:49 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGExecutionTimeMs', Name='Execution Time in ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800703 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_Element SET ColumnName='AIGQueryPurpose', Name='Query Purpose', PrintName='Query Purpose',Updated=TO_TIMESTAMP('2025-11-19 09:05:21','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800699
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_Column SET ColumnName='AIGQueryPurpose', Name='Query Purpose', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800699
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGQueryPurpose', Name='Query Purpose', Description=NULL, Help=NULL, AD_Element_ID=800699 WHERE UPPER(ColumnName)='AIGQUERYPURPOSE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGQueryPurpose', Name='Query Purpose', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800699 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGQueryPurpose', Name='Query Purpose', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800699 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_Field SET Name='Query Purpose', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800699) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:21 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Query Purpose', Name='Query Purpose' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800699)
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_Element SET ColumnName='AIGQuerySQL', Name='Query SQL', PrintName='Query SQL',Updated=TO_TIMESTAMP('2025-11-19 09:05:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800697
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_Column SET ColumnName='AIGQuerySQL', Name='Query SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800697
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGQuerySQL', Name='Query SQL', Description=NULL, Help=NULL, AD_Element_ID=800697 WHERE UPPER(ColumnName)='AIGQUERYSQL' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGQuerySQL', Name='Query SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800697 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGQuerySQL', Name='Query SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800697 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_Field SET Name='Query SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800697) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:05:57 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Query SQL', Name='Query SQL' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800697)
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_Element SET ColumnName='AIGRowCount', Name='Row Count', PrintName='Row Count',Updated=TO_TIMESTAMP('2025-11-19 09:06:42','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800702
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_Column SET ColumnName='AIGRowCount', Name='Row Count', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800702
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGRowCount', Name='Row Count', Description=NULL, Help=NULL, AD_Element_ID=800702 WHERE UPPER(ColumnName)='AIGROWCOUNT' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGRowCount', Name='Row Count', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800702 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGRowCount', Name='Row Count', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800702 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_Field SET Name='Row Count', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800702) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:06:42 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Row Count', Name='Row Count' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800702)
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_Element SET ColumnName='SecuredSQL', Name='Secured SQL', PrintName='Secured SQL',Updated=TO_TIMESTAMP('2025-11-19 09:07:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800698
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_Column SET ColumnName='SecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_Process_Para SET ColumnName='SecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, AD_Element_ID=800698 WHERE UPPER(ColumnName)='SECUREDSQL' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_Process_Para SET ColumnName='SecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_InfoColumn SET ColumnName='SecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_Field SET Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800698) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:07:44 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Secured SQL', Name='Secured SQL' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800698)
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_Element SET ColumnName='TablesAccessed', Name='Tables Accessed', PrintName='Tables Accessed',Updated=TO_TIMESTAMP('2025-11-19 09:08:11','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800704
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_Column SET ColumnName='TablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_Process_Para SET ColumnName='TablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, AD_Element_ID=800704 WHERE UPPER(ColumnName)='TABLESACCESSED' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_Process_Para SET ColumnName='TablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_InfoColumn SET ColumnName='TablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_Field SET Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800704) AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:11 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Tables Accessed', Name='Tables Accessed' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800704)
;

-- Nov 19, 2025, 9:08:29 AM CET
UPDATE AD_Element SET ColumnName='AIGSecuredSQL',Updated=TO_TIMESTAMP('2025-11-19 09:08:29','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800698
;

-- Nov 19, 2025, 9:08:29 AM CET
UPDATE AD_Column SET ColumnName='AIGSecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698
;

-- Nov 19, 2025, 9:08:29 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGSecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, AD_Element_ID=800698 WHERE UPPER(ColumnName)='AIGSECUREDSQL' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:08:29 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGSecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:29 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGSecuredSQL', Name='Secured SQL', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800698 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:48 AM CET
UPDATE AD_Element SET ColumnName='AIGTablesAccessed',Updated=TO_TIMESTAMP('2025-11-19 09:08:48','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800704
;

-- Nov 19, 2025, 9:08:48 AM CET
UPDATE AD_Column SET ColumnName='AIGTablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704
;

-- Nov 19, 2025, 9:08:48 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGTablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, AD_Element_ID=800704 WHERE UPPER(ColumnName)='AIGTABLESACCESSED' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Nov 19, 2025, 9:08:48 AM CET
UPDATE AD_Process_Para SET ColumnName='AIGTablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:08:48 AM CET
UPDATE AD_InfoColumn SET ColumnName='AIGTablesAccessed', Name='Tables Accessed', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800704 AND IsCentrallyMaintained='Y'
;

-- Nov 19, 2025, 9:09:36 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGContextType VARCHAR2(60 CHAR) DEFAULT NULL 
;

-- Nov 19, 2025, 9:09:45 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGErrorMessage VARCHAR2(2000 CHAR) DEFAULT NULL 
;

-- Nov 19, 2025, 9:09:50 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGExecutionTimeMs NUMBER(10) DEFAULT NULL 
;

-- Nov 19, 2025, 9:09:55 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGPermissionDeniedReason VARCHAR2(1000 CHAR) DEFAULT NULL 
;

-- Nov 19, 2025, 9:09:59 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGQueryPurpose VARCHAR2(255 CHAR) DEFAULT NULL 
;

-- Nov 19, 2025, 9:10:04 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGQuerySQL CLOB NOT NULL
;

-- Nov 19, 2025, 9:10:08 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGRowCount NUMBER(10) DEFAULT NULL 
;

-- Nov 19, 2025, 9:10:12 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGSecuredSQL CLOB DEFAULT NULL 
;

-- Nov 19, 2025, 9:10:16 AM CET
ALTER TABLE AIG_QueryAudit ADD AIGTablesAccessed VARCHAR2(2000 CHAR) DEFAULT NULL 
;

ALTER TABLE AIG_QueryAudit DROP COLUMN contexttype
;

ALTER TABLE AIG_QueryAudit DROP COLUMN errormessage
;

ALTER TABLE AIG_QueryAudit DROP COLUMN rowcount
;

ALTER TABLE AIG_QueryAudit DROP COLUMN executiontimems
;

ALTER TABLE AIG_QueryAudit DROP COLUMN tablesaccessed
;

ALTER TABLE AIG_QueryAudit DROP COLUMN permissiondeniedreason
;


-- Nov 19, 2025, 9:19:09 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802762,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800210,803458,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:19:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:19:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','336b0965-4780-49f2-8d25-df79a8c034b6','Y',60,2)
;

-- Nov 19, 2025, 9:20:07 AM CET
UPDATE AD_Column SET IsParent='Y', IsUpdateable='N',Updated=TO_TIMESTAMP('2025-11-19 09:20:07','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803468
;

-- Nov 19, 2025, 9:20:37 AM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800211,'AI Query Audit',800073,20,'Y',800203,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:37','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_QueryAudit.Created DESC','N',1,'N','MM02','Y','N','086d297b-f05f-483b-9a6c-3e3ebded6df0')
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802763,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800211,803460,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','e02ed17a-6e2f-4275-a7a6-0b79cecc0021','Y',10,2)
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (802764,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800211,803461,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ab397ffb-e730-4a96-81eb-8e95f8fdbcf8','Y','N',4,2)
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802765,'AI Provider','Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)',800211,803468,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','c6e1f545-1b3f-43f7-9812-f0a55dcd3aa7','Y',20,2)
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802766,'AI Query Audit',800211,803459,'N',10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','07a21aef-610a-4dc5-8f56-000df22e8bfc','N',2)
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802767,'AIG_QueryAudit_UU',800211,803467,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','c88c6a84-5292-4356-b707-038baf03c330','N',2)
;

-- Nov 19, 2025, 9:20:38 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802768,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800211,803469,'Y',10,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b7708cec-a051-479d-ac56-3b27f3917586','Y',30,2)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802769,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800211,803470,'Y',10,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:38','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','9bbc5a36-6060-4823-a966-a8c082d47c99','Y',40,2)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802770,'Query SQL',800211,803471,'Y',2147483647,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5c0cc121-df56-41ee-b3ee-9441d009fae0','Y',50,5,3)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802771,'Secured SQL',800211,803472,'Y',2147483647,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0efad5a7-ba21-4961-8ffb-046de84e9bd0','Y',60,5,3)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802772,'Query Purpose',800211,803473,'Y',255,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b27850bd-6791-4828-9cda-b41359c919d1','Y',70,5)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802773,'Context Type',800211,803474,'Y',60,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e5827769-2e61-4283-9af7-016ce3398606','Y',80,5)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802774,'Status','Status of the currently running check','Status of the currently running check',800211,803475,'Y',60,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ca3bafd8-c164-4f61-a86b-e667bf872211','Y',90,5)
;

-- Nov 19, 2025, 9:20:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802775,'Error Message',800211,803476,'Y',2000,110,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b5f00a9e-a24a-4591-88c6-0937d91e245b','Y',100,5,3)
;

-- Nov 19, 2025, 9:20:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802776,'Row Count',800211,803477,'Y',10,120,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a2b7c504-4314-4a39-be88-a1427c8dfa37','Y',110,2)
;

-- Nov 19, 2025, 9:20:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802777,'Execution Time in ms',800211,803478,'Y',10,130,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','da0fe8a2-3d4a-416f-a59c-ac77cb0bd6ea','Y',120,2)
;

-- Nov 19, 2025, 9:20:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802778,'Tables Accessed',800211,803479,'Y',2000,140,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0e70c4a0-c92e-45a5-810f-35a788930b1c','Y',130,5,3)
;

-- Nov 19, 2025, 9:20:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802779,'Permission Denied Reason',800211,803480,'Y',1000,150,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','3223a38c-c1a3-44bb-ab5a-665a2ba51a0f','Y',140,5,3)
;

-- Nov 19, 2025, 9:20:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802780,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800211,803462,'Y',1,160,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','87bd01b7-4a7e-455b-9936-f04226227afb','Y',150,2,2)
;

-- Nov 19, 2025, 9:20:40 AM CET
UPDATE AD_Table SET AD_Window_ID=800073,Updated=TO_TIMESTAMP('2025-11-19 09:20:40','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800203
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802780
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802768
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Role', Description='Responsibility Role', Help='The Role determines security and access a user who has this Role will have in the System.', SeqNo=60, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802769
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Query SQL', Description=NULL, Help=NULL, SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802770
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Secured SQL', Description=NULL, Help=NULL, SeqNo=80, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802771
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Query Purpose', Description=NULL, Help=NULL, SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802772
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Context Type', Description=NULL, Help=NULL, SeqNo=100, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802773
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Status', Description='Status of the currently running check', Help='Status of the currently running check', SeqNo=110, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802774
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Error Message', Description=NULL, Help=NULL, SeqNo=120, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802775
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Row Count', Description=NULL, Help=NULL, SeqNo=130, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802776
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Execution Time in ms', Description=NULL, Help=NULL, SeqNo=140, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802777
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Tables Accessed', Description=NULL, Help=NULL, SeqNo=150, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802778
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='Permission Denied Reason', Description=NULL, Help=NULL, SeqNo=160, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802779
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='AI Query Audit', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802766
;

-- Nov 19, 2025, 9:22:00 AM CET
UPDATE AD_Field SET Name='AIG_QueryAudit_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:22:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802767
;

-- Nov 19, 2025, 9:28:29 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,Description,Help,PrintName,EntityType,AD_Element_UU) VALUES (800706,0,0,'Y',TO_TIMESTAMP('2025-11-19 09:28:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:28:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIGQueryStatus','Query Status','Status of the database query of the AI agent',NULL,'Query Status','MM02','035480c3-ff72-4a7d-9a11-ef43f9dd25df')
;

-- Nov 19, 2025, 9:28:48 AM CET
ALTER TABLE AIG_QueryAudit RENAME COLUMN Status TO AIGQueryStatus
;

-- Nov 19, 2025, 9:28:48 AM CET
UPDATE AD_Column SET Name='Query Status', Description='Status of the database query of the AI agent', Help=NULL, ColumnName='AIGQueryStatus', AD_Element_ID=800706, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-11-19 09:28:48','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803475
;

-- Nov 19, 2025, 9:29:07 AM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800125,'AIGQueryStatus','L',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:29:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:29:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','5ebae8a4-1ace-4097-bb6b-4d76c083aeb5','N')
;

-- Nov 19, 2025, 9:29:36 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800293,'Error',800125,'E',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:29:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:29:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','189e6588-d0ef-4535-9c4d-aae3b2b79ff3')
;

-- Nov 19, 2025, 9:31:52 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800294,'Permission Denied',800125,'P',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:31:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:31:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','a612cc27-69a6-4efe-9c33-13f7fa22905e')
;

-- Nov 19, 2025, 9:32:08 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800295,'Success',800125,'S',0,0,'Y',TO_TIMESTAMP('2025-11-19 09:32:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-11-19 09:32:08','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','202e5eab-e223-4fc2-acd8-fe87212364e1')
;

-- Nov 19, 2025, 9:32:44 AM CET
UPDATE AD_Column SET DefaultValue=NULL, AD_Reference_Value_ID=800125, IsAllowLogging='N',Updated=TO_TIMESTAMP('2025-11-19 09:32:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803475
;

ALTER TABLE AIG_QueryAudit DROP COLUMN querysql
;

ALTER TABLE AIG_QueryAudit DROP COLUMN securedsql
;

ALTER TABLE AIG_QueryAudit DROP COLUMN querypurpose
;

-- Nov 19, 2025, 10:33:37 AM CET
UPDATE AD_Tab SET IsReadOnly='Y', IsInsertRecord='N',Updated=TO_TIMESTAMP('2025-11-19 10:33:37','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Tab_ID=800211
;

