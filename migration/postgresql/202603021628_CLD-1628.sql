-- CLD-1628
SELECT register_migration_script('202603021628_CLD-1628.sql') FROM dual;

-- Mar 2, 2026, 4:28:12 PM CET
INSERT INTO AD_Reference (AD_Reference_ID,Name,ValidationType,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,IsOrderByValue,AD_Reference_UU,ShowInactive) VALUES (800138,'AI Budget BudgetScope','L',0,0,'Y',TO_TIMESTAMP('2026-03-02 16:28:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-02 16:28:12','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','N','ca6c9edd-7b4d-4ca7-9764-e8d9d2bb920c','N')
;

-- Mar 2, 2026, 4:28:27 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800336,'User',800138,'U',0,0,'Y',TO_TIMESTAMP('2026-03-02 16:28:27','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-02 16:28:27','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','7f6a7546-9886-458e-9828-0c63a598aaff')
;

-- Mar 2, 2026, 4:28:35 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800337,'Agent',800138,'A',0,0,'Y',TO_TIMESTAMP('2026-03-02 16:28:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-02 16:28:35','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','48eace93-8b92-4090-9b3e-7315043918b2')
;

-- Mar 2, 2026, 4:28:55 PM CET
INSERT INTO AD_Ref_List (AD_Ref_List_ID,Name,AD_Reference_ID,Value,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,EntityType,AD_Ref_List_UU) VALUES (800338,'Client',800138,'C',0,0,'Y',TO_TIMESTAMP('2026-03-02 16:28:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),TO_TIMESTAMP('2026-03-02 16:28:55','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','5d93c7a3-ee93-4394-9f44-6ba4ad8b9bac')
;

-- Mar 2, 2026, 4:29:25 PM CET
UPDATE AD_Column SET FieldLength=1, AD_Reference_ID=17, AD_Reference_Value_ID=800138, FKConstraintType=NULL,Updated=TO_TIMESTAMP('2026-03-02 16:29:25','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_Column_ID=803969
;

-- Mar 2, 2026, 4:29:28 PM CET
INSERT INTO t_alter_column values('aig_budget','BudgetScope','CHAR(1)',null,null)
;

-- Mar 2, 2026, 4:29:54 PM CET
INSERT INTO t_alter_column values('aig_budget','BudgetScope','CHAR(1)',null,null)
;

