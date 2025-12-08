-- CLD-1628
SELECT register_migration_script('202512081358_CLD-1628.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Dec 8, 2025, 1:58:45 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,FKConstraintType,IsHtml,IsPartitionKey) VALUES (803558,0,'Model Name',800202,'ModelName',100,'N','N','N','N','N',0,'N',10,0,0,'Y',TO_TIMESTAMP('2025-12-08 13:58:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-08 13:58:45','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),800716,'Y','Y','MM02','N','N','N','Y','26de8adb-42b1-4fd2-aee3-bcb080a41a68','Y',20,'N','N','N','N','N')
;

-- Dec 8, 2025, 1:58:46 PM CET
ALTER TABLE AIG_Provider ADD ModelName VARCHAR2(100 CHAR) DEFAULT NULL 
;

-- Dec 8, 2025, 1:59:18 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (802864,'Model Name',800210,803558,'Y',100,90,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-08 13:59:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-08 13:59:18','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','02bbba39-49e3-45b5-8671-4dae868813c6','Y',80,5)
;

-- Dec 8, 2025, 2:00:21 PM CET
UPDATE AD_Field SET Name='Model Name', Description=NULL, Help=NULL, IsDisplayed='Y', SeqNo=40, XPosition=4, ColumnSpan=2, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-08 14:00:21','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802864
;

-- Dec 8, 2025, 2:00:21 PM CET
UPDATE AD_Field SET Name='Default', Description='Default value', Help='The Default Checkbox indicates if this record will be used as a default value.', SeqNo=80, ColumnSpan=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-08 14:00:21','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802851
;

-- Dec 8, 2025, 2:00:21 PM CET
UPDATE AD_Field SET Name='Active', Description='The record is active in the system', Help='There are two methods of making records unavailable in the system: One is to delete the record, the other is to de-activate the record. A de-activated record is not available for selection, but available for reports.
There are two reasons for de-activating and not deleting records:
(1) The system requires the record for audit purposes.
(2) The record is referenced by other records. E.g., you cannot delete a Business Partner, if there are invoices for this partner record existing. You de-activate the Business Partner and prevent that this record is used for future entries.', IsDisplayed='Y', SeqNo=90, XPosition=6, ColumnSpan=1, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-08 14:00:21','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802760
;

