-- CLD-1601
SELECT register_migration_script('202512291504_CLD-1601.sql') FROM dual;

-- Dec 29, 2025, 3:06:07 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800210,'AI Ingestion Metadata','AIG_IngestionMetadata',0,'6',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','3b9a2934-11f7-4fbc-a2f8-cef3b3795450','N','N','N','N','N','N','N')
;

-- Dec 29, 2025, 3:06:07 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_IngestionMetadata',1000000,'N','N','Table AIG_IngestionMetadata','Y','Y',0,0,TO_TIMESTAMP('2025-12-29 15:06:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:07','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800613,'Y',1000000,1,200000,'72ba291b-1c70-481c-b8b3-537c45c78557')
;

-- Dec 29, 2025, 3:06:07 PM CET
CREATE SEQUENCE AIG_INGESTIONMETADATA_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Dec 29, 2025, 3:06:28 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800752,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_IngestionMetadata_UU','AIG_IngestionMetadata_UU','AIG_IngestionMetadata_UU','MM02','4e5869b0-e6d1-4136-93ee-8f67d368e2c7')
;

-- Dec 29, 2025, 3:06:28 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803577,0.0,'AIG_IngestionMetadata_UU',800210,'AIG_IngestionMetadata_UU',2147483647,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800752,'N','N','MM02','N','43376f8e-74f2-4290-98ce-4e8530fbec9e','N')
;

-- Dec 29, 2025, 3:06:28 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803578,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800210,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','1f8a6fde-052b-45fe-9727-26e7220434aa','N','D')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803579,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800210,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','8c50852d-d4c4-4568-971a-f414691227de','N','D')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803580,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800210,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','89b72f0b-4a96-4ac4-bcca-25bdb5e67cdc','N')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803581,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800210,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','957e6779-084f-405e-b6ed-23a7553674e0','N')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803582,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800210,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','65929473-b0d7-46d7-b6c0-56f0b2a7fc28','N','D')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803583,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800210,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','b719fce5-3100-43b7-873a-2169e25e85ed','N')
;

-- Dec 29, 2025, 3:06:29 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803584,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800210,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','21ccb598-9d22-4c34-b417-4d8287805e34','N','D')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803585,0.0,'sourcetype',800210,'sourcetype',50,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:29','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800749,'Y','N','MM02','N','bc17632b-8642-4ad6-bca6-b4fe0f1f3257','N','N')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800753,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'lastingestion','lastingestion','lastingestion','MM02','c51f8979-33c3-46f4-8503-53165bde2ed4')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803586,0.0,'lastingestion',800210,'lastingestion',29,'N','N','N','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800753,'Y','N','MM02','N','6243f5d1-f5f6-4301-a85f-f0ac876237cb','N')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800754,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'lastsuccessfulingestion','lastsuccessfulingestion','lastsuccessfulingestion','MM02','9a8dc63a-2567-4239-b5b5-620999702982')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803587,0.0,'lastsuccessfulingestion',800210,'lastsuccessfulingestion',29,'N','N','N','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800754,'Y','N','MM02','N','d1d37a03-1548-43ad-b372-df6160f1ffea','N')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800755,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'recordcount','recordcount','recordcount','MM02','7bd6d306-6100-4a00-822d-cbe9c28746fb')
;

-- Dec 29, 2025, 3:06:30 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803588,0.0,'recordcount',800210,'recordcount',10,'N','N','N','N','N','N',11,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:30','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800755,'Y','N','MM02','N','1031bbfc-a977-46ec-b7ee-0a005d52b8e1','N')
;

-- Dec 29, 2025, 3:06:31 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803589,0.0,'Status','Status of the currently running check','Status of the currently running check',800210,'Status','WA',20,'N','N','N','N','N','N',17,toRecordId('AD_Reference','bb0a6404-830e-4b59-b95d-371c04930bf7'),0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),3020,'N','N','MM02','N','61edda78-11b8-4f7a-b907-4c56bbfca306','N','N')
;

-- Dec 29, 2025, 3:06:31 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803590,0.0,'Error Message',800210,'ErrorMessage',2147483647,'N','N','N','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800723,'Y','N','MM02','N','a86fadce-a330-45f7-bfcf-98cda0edf841','N','N')
;

-- Dec 29, 2025, 3:06:31 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800756,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'durationms','durationms','durationms','MM02','bfde5a98-fe51-49a5-b5e6-ada8d0c45b52')
;

-- Dec 29, 2025, 3:06:31 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803591,0.0,'durationms',800210,'durationms',19,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:31','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800756,'Y','N','MM02','N','13494385-a4c9-4cc8-9426-60742c8ff04b','N')
;

-- Dec 29, 2025, 3:06:56 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800077,'AI Embedding',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','96c4e326-87ce-4439-ad46-6addf227104a')
;

-- Dec 29, 2025, 3:06:57 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800217,'AI Embedding',800077,10,'Y',800208,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_Embedding.Created DESC','N',0,'N','MM02','Y','N','2d6808c2-96dd-4bde-afbc-e18742e626fc')
;

-- Dec 29, 2025, 3:06:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802866,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800217,803561,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','c4638f95-aa76-41a3-b6b6-a0cbc29bf956','Y',10,2)
;

-- Dec 29, 2025, 3:06:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (802867,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800217,803562,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7ab0994a-641d-4da7-bb12-7a69b36d29cb','Y','N',4,2)
;

-- Dec 29, 2025, 3:06:57 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802868,'AIG_Embedding_UU',800217,803560,'N',2147483647,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','b29eb008-0325-48b7-83d2-fbe6217bb659','N',5)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802869,'Embedding',800217,803568,'Y',2147483647,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:57','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','9cf41f17-eb98-4fbd-875e-e08e9d35dfac','Y',20,5)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802870,'textsegment',800217,803569,'Y',2147483647,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e4017c46-7da6-49ad-8f86-ec09fbebae89','Y',30,5,3)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802871,'textsegmenthash',800217,803570,'Y',64,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','6a0ff5b5-a413-400a-b70b-94254c04e1ff','Y',40,5)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802872,'metadata',800217,803571,'Y',2147483647,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','deef8e9c-9f4d-432e-aad3-19508eee5c50','Y',50,5)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802873,'sourcetype',800217,803572,'Y',50,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ba69dea2-8cdf-4845-83c7-d36d03bd08ef','Y',60,2)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802874,'sourceid',800217,803573,'Y',100,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','46000e23-f928-4c98-9595-8787821f0605','Y',70,5)
;

-- Dec 29, 2025, 3:06:58 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802875,'sourcetable',800217,803574,'Y',100,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','e59757fd-5125-4bc9-a46d-5d9441e1d3e5','Y',80,5)
;

-- Dec 29, 2025, 3:06:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802876,'Language','Language for this entity','The Language identifies the language to use for display and formatting',800217,803575,'Y',6,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:58','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','07dac6e1-80f2-4eed-ad8e-97df75111d14','Y',90,2)
;

-- Dec 29, 2025, 3:06:59 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802877,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800217,803563,'Y',1,110,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','72d423ad-fa12-40f6-b335-578c1d528723','Y',100,2,2)
;

-- Dec 29, 2025, 3:06:59 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,"action",AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800132,'AI Embedding','W',800077,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:06:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:06:59','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','2166acaa-5dbf-4d14-9cb7-b59aef310a73')
;

-- Dec 29, 2025, 3:06:59 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', statement_timestamp(), 1134855, statement_timestamp(), 1134855,t.AD_Tree_ID, 800132, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800132)
;

-- Dec 29, 2025, 3:06:59 PM CET
UPDATE AD_Table SET AD_Window_ID=800077,Updated=TO_TIMESTAMP('2025-12-29 15:06:59','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800208
;

-- Dec 29, 2025, 3:07:43 PM CET
INSERT INTO AD_Window (AD_Window_ID,Name,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,WindowType,Processing,EntityType,IsSOTrx,IsDefault,IsBetaFunctionality,AD_Window_UU) VALUES (800078,'AI Ingestion Metadata',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'M','N','MM02','N','N','N','7ced1326-31bf-46f0-bdc0-3ab0f4a3cd61')
;

-- Dec 29, 2025, 3:07:43 PM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800218,'AI Ingestion Metadata',800078,10,'Y',800210,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_IngestionMetadata.Created DESC','N',0,'N','MM02','Y','N','fa6500fb-bc22-4b67-b498-938e559325c6')
;

-- Dec 29, 2025, 3:07:43 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802878,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800218,803578,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','4dff147f-1fda-49c8-8fa3-d1ceca02e67f','Y',10,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (802879,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800218,803579,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','8c842ec8-024a-4db0-945b-282e71851891','Y','N',4,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802880,'AIG_IngestionMetadata_UU',800218,803577,'N',2147483647,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','71b03fa8-6167-48a9-b9c6-423ad1107898','N',5)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802881,'sourcetype',800218,803585,'Y',50,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5eb88e6c-d9b5-482b-85c6-aebec61d95a7','Y',20,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802882,'lastingestion',800218,803586,'Y',29,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','dd5e523f-8f64-4592-abcd-e1adca4d79d3','Y',30,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802883,'lastsuccessfulingestion',800218,803587,'Y',29,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','15f60afb-3581-46ac-b086-07604ca9b816','Y',40,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802884,'recordcount',800218,803588,'Y',10,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','48240ec2-b54e-4fa3-9c5a-4b6091583d38','Y',50,2)
;

-- Dec 29, 2025, 3:07:44 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802885,'Status','Status of the currently running check','Status of the currently running check',800218,803589,'Y',20,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','3b9f5fa7-53e6-465d-a1bd-68e54e784f21','Y',60,2)
;

-- Dec 29, 2025, 3:07:45 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (802886,'Error Message',800218,803590,'Y',2147483647,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:44','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','50b04fca-2dd1-428b-8bca-ba729febf903','Y',70,5,3)
;

-- Dec 29, 2025, 3:07:45 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802887,'durationms',800218,803591,'Y',19,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','39848cd4-cbb9-4f58-813b-224835cfb542','Y',80,2)
;

-- Dec 29, 2025, 3:07:45 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802888,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800218,803580,'Y',1,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','1aadef18-c3f8-44dd-bd79-05e01b827bb9','Y',90,2,2)
;

-- Dec 29, 2025, 3:07:45 PM CET
INSERT INTO AD_Menu (AD_Menu_ID,Name,"action",AD_Window_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSummary,IsSOTrx,IsReadOnly,EntityType,AD_Menu_UU) VALUES (800133,'AI Ingestion Metadata','W',800078,0,0,'Y',TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','MM02','a131262c-ecf2-4cf5-8514-68392f8c0fa4')
;

-- Dec 29, 2025, 3:07:45 PM CET
INSERT INTO AD_TreeNodeMM (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, AD_Tree_ID, Node_ID, Parent_ID, SeqNo, AD_TreeNodeMM_UU) SELECT t.AD_Client_ID, 0, 'Y', statement_timestamp(), 1134855, statement_timestamp(), 1134855,t.AD_Tree_ID, 800133, 0, 999, Generate_UUID() FROM AD_Tree t WHERE t.AD_Client_ID=0 AND t.IsActive='Y' AND t.IsAllNodes='Y' AND t.TreeType='MM' AND NOT EXISTS (SELECT * FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=800133)
;

-- Dec 29, 2025, 3:07:45 PM CET
UPDATE AD_Table SET AD_Window_ID=800078,Updated=TO_TIMESTAMP('2025-12-29 15:07:45','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800210
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802888
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='lastingestion', Description=NULL, Help=NULL, SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802882
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='recordcount', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802884
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='lastsuccessfulingestion', Description=NULL, Help=NULL, SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802883
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='Status', Description='Status of the currently running check', Help='Status of the currently running check', IsDisplayed='Y', SeqNo=80, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802885
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='Error Message', Description=NULL, Help=NULL, SeqNo=100, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802886
;

-- Dec 29, 2025, 3:08:24 PM CET
UPDATE AD_Field SET Name='AIG_IngestionMetadata_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:08:24','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802880
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='Language', Description='Language for this entity', Help='The Language identifies the language to use for display and formatting', IsDisplayed='Y', SeqNo=30, XPosition=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802876
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802877
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='Embedding', Description=NULL, Help=NULL, SeqNo=50, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802869
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='textsegment', Description=NULL, Help=NULL, SeqNo=60, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802870
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='textsegmenthash', Description=NULL, Help=NULL, SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802871
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='metadata', Description=NULL, Help=NULL, SeqNo=80, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802872
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='sourcetype', Description=NULL, Help=NULL, SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802873
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='sourceid', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=100, XPosition=4, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802874
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='sourcetable', Description=NULL, Help=NULL, SeqNo=110, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802875
;

-- Dec 29, 2025, 3:09:00 PM CET
UPDATE AD_Field SET Name='AIG_Embedding_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-29 15:09:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802868
;

