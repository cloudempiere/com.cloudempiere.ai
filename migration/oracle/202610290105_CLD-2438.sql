-- Migration script: 202610290105_CLD-2438.sql
-- Menu from spec: AI Assistant
-- Generated: 2026-10-29 00:03:29
-- Generator: CLDE Hub MCP (generateMenuFromSpec)
-- Changed after generation (generator gaps, CLD-2439): the placeholder id 1000000 is the centralid-committed AD_Menu
-- 800226 (CLD-2438, Norbert Bede); NOT EXISTS guards, so the script also applies where the rows exist; the node goes
-- only into menu trees that contain the parent folder 800128 (not into every MM tree of client 0); file name later
-- than the form script, the menu points to AD_Form 800030.

SELECT register_migration_script('202610290105_CLD-2438.sql') FROM dual;

-- GENERATED FROM SPECIFICATION (no database lookup)
-- Menu: AI Assistant
-- Action: X
-- Action ID: 800030

-- AD_Menu
INSERT INTO AD_Menu (AD_Menu_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, IsSummary, IsSOTrx, IsReadOnly, Action,
    AD_Window_ID, AD_Process_ID, AD_Form_ID, AD_Workflow_ID, EntityType, AD_Menu_UU)
SELECT 800226, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
    'AI Assistant', 'Ask the AI about your ERP data', 'N', 'Y', 'N', 'X',
    NULL, NULL, 800030, NULL, 'MM02', generate_uuid() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM AD_Menu WHERE AD_Menu_ID=800226);

-- AD_TreeNodeMM (Menu Tree)
INSERT INTO AD_TreeNodeMM (AD_Tree_ID, Node_ID, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, Parent_ID, SeqNo)
SELECT t.AD_Tree_ID, 800226, 0, 0, 'Y',
    CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100, 800128, 8
FROM AD_Tree t WHERE t.TreeType = 'MM' AND t.AD_Client_ID = 0
AND EXISTS (SELECT 1 FROM AD_TreeNodeMM p WHERE p.AD_Tree_ID=t.AD_Tree_ID AND p.Node_ID=800128)
AND NOT EXISTS (SELECT 1 FROM AD_TreeNodeMM e WHERE e.AD_Tree_ID=t.AD_Tree_ID AND e.Node_ID=800226);

