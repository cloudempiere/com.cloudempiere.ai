-- CLD-1628
SELECT register_migration_script('202602271440_CLD-1628.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_Element SET ColumnName='AgentName', Name='Agent Name', PrintName='Agent Name',Updated=TO_TIMESTAMP('2026-02-27 14:40:09','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800839
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_Column SET ColumnName='AgentName', Name='Agent Name', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800839
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_Process_Para SET ColumnName='AgentName', Name='Agent Name', Description=NULL, Help=NULL, AD_Element_ID=800839 WHERE UPPER(ColumnName)='AGENTNAME' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_Process_Para SET ColumnName='AgentName', Name='Agent Name', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800839 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_InfoColumn SET ColumnName='AgentName', Name='Agent Name', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800839 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_Field SET Name='Agent Name', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800839) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:09 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Agent Name', Name='Agent Name' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800839)
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_Element SET ColumnName='AgentType', Name='Agent Type', PrintName='Agent Type',Updated=TO_TIMESTAMP('2026-02-27 14:40:19','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800840
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_Column SET ColumnName='AgentType', Name='Agent Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800840
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_Process_Para SET ColumnName='AgentType', Name='Agent Type', Description=NULL, Help=NULL, AD_Element_ID=800840 WHERE UPPER(ColumnName)='AGENTTYPE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_Process_Para SET ColumnName='AgentType', Name='Agent Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800840 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_InfoColumn SET ColumnName='AgentType', Name='Agent Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800840 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_Field SET Name='Agent Type', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800840) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:19 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Agent Type', Name='Agent Type' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800840)
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_Element SET ColumnName='BudgetScope', Name='Budget Scope', PrintName='Budget Scope',Updated=TO_TIMESTAMP('2026-02-27 14:40:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800852
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_Column SET ColumnName='BudgetScope', Name='Budget Scope', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800852
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_Process_Para SET ColumnName='BudgetScope', Name='Budget Scope', Description=NULL, Help=NULL, AD_Element_ID=800852 WHERE UPPER(ColumnName)='BUDGETSCOPE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_Process_Para SET ColumnName='BudgetScope', Name='Budget Scope', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800852 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_InfoColumn SET ColumnName='BudgetScope', Name='Budget Scope', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800852 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_Field SET Name='Budget Scope', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800852) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:40:36 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Budget Scope', Name='Budget Scope' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800852)
;

-- Feb 27, 2026, 2:43:31 PM CET
ALTER TABLE AIG_UsageMetrics RENAME COLUMN costusd TO CostAmt
;

-- Feb 27, 2026, 2:43:31 PM CET
UPDATE AD_Column SET Name='Cost Value', Description='Value with Cost', Help=NULL, ColumnName='CostAmt', AD_Element_ID=2962, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:43:31','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803951
;

-- Feb 27, 2026, 2:43:56 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='714f1530-6b28-404a-8f1d-9e7b6717d547'
;

-- Feb 27, 2026, 2:44:43 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800865,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:44:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:44:43','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'CurrentDailyAmt','Current Daily Amt','Current Daily Amt','MM02','789a8a9a-7459-409a-ba2c-99a9a01b52f8')
;

-- Feb 27, 2026, 2:44:58 PM CET
ALTER TABLE AIG_Budget RENAME COLUMN currentdailyusd TO CurrentDailyAmt
;

-- Feb 27, 2026, 2:44:58 PM CET
UPDATE AD_Column SET Name='Current Daily Amt', Description=NULL, Help=NULL, ColumnName='CurrentDailyAmt', AD_Element_ID=800865, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:44:58','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803974
;

-- Feb 27, 2026, 2:45:13 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='d0da1ffc-c1e2-455e-b6d9-c057c7533048'
;

-- Feb 27, 2026, 2:45:34 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800866,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:45:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:45:34','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'CurrentMonthlyAmt','Current Monthly Amt','Current Monthly Amt','MM02','69d060af-b05e-4787-b329-03d4c165f894')
;

-- Feb 27, 2026, 2:45:45 PM CET
ALTER TABLE AIG_Budget RENAME COLUMN currentmonthlyusd TO CurrentMonthlyAmt
;

-- Feb 27, 2026, 2:45:45 PM CET
UPDATE AD_Column SET Name='Current Monthly Amt', Description=NULL, Help=NULL, ColumnName='CurrentMonthlyAmt', AD_Element_ID=800866, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:45:45','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803975
;

-- Feb 27, 2026, 2:45:53 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='630bcf70-0744-4993-919d-d9e203862009'
;

-- Feb 27, 2026, 2:46:23 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800867,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:46:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:46:23','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'DailyLimit','Daily Limit','Daily Limit','MM02','70370e50-cf00-4bab-b34f-811a3df30a8d')
;

-- Feb 27, 2026, 2:46:40 PM CET
ALTER TABLE AIG_Budget RENAME COLUMN dailylimitusd TO DailyLimit
;

-- Feb 27, 2026, 2:46:40 PM CET
UPDATE AD_Column SET Name='Daily Limit', Description=NULL, Help=NULL, ColumnName='DailyLimit', AD_Element_ID=800867, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:46:40','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803970
;

-- Feb 27, 2026, 2:46:48 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='1a2a4b43-3e1a-4375-aab2-f07d1a5be388'
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_Element SET ColumnName='InputTokens', Name='Input Tokens', PrintName='Input Tokens',Updated=TO_TIMESTAMP('2026-02-27 14:47:07','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800842
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_Column SET ColumnName='InputTokens', Name='Input Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800842
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_Process_Para SET ColumnName='InputTokens', Name='Input Tokens', Description=NULL, Help=NULL, AD_Element_ID=800842 WHERE UPPER(ColumnName)='INPUTTOKENS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_Process_Para SET ColumnName='InputTokens', Name='Input Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800842 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_InfoColumn SET ColumnName='InputTokens', Name='Input Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800842 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_Field SET Name='Input Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800842) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:07 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Input Tokens', Name='Input Tokens' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800842)
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_Element SET ColumnName='LastResetDaily', Name='Last Reset Daily', PrintName='Last Reset Daily',Updated=TO_TIMESTAMP('2026-02-27 14:47:26','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800859
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_Column SET ColumnName='LastResetDaily', Name='Last Reset Daily', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800859
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_Process_Para SET ColumnName='LastResetDaily', Name='Last Reset Daily', Description=NULL, Help=NULL, AD_Element_ID=800859 WHERE UPPER(ColumnName)='LASTRESETDAILY' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_Process_Para SET ColumnName='LastResetDaily', Name='Last Reset Daily', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800859 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_InfoColumn SET ColumnName='LastResetDaily', Name='Last Reset Daily', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800859 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_Field SET Name='Last Reset Daily', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800859) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:26 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Last Reset Daily', Name='Last Reset Daily' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800859)
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_Element SET ColumnName='LastResetMonthly', Name='Last Reset Monthly', PrintName='Last Reset Monthly',Updated=TO_TIMESTAMP('2026-02-27 14:47:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800860
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_Column SET ColumnName='LastResetMonthly', Name='Last Reset Monthly', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800860
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_Process_Para SET ColumnName='LastResetMonthly', Name='Last Reset Monthly', Description=NULL, Help=NULL, AD_Element_ID=800860 WHERE UPPER(ColumnName)='LASTRESETMONTHLY' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_Process_Para SET ColumnName='LastResetMonthly', Name='Last Reset Monthly', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800860 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_InfoColumn SET ColumnName='LastResetMonthly', Name='Last Reset Monthly', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800860 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_Field SET Name='Last Reset Monthly', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800860) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:44 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Last Reset Monthly', Name='Last Reset Monthly' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800860)
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_Element SET ColumnName='LatencyMs', Name='Latency ms', PrintName='Latency ms',Updated=TO_TIMESTAMP('2026-02-27 14:47:59','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800846
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_Column SET ColumnName='LatencyMs', Name='Latency ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800846
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_Process_Para SET ColumnName='LatencyMs', Name='Latency ms', Description=NULL, Help=NULL, AD_Element_ID=800846 WHERE UPPER(ColumnName)='LATENCYMS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_Process_Para SET ColumnName='LatencyMs', Name='Latency ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800846 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_InfoColumn SET ColumnName='LatencyMs', Name='Latency ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800846 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_Field SET Name='Latency ms', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800846) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:47:59 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Latency ms', Name='Latency ms' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800846)
;

-- Feb 27, 2026, 2:48:21 PM CET
INSERT INTO AD_Element (AD_Element_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,ColumnName,Name,PrintName,EntityType,AD_Element_UU) VALUES (800868,0,0,'Y',TO_TIMESTAMP('2026-02-27 14:48:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-02-27 14:48:20','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MonthlyLimit','Monthly Limit','Monthly Limit','MM02','0e41e228-3272-4454-ab19-b12cddcd78bb')
;

-- Feb 27, 2026, 2:48:35 PM CET
ALTER TABLE AIG_Budget RENAME COLUMN monthlylimitusd TO MonthlyLimit
;

-- Feb 27, 2026, 2:48:35 PM CET
UPDATE AD_Column SET Name='Monthly Limit', Description=NULL, Help=NULL, ColumnName='MonthlyLimit', AD_Element_ID=800868, Placeholder=NULL,Updated=TO_TIMESTAMP('2026-02-27 14:48:35','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803971
;

-- Feb 27, 2026, 2:48:52 PM CET
DELETE FROM AD_Element WHERE AD_Element_UU='785a29d2-730a-4af6-aa5c-42d3ef763742'
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_Element SET ColumnName='OutputTokens', Name='Output Tokens', PrintName='Output Tokens',Updated=TO_TIMESTAMP('2026-02-27 14:49:11','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800843
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_Column SET ColumnName='OutputTokens', Name='Output Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800843
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_Process_Para SET ColumnName='OutputTokens', Name='Output Tokens', Description=NULL, Help=NULL, AD_Element_ID=800843 WHERE UPPER(ColumnName)='OUTPUTTOKENS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_Process_Para SET ColumnName='OutputTokens', Name='Output Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800843 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_InfoColumn SET ColumnName='OutputTokens', Name='Output Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800843 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_Field SET Name='Output Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800843) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:11 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Output Tokens', Name='Output Tokens' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800843)
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_Element SET ColumnName='RequestHash', Name='Request Hash', PrintName='Request Hash',Updated=TO_TIMESTAMP('2026-02-27 14:49:19','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800848
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_Column SET ColumnName='RequestHash', Name='Request Hash', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800848
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestHash', Name='Request Hash', Description=NULL, Help=NULL, AD_Element_ID=800848 WHERE UPPER(ColumnName)='REQUESTHASH' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestHash', Name='Request Hash', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800848 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_InfoColumn SET ColumnName='RequestHash', Name='Request Hash', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800848 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_Field SET Name='Request Hash', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800848) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:19 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Request Hash', Name='Request Hash' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800848)
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_Element SET ColumnName='RequestsPerMinute', Name='Requests/Minute', PrintName='Requests/Minute',Updated=TO_TIMESTAMP('2026-02-27 14:49:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800856
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_Column SET ColumnName='RequestsPerMinute', Name='Requests/Minute', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800856
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestsPerMinute', Name='Requests/Minute', Description=NULL, Help=NULL, AD_Element_ID=800856 WHERE UPPER(ColumnName)='REQUESTSPERMINUTE' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestsPerMinute', Name='Requests/Minute', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800856 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_InfoColumn SET ColumnName='RequestsPerMinute', Name='Requests/Minute', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800856 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_Field SET Name='Requests/Minute', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800856) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:44 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Requests/Minute', Name='Requests/Minute' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800856)
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_Element SET ColumnName='RequestTimestamp', Name='Request Timestamp', PrintName='Request Timestamp',Updated=TO_TIMESTAMP('2026-02-27 14:49:54','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800847
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_Column SET ColumnName='RequestTimestamp', Name='Request Timestamp', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800847
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestTimestamp', Name='Request Timestamp', Description=NULL, Help=NULL, AD_Element_ID=800847 WHERE UPPER(ColumnName)='REQUESTTIMESTAMP' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_Process_Para SET ColumnName='RequestTimestamp', Name='Request Timestamp', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800847 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_InfoColumn SET ColumnName='RequestTimestamp', Name='Request Timestamp', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800847 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_Field SET Name='Request Timestamp', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800847) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:49:54 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Request Timestamp', Name='Request Timestamp' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800847)
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_Element SET ColumnName='SessionID', Name='Session ID', PrintName='Session ID',Updated=TO_TIMESTAMP('2026-02-27 14:50:04','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800841
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_Column SET ColumnName='SessionID', Name='Session ID', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800841
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_Process_Para SET ColumnName='SessionID', Name='Session ID', Description=NULL, Help=NULL, AD_Element_ID=800841 WHERE UPPER(ColumnName)='SESSIONID' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_Process_Para SET ColumnName='SessionID', Name='Session ID', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800841 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_InfoColumn SET ColumnName='SessionID', Name='Session ID', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800841 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_Field SET Name='Session ID', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800841) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:04 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Session ID', Name='Session ID' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800841)
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_Element SET ColumnName='SuccessFlag', Name='Success Flag', PrintName='Success Flag',Updated=TO_TIMESTAMP('2026-02-27 14:50:12','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800849
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_Column SET ColumnName='SuccessFlag', Name='Success Flag', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800849
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_Process_Para SET ColumnName='SuccessFlag', Name='Success Flag', Description=NULL, Help=NULL, AD_Element_ID=800849 WHERE UPPER(ColumnName)='SUCCESSFLAG' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_Process_Para SET ColumnName='SuccessFlag', Name='Success Flag', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800849 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_InfoColumn SET ColumnName='SuccessFlag', Name='Success Flag', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800849 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_Field SET Name='Success Flag', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800849) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:12 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Success Flag', Name='Success Flag' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800849)
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_Element SET ColumnName='TokenLimitPerRequest', Name='Token Limit Per Request', PrintName='Token Limit Per Request',Updated=TO_TIMESTAMP('2026-02-27 14:50:30','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800855
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_Column SET ColumnName='TokenLimitPerRequest', Name='Token Limit Per Request', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800855
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_Process_Para SET ColumnName='TokenLimitPerRequest', Name='Token Limit Per Request', Description=NULL, Help=NULL, AD_Element_ID=800855 WHERE UPPER(ColumnName)='TOKENLIMITPERREQUEST' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_Process_Para SET ColumnName='TokenLimitPerRequest', Name='Token Limit Per Request', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800855 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_InfoColumn SET ColumnName='TokenLimitPerRequest', Name='Token Limit Per Request', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800855 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_Field SET Name='Token Limit Per Request', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800855) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:30 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Token Limit Per Request', Name='Token Limit Per Request' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800855)
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_Element SET ColumnName='TotalTokens', Name='Total Tokens', PrintName='Total Tokens',Updated=TO_TIMESTAMP('2026-02-27 14:50:40','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Element_ID=800844
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_Column SET ColumnName='TotalTokens', Name='Total Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800844
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_Process_Para SET ColumnName='TotalTokens', Name='Total Tokens', Description=NULL, Help=NULL, AD_Element_ID=800844 WHERE UPPER(ColumnName)='TOTALTOKENS' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_Process_Para SET ColumnName='TotalTokens', Name='Total Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800844 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_InfoColumn SET ColumnName='TotalTokens', Name='Total Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Element_ID=800844 AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_Field SET Name='Total Tokens', Description=NULL, Help=NULL, Placeholder=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=800844) AND IsCentrallyMaintained='Y'
;

-- Feb 27, 2026, 2:50:40 PM CET
UPDATE AD_PrintFormatItem SET PrintName='Total Tokens', Name='Total Tokens' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=800844)
;

