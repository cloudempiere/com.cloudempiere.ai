-- CLD-1704
SELECT register_migration_script('202602051111_CLD-1704.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Feb 5, 2026, 11:11:21 AM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (nextidfunc(3,'N'),0,'Content HTML','Contains the content itself','Contains the content itself as HTML code. Should normally only use basic tags, no real layouting',877,'ContentHTML',400000,'N','N','N','N','N',0,'N',14,0,0,'Y',TO_TIMESTAMP('2026-02-05 11:11:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-05 11:11:21','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),3006,'N','N','MM02','N','N','N','Y','ab6d28f6-b4d8-45a6-8f0d-bfdad952e3ba','Y',0,'N','N','N','N')
;

-- Feb 5, 2026, 11:11:31 AM CET
ALTER TABLE CM_ChatEntry ADD ContentHTML CLOB DEFAULT NULL 
;

-- Feb 5, 2026, 11:11:56 AM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan,NumLines) VALUES (nextidfunc(4,'N'),'Content HTML','Contains the content itself','Contains the content itself as HTML code. Should normally only use basic tags, no real layouting',822,toRecordId('AD_Column','ab6d28f6-b4d8-45a6-8f0d-bfdad952e3ba'),'Y',400000,130,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2026-02-05 11:11:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-05 11:11:56','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','8041ff06-b6f5-4a6a-a550-2656a9ac5511','Y',130,5,3)
;

-- Feb 5, 2026, 11:12:13 AM CET
UPDATE AD_Field SET Name='Content HTML', Description='Contains the content itself', Help='Contains the content itself as HTML code. Should normally only use basic tags, no real layouting', IsDisplayed='Y', SeqNo=120, XPosition=1, NumLines=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-05 11:12:13','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_UU='8041ff06-b6f5-4a6a-a550-2656a9ac5511'
;

-- Feb 5, 2026, 11:12:13 AM CET
UPDATE AD_Field SET Name='User/Contact', Description='User within the system - Internal or Business Partner Contact', Help='The User identifies a unique user in the system. This could be an internal user or a business partner contact', SeqNo=130, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-05 11:12:13','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=13763
;

-- Feb 5, 2026, 11:12:13 AM CET
UPDATE AD_Field SET Name='CM_ChatEntry_UU', Description=NULL, Help=NULL, SeqNo=0, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-05 11:12:13','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=204752
;

