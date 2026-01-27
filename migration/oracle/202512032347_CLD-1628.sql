-- CLD-1628
SELECT register_migration_script('202512032347_CLD-1628.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Dec 3, 2025, 11:47:28 PM CET
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure,IsHtml,IsPartitionKey) VALUES (803541,0,'Default','Default value','The Default Checkbox indicates if this record will be used as a default value.',800202,'IsDefault',1,'N','N','N','N','N',0,'N',20,0,0,'Y',TO_TIMESTAMP('2025-12-03 23:47:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-03 23:47:28','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),1103,'Y','N','MM02','N','N','N','Y','4ecef331-aadb-49a1-837b-b26533d6ebeb','Y',0,'N','N','N','N')
;

-- Dec 3, 2025, 11:47:37 PM CET
UPDATE AD_Column SET DefaultValue='N',Updated=TO_TIMESTAMP('2025-12-03 23:47:37','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803541
;

-- Dec 3, 2025, 11:47:44 PM CET
UPDATE AD_Column SET FKConstraintName='aduser_aigprovider', FKConstraintType='N',Updated=TO_TIMESTAMP('2025-12-03 23:47:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803458
;

-- Dec 3, 2025, 11:47:44 PM CET
ALTER TABLE AIG_Provider MODIFY AD_User_ID NUMBER(10) DEFAULT NULL 
;

-- Dec 3, 2025, 11:47:44 PM CET
ALTER TABLE AIG_Provider ADD IsDefault CHAR(1) DEFAULT 'N' CHECK (IsDefault IN ('Y','N'))
;

-- Dec 3, 2025, 11:48:17 PM CET
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,XPosition,ColumnSpan) VALUES (802851,'Default','Default value','The Default Checkbox indicates if this record will be used as a default value.',800210,803541,'Y',1,80,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2025-12-03 23:48:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2025-12-03 23:48:16','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'N','Y','MM02','a99d8548-dfcc-4cce-ba42-0ef8d12cb978','Y',70,2,2)
;

-- Dec 3, 2025, 11:48:27 PM CET
UPDATE AD_Field SET Name='Default', Description='Default value', Help='The Default Checkbox indicates if this record will be used as a default value.', IsDisplayed='Y', SeqNo=80, XPosition=5, Placeholder=NULL,Updated=TO_TIMESTAMP('2025-12-03 23:48:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Field_ID=802851
;

