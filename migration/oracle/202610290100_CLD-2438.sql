-- CLD-2438 AI Assistant form: AD_Form, its translations and form access for the non-manual roles.
-- Written by hand: clde-hub has no AD_Form generator yet (CLD-2439). Hub conventions: the numeric id is the key across
-- environments, UUIDs are minted at import (generate_uuid()).
-- centralid: AD_Form 800030 (created in the dictionary for CLD-2438, Norbert Bede)
SELECT register_migration_script('202610290100_CLD-2438.sql') FROM dual
;

INSERT INTO AD_Form (AD_Form_ID,Name,Description,Help,Classname,AccessLevel,EntityType,IsBetaFunctionality,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Form_UU)
SELECT 800030,'AI Assistant','Ask the AI about your ERP data','The same AI chat as in the help panel, opened as a tab. Answers respect the tenant and role of the logged-in user.','com.cloudempiere.ai.form.AIChatForm','6','MM02','N',0,0,'Y',TO_TIMESTAMP('2026-10-29 01:00:00','YYYY-MM-DD HH24:MI:SS'),100,TO_TIMESTAMP('2026-10-29 01:00:00','YYYY-MM-DD HH24:MI:SS'),100,generate_uuid() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM AD_Form WHERE AD_Form_ID=800030)
;

-- The form may exist without its class (created in the dictionary first)
UPDATE AD_Form SET Name='AI Assistant', Description='Ask the AI about your ERP data', Classname='com.cloudempiere.ai.form.AIChatForm', Updated=SYSDATE, UpdatedBy=100
WHERE AD_Form_ID=800030 AND (Classname IS NULL OR Classname<>'com.cloudempiere.ai.form.AIChatForm')
;

-- Translation rows, as the application creates them on save (without them a non-base login language misses the form)
INSERT INTO AD_Form_Trl (AD_Language,AD_Form_ID,Name,Description,Help,IsTranslated,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Form_Trl_UU)
SELECT l.AD_Language,t.AD_Form_ID,t.Name,t.Description,t.Help,'N',t.AD_Client_ID,t.AD_Org_ID,'Y',SYSDATE,100,SYSDATE,100,generate_uuid()
FROM AD_Language l, AD_Form t
WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' AND t.AD_Form_ID=800030
AND NOT EXISTS (SELECT 1 FROM AD_Form_Trl tt WHERE tt.AD_Language=l.AD_Language AND tt.AD_Form_ID=t.AD_Form_ID)
;

-- Form access as "Role Access Update" grants it: every active non-manual role (access level 6, System+Client).
-- Manual roles get it from their administrator, as for any form. The help panel does not depend on it: it keeps the AI
-- provider access (AIG_Provider_Access) as its gate.
INSERT INTO AD_Form_Access (AD_Form_ID,AD_Role_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadWrite,AD_Form_Access_UU)
SELECT 800030,r.AD_Role_ID,r.AD_Client_ID,0,'Y',SYSDATE,100,SYSDATE,100,'Y',generate_uuid() FROM AD_Role r
WHERE r.IsManual='N' AND r.IsActive='Y'
AND NOT EXISTS (SELECT 1 FROM AD_Form_Access a WHERE a.AD_Form_ID=800030 AND a.AD_Role_ID=r.AD_Role_ID)
;
