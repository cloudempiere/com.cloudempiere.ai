-- CLD-1645
SELECT register_migration_script('202512111627_CLD-1645.sql') FROM dual;

-- Dec 11, 2025, 4:27:53 PM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800208,'AI Embedding','AIG_Embedding',0,'6',0,0,'Y',TO_TIMESTAMP('2025-12-11 16:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','94fbb1d3-326d-482e-83dd-7884308d59be','N','N','N','N','N','N','N')
;

-- Dec 11, 2025, 4:27:53 PM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_Embedding',1000000,'N','N','Table AIG_Embedding','Y','Y',0,0,TO_TIMESTAMP('2025-12-11 16:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:27:53','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800612,'Y',1000000,1,200000,'3c491d4d-e1d6-4add-ae04-9599b7143edd')
;

-- Dec 11, 2025, 4:27:53 PM CET
CREATE SEQUENCE AIG_EMBEDDING_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Dec 11, 2025, 4:28:16 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800744,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_Embedding_UU','AIG_Embedding_UU','AIG_Embedding_UU','MM02','cf9091e8-399a-4516-86fb-b3b40111193d')
;

-- Dec 11, 2025, 4:28:16 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803560,0.0,'AIG_Embedding_UU',800208,'AIG_Embedding_UU',2147483647,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800744,'N','N','MM02','N','f893cf3b-95eb-4025-b58a-0f6da16b9c77','N')
;

-- Dec 11, 2025, 4:28:16 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803561,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800208,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','eb9d84e2-fd8c-485e-ba40-6f27042119e1','N','D')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803562,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800208,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','3e5a1da2-ff19-4d3f-9f39-1dae8fa4e544','N','D')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803563,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800208,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','6e028943-6cf1-428c-a46a-65737fc349cd','N')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803564,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800208,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','981d08a4-00d5-4308-b9b4-698e7bc2aeb5','N')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803565,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800208,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','b1edbddb-dbe0-4bc9-91fc-7361f030b2de','N','D')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803566,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800208,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','0b157d90-064d-4781-aebf-d829a1fd7f52','N')
;

-- Dec 11, 2025, 4:28:17 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803567,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800208,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','cb9ae0b1-e07b-4d90-b969-e00ba0896b26','N','D')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800745,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'embedding','embedding','embedding','MM02','67ef3529-37bc-41a2-8614-44ed973017f2')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803568,0.0,'embedding',800208,'embedding',2147483647,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800745,'Y','N','MM02','N','76f701b5-028f-4748-bf47-532e07417b87','N')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800746,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'textsegment','textsegment','textsegment','MM02','938c6b44-cc58-4cab-8654-6279da9ab66c')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803569,0.0,'textsegment',800208,'textsegment',2147483647,'N','N','Y','N','N','N',14,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800746,'Y','N','MM02','N','621b0f5a-b223-483d-8a49-a5937d740d41','N')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800747,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'textsegmenthash','textsegmenthash','textsegmenthash','MM02','0536a658-ad12-4674-84c5-a9d3d0fb2307')
;

-- Dec 11, 2025, 4:28:18 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803570,0.0,'textsegmenthash',800208,'textsegmenthash',64,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800747,'Y','N','MM02','N','01cc1dd2-5016-46dd-86a6-3ed5ca27665c','N')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800748,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'metadata','metadata','metadata','MM02','88efc7dd-cf6e-4210-b21d-7991deb08264')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803571,0.0,'metadata',800208,'metadata',2147483647,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800748,'Y','N','MM02','N','53327297-a145-4a45-b03d-a4a44f3b403a','N')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800749,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'sourcetype','sourcetype','sourcetype','MM02','c1b3d203-c842-4b42-bfa6-f4e0b3d53546')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803572,0.0,'sourcetype',800208,'sourcetype',50,'N','N','Y','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800749,'Y','N','MM02','N','56792d41-18ab-41a8-9e99-36c92eae6a0e','N')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800750,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'sourceid','sourceid','sourceid','MM02','6520cee3-ca95-409e-8dab-27748b881a28')
;

-- Dec 11, 2025, 4:28:19 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803573,0.0,'sourceid',800208,'sourceid',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800750,'Y','N','MM02','N','119fdafb-7a92-4ca2-bb65-a10fa05f8ff1','N')
;

-- Dec 11, 2025, 4:28:20 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800751,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'sourcetable','sourcetable','sourcetable','MM02','522d4247-c765-46e7-b5ce-c8e14162b099')
;

-- Dec 11, 2025, 4:28:20 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803574,0.0,'sourcetable',800208,'sourcetable',100,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800751,'Y','N','MM02','N','10658d5e-11f2-459f-96c4-3f16c58351e0','N')
;

-- Dec 11, 2025, 4:28:20 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803575,0.0,'Language','Language for this entity','The Language identifies the language to use for display and formatting',800208,'AD_Language',6,'N','N','N','N','N','N',18,106,0,0,'Y',TO_TIMESTAMP('2025-12-11 16:28:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-11 16:28:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),109,'N','N','MM02','N','e2a10a65-c12c-45e3-bd91-3690605c73d0','N','N')
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_Element SET ColumnName='Embedding', Name='Embedding', PrintName='Embedding',Updated=TO_TIMESTAMP('2025-12-11 16:29:22','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800745
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_Column SET ColumnName='Embedding', Name='Embedding', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800745
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_Process_Para SET ColumnName='Embedding', Name='Embedding', Description=NULL, Help=NULL, AD_Element_ID=800745 WHERE UPPER(ColumnName)='EMBEDDING' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_Process_Para SET ColumnName='Embedding', Name='Embedding', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800745 AND IsCentrallyMaintained='Y'
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_InfoColumn SET ColumnName='Embedding', Name='Embedding', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800745 AND IsCentrallyMaintained='Y'
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_Field SET Name='Embedding', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800745) AND IsCentrallyMaintained='Y'
;

-- Dec 11, 2025, 4:29:22 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Embedding', Name='Embedding' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800745)
;

