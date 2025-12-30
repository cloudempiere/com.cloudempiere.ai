-- CLD-1628
SELECT register_migration_script('202512041116_CLD-1628.sql') FROM dual;

-- Dec 4, 2025, 11:16:52 AM CET
INSERT INTO AD_Table (AD_Table_ID,Name,TableName,LoadSeq,AccessLevel,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsSecurityEnabled,IsDeleteable,IsHighVolume,IsView,EntityType,ImportTable,IsChangeLog,ReplicationType,CopyColumnsFromTable,IsCentrallyMaintained,AD_Table_UU,Processing,DatabaseViewDrop,CopyComponentsFromView,CreateWindowFromTable,IsShowInDrillOptions,IsPartition,CreatePartition) VALUES (800207,'AI Chat Ownership','AIG_ChatOwnership',0,'6',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:16:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:16:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','N','N','MM02','N','Y','L','N','Y','bb2456c3-6f1e-44bc-92c4-fb32445713db','N','N','N','N','N','N','N')
;

-- Dec 4, 2025, 11:16:52 AM CET
INSERT INTO AD_Sequence (Name,CurrentNext,IsAudited,StartNewYear,Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,AD_Sequence_ID,IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) VALUES ('AIG_ChatOwnership',1000000,'N','N','Table AIG_ChatOwnership','Y','Y',0,0,TO_TIMESTAMP('2025-12-04 11:16:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:16:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800266,'Y',1000000,1,200000,'dca5d790-5c71-4a38-a1bd-3157a5c2fada')
;

-- Dec 4, 2025, 11:16:52 AM CET
CREATE SEQUENCE AIG_CHATOWNERSHIP_SQ INCREMENT 1 MINVALUE 1000000 MAXVALUE 2147483647 START 1000000
;

-- Dec 4, 2025, 11:17:18 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800740,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_ChatOwnership_ID','AI Chat Ownership','AI Chat Ownership','MM02','fd038391-4636-4dd6-ad44-9f80cb9330e9')
;

-- Dec 4, 2025, 11:17:18 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803542,0.0,'AI Chat Ownership',800207,'AIG_ChatOwnership_ID',10,'Y','N','Y','N','N','N',13,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:17','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800740,'N','N','MM02','N','ca4656ba-27ee-4300-bbb7-43fccc101d3e','N')
;

-- Dec 4, 2025, 11:17:18 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800741,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'AIG_ChatOwnership_UU','AIG_ChatOwnership_UU','AIG_ChatOwnership_UU','MM02','c0b1c7f2-de8c-4eb2-8a8d-676bd3388c6a')
;

-- Dec 4, 2025, 11:17:18 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803543,0.0,'AIG_ChatOwnership_UU',800207,'AIG_ChatOwnership_UU',36,'N','N','N','N','N','N',10,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800741,'N','N','MM02','N','06b3e78b-a4a2-43c6-b2fc-7c22d6ddee96','N')
;

-- Dec 4, 2025, 11:17:18 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803544,0.0,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800207,129,'AD_Client_ID','@#AD_Client_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),102,'N','N','MM02','N','cf47c58f-0035-4c25-9cd5-f74ade28d43e','N','D')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,AD_Val_Rule_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803545,0.0,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800207,104,'AD_Org_ID','@#AD_Org_ID@',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),113,'N','N','MM02','N','42d30074-538f-4a32-82ec-63a93b8b5c99','N','D')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803546,0.0,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800207,'IsActive','Y',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),348,'Y','N','MM02','N','fc060a32-7ba3-496e-8da5-092258569626','N')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803547,0.0,'Created','Date this record was created','The Created field indicates the date that this record was created.',800207,'Created','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),245,'N','N','MM02','N','e2bf81e2-d304-4931-bf95-ca831015d658','N')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803548,0.0,'Created By','User who created this records','The Created By field indicates the user who created this record.',800207,'CreatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),246,'N','N','MM02','N','9904199e-a0d2-4676-8f1c-07383eabf554','N','D')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,DefaultValue,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803549,0.0,'Updated','Date this record was updated','The Updated field indicates the date that this record was updated.',800207,'Updated','SYSDATE',29,'N','N','Y','N','N','N',16,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),607,'N','N','MM02','N','b6549d50-4a68-49ca-9970-0b92fc360222','N')
;

-- Dec 4, 2025, 11:17:19 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Reference_Value_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803550,0.0,'Updated By','User who updated this records','The Updated By field indicates the user who updated this record.',800207,'UpdatedBy',10,'N','N','Y','N','N','N',30,110,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),608,'N','N','MM02','N','fedf7b7d-0473-4362-8757-1db93fd1af44','N','D')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803551,0.0,'Chat','Chat or discussion thread','Thread of discussion',800207,'CM_Chat_ID',10,'N','N','Y','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:19','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),3045,'N','N','MM02','N','e04b9b3e-9c9d-4bdc-a360-0f89e15384c6','N','C')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803552,0.0,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800207,'AD_User_ID',10,'N','N','N','N','N','N',30,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),138,'N','N','MM02','N','eb66460e-4b49-4551-a52e-5302af13efac','N','N')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803553,0.0,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800207,'AD_Role_ID',10,'N','N','N','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),123,'N','N','MM02','N','63c0f982-d262-481b-8d84-c477ceff382b','N','C')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800742,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'ownershiptype','ownershiptype','ownershiptype','MM02','5747310e-ff97-40cf-aebb-827a6b0352d3')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803554,0.0,'ownershiptype',800207,'ownershiptype',1,'N','N','Y','N','N','N',20,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800742,'Y','N','MM02','N','4d11380d-fffe-48ff-aa9e-4745ee12e804','N')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800743,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'sharedby_user_id','sharedby_user_id','sharedby_user_id','MM02','61d576ec-692a-47ba-a127-161971b37cf7')
;

-- Dec 4, 2025, 11:17:20 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803555,0.0,'sharedby_user_id',800207,'sharedby_user_id',10,'N','N','N','N','N','N',19,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800743,'Y','N','MM02','N','934ad4b5-1a56-41a0-877c-f0a40f90dc02','N')
;

-- Dec 4, 2025, 11:17:21 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton,FKConstraintType) VALUES (803556,0.0,'Valid from','Valid from including this date (first day)','The Valid From date indicates the first day of a date range',800207,'ValidFrom',29,'N','N','N','N','N','N',15,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),617,'N','N','MM02','N','b75c46fb-d25f-407f-90bb-65d34c0b69a7','N','N')
;

-- Dec 4, 2025, 11:17:21 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsAlwaysUpdateable,AD_Column_UU,IsToolbarButton) VALUES (803557,0.0,'Valid to','Valid to including this date (last day)','The Valid To date indicates the last day of a date range',800207,'ValidTo',29,'N','N','N','N','N','N',15,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:17:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:17:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),618,'Y','N','MM02','N','03aa93f0-a45b-449a-8ab7-36c517dc9373','N')
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_Element SET ColumnName='OwnershipType', Name='Ownership Type', PrintName='Ownership Type',Updated=TO_TIMESTAMP('2025-12-04 11:18:11','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800742
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_Column SET ColumnName='OwnershipType', Name='Ownership Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800742
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_Process_Para SET ColumnName='OwnershipType', Name='Ownership Type', Description=NULL, Help=NULL, AD_Element_ID=800742 WHERE UPPER(ColumnName)='OWNERSHIPTYPE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_Process_Para SET ColumnName='OwnershipType', Name='Ownership Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800742 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_InfoColumn SET ColumnName='OwnershipType', Name='Ownership Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800742 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_Field SET Name='Ownership Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800742) AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:11 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Ownership Type', Name='Ownership Type' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800742)
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_Element SET ColumnName='SharedBy_User_ID', Name='Sharedby User', PrintName='Sharedby User',Updated=TO_TIMESTAMP('2025-12-04 11:18:49','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800743
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_Column SET ColumnName='SharedBy_User_ID', Name='Sharedby User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_Process_Para SET ColumnName='SharedBy_User_ID', Name='Sharedby User', Description=NULL, Help=NULL, AD_Element_ID=800743 WHERE UPPER(ColumnName)='SHAREDBY_USER_ID' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_Process_Para SET ColumnName='SharedBy_User_ID', Name='Sharedby User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_InfoColumn SET ColumnName='SharedBy_User_ID', Name='Sharedby User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_Field SET Name='Sharedby User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800743) AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:18:49 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Sharedby User', Name='Sharedby User' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800743)
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_Element SET Name='Shared by User', PrintName='Shared by User',Updated=TO_TIMESTAMP('2025-12-04 11:19:11','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800743
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_Column SET ColumnName='SharedBy_User_ID', Name='Shared by User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_Process_Para SET ColumnName='SharedBy_User_ID', Name='Shared by User', Description=NULL, Help=NULL, AD_Element_ID=800743 WHERE UPPER(ColumnName)='SHAREDBY_USER_ID' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_Process_Para SET ColumnName='SharedBy_User_ID', Name='Shared by User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_InfoColumn SET ColumnName='SharedBy_User_ID', Name='Shared by User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800743 AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_Field SET Name='Shared by User', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800743) AND IsCentrallyMaintained='Y'
;

-- Dec 4, 2025, 11:19:11 AM CET
UPDATE AD_PrintFormatItem SET PrintName='Shared by User', Name='Shared by User' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800743)
;

-- Dec 4, 2025, 11:19:39 AM CET
UPDATE AD_Column SET AD_Reference_ID=18, AD_Reference_Value_ID=110,Updated=TO_TIMESTAMP('2025-12-04 11:19:39','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803555
;

-- Dec 4, 2025, 11:20:22 AM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800126,'OwnershipType','L',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:20:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:20:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','00c13959-e765-4715-9ccc-328476669c56','N')
;

-- Dec 4, 2025, 11:20:42 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800296,'Owner',800126,'O',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:20:42','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:20:42','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','6919ceb0-e272-4b03-87dd-08aee979935c')
;

-- Dec 4, 2025, 11:20:52 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800297,'Read',800126,'R',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:20:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:20:52','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','f018cefa-a1ca-4061-8abf-54d2c3c0c3cb')
;

-- Dec 4, 2025, 11:21:02 AM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800298,'Write',800126,'W',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:21:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:21:01','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','ef458f25-72f2-4146-9960-f0dd99180574')
;

-- Dec 4, 2025, 11:21:32 AM CET
UPDATE AD_Column SET DefaultValue='R', AD_Reference_ID=17, AD_Reference_Value_ID=800126, FKConstraintType=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:21:32','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803554
;

-- Dec 4, 2025, 11:30:20 AM CET
UPDATE AD_Column SET IsParent='Y', IsUpdateable='N',Updated=TO_TIMESTAMP('2025-12-04 11:30:20','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803551
;

-- Dec 4, 2025, 11:30:39 AM CET
INSERT INTO AD_Tab (AD_Tab_ID,Name,AD_Window_ID,SeqNo,IsSingleRow,AD_Table_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,HasTree,IsTranslationTab,IsReadOnly,OrderByClause,Processing,TabLevel,IsSortTab,EntityType,IsInsertRecord,IsAdvancedTab,AD_Tab_UU) VALUES (800216,'AI Chat Ownership',377,40,'Y',800207,0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','N','N','AIG_ChatOwnership.Created DESC','N',1,'N','D','Y','N','e4e71aa9-e0e5-43d9-a8e9-e5b147bbbee6')
;

-- Dec 4, 2025, 11:30:39 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802852,'Tenant','Tenant for this installation.','A Tenant is a company or a legal entity. You cannot share data between Tenants.',800216,803544,'Y',10,10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'Y','Y','MM02','10dd3f99-9beb-464c-acb3-a616e08f880d','Y',10,2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsAllowCopy,IsDisplayedGrid,XPosition,ColumnSpan) VALUES (802853,'Organization','Organizational entity within tenant','An organization is a unit of your tenant or legal entity - examples are store, department. You can share data between organizations.',800216,803545,'Y',10,20,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:39','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','0e69e50d-ef7a-4307-b424-1d0db4174e63','Y','N',4,2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802854,'Chat','Chat or discussion thread','Thread of discussion',800216,803551,'Y',10,30,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','3031a5f2-3bd6-4e90-af49-0bafb3f75ce9','Y',20,2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802855,'AI Chat Ownership',800216,803542,'N',10,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5989ea0a-a42d-459f-99ef-9b543aeefe11','N',2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,ColumnSpan) VALUES (802856,'AIG_ChatOwnership_UU',800216,803543,'N',36,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','45a05f2a-431c-4243-81e8-cc559f113f0e','N',2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802857,'User/Contact','User within the system - Internal or Business Partner Contact','The User identifies a unique user in the system. This could be an internal user or a business partner contact',800216,803552,'Y',10,40,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','05d3130d-bce8-4ca1-9ac5-1ef87282eb4b','Y',30,2)
;

-- Dec 4, 2025, 11:30:40 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802858,'Role','Responsibility Role','The Role determines security and access a user who has this Role will have in the System.',800216,803553,'Y',10,50,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','7b48f366-1a2b-4d9a-91e5-2e439d1bab71','Y',40,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802859,'Ownership Type',800216,803554,'Y',1,60,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:40','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','d8819bfb-ba43-4a04-8183-800366ce4d31','Y',50,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802860,'Shared by User',800216,803555,'Y',10,70,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','2b1729b7-fa64-447f-8b54-36ac377741fb','Y',60,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802861,'Valid from','Valid from including this date (first day)','The Valid From date indicates the first day of a date range',800216,803556,'Y',29,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','5924abb5-44f4-41e4-9ba0-afbfa65dc223','Y',70,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802862,'Valid to','Valid to including this date (last day)','The Valid To date indicates the last day of a date range',800216,803557,'Y',29,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','ff3a3d51-2ee9-4034-8bfa-40fca036ec45','Y',80,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802863,'Active','The record is active in the system','There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.',800216,803546,'Y',1,100,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','06096663-158b-4a53-a3f4-04e1b3820c38','Y',90,2,2)
;

-- Dec 4, 2025, 11:30:41 AM CET
UPDATE AD_Table SET AD_Window_ID=377,Updated=TO_TIMESTAMP('2025-12-04 11:30:41','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Table_ID=800207
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Chat', Description='Chat or discussion thread', Help='Thread of discussion', SeqNo=30, IsReadOnly='Y', Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802854
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=40, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802863
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', IsDisplayed='Y', SeqNo=60, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802857
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Ownership Type', Description=NULL, Help=NULL, SeqNo=70, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802859
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Shared by User', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=80, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802860
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Valid from', Description='Valid from including this date (first day)', Help='The Valid From date indicates the first day of a date range', SeqNo=90, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802861
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='Valid to', Description='Valid to including this date (last day)', Help='The Valid To date indicates the last day of a date range', IsDisplayed='Y', SeqNo=100, XPosition=4, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802862
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='AI Chat Ownership', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802855
;

-- Dec 4, 2025, 11:31:18 AM CET
UPDATE AD_Field SET Name='AIG_ChatOwnership_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-04 11:31:18','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802856
;

