-- CLD-2438 translation rows for the AI Assistant menu entry. Written by hand: generateMenuFromSpec inserts no
-- AD_Menu_Trl (CLD-2439), and without it the menu leaves the entry out for every login language that is not the base
-- language.
SELECT register_migration_script('202610290107_CLD-2438.sql') FROM dual
;

INSERT INTO AD_Menu_Trl (AD_Language,AD_Menu_ID,Name,Description,IsTranslated,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Menu_Trl_UU)
SELECT l.AD_Language,t.AD_Menu_ID,t.Name,t.Description,'N',t.AD_Client_ID,t.AD_Org_ID,'Y',statement_timestamp(),100,statement_timestamp(),100,generate_uuid()
FROM AD_Language l, AD_Menu t
WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' AND t.AD_Menu_ID=800226
AND NOT EXISTS (SELECT 1 FROM AD_Menu_Trl tt WHERE tt.AD_Language=l.AD_Language AND tt.AD_Menu_ID=t.AD_Menu_ID)
;
