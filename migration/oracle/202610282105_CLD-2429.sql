-- CLD-2429 Anchor global AI chats on the user's AD_UserPreference of the tenant
-- See the PostgreSQL script for the reason. Plain statements: safe to run again.
SELECT register_migration_script('202610282105_CLD-2429.sql') FROM dual
;

-- 1. A user preference for every user and tenant that has a global AI chat on the former AD_User anchor
INSERT INTO AD_UserPreference (AD_UserPreference_ID, AD_Client_ID, AD_Org_ID, AD_User_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, AD_UserPreference_UU)
SELECT nextidfunc((SELECT AD_Sequence_ID FROM AD_Sequence WHERE Name='AD_UserPreference' AND IsTableID='Y' AND AD_Client_ID=0), 'N'),
    c.AD_Client_ID, 0, c.Record_ID, 'Y', SYSDATE, 100, SYSDATE, 100, generate_uuid()
FROM (SELECT DISTINCT ch.AD_Client_ID, ch.Record_ID FROM CM_Chat ch
      WHERE ch.AD_Table_ID=(SELECT AD_Table_ID FROM AD_Table WHERE TableName='AD_User') AND ch.Record_ID IS NOT NULL) c
WHERE EXISTS (SELECT 1 FROM AD_User u WHERE u.AD_User_ID=c.Record_ID)
  AND NOT EXISTS (SELECT 1 FROM AD_UserPreference p WHERE p.AD_User_ID=c.Record_ID AND p.AD_Client_ID=c.AD_Client_ID)
;

-- 2. Move the global AI chats to that preference; Record_UU becomes the preference's UUID
UPDATE CM_Chat c SET (AD_Table_ID, Record_ID, Record_UU, Updated, UpdatedBy) = (
    SELECT (SELECT AD_Table_ID FROM AD_Table WHERE TableName='AD_UserPreference'),
        p.AD_UserPreference_ID, p.AD_UserPreference_UU, SYSDATE, 100
    FROM AD_UserPreference p WHERE p.AD_User_ID=c.Record_ID AND p.AD_Client_ID=c.AD_Client_ID)
WHERE c.AD_Table_ID=(SELECT AD_Table_ID FROM AD_Table WHERE TableName='AD_User')
  AND EXISTS (SELECT 1 FROM AD_UserPreference p WHERE p.AD_User_ID=c.Record_ID AND p.AD_Client_ID=c.AD_Client_ID)
;
