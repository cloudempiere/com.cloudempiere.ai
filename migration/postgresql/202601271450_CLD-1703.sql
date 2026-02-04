-- CLD-1703
SELECT register_migration_script('202601271450_CLD-1703.sql') FROM dual;

-- Jan 27, 2026, 2:50:26 PM CET
DROP INDEX cm_chat_record
;

-- Jan 27, 2026, 2:50:49 PM CET
INSERT INTO AD_IndexColumn (AD_Client_ID,AD_Org_ID,AD_IndexColumn_ID,AD_IndexColumn_UU,Created,CreatedBy,EntityType,IsActive,Updated,UpdatedBy,AD_Column_ID,AD_TableIndex_ID,SeqNo) VALUES (0,0,800074,'74bf429b-8755-404b-b6e9-fe02ffcd645c',TO_TIMESTAMP('2026-01-27 14:50:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),'MM02','Y',TO_TIMESTAMP('2026-01-27 14:50:49','YYYY-MM-DD HH24:MI:SS'),toRecordId('AD_User','7803d4f7-a42f-4a02-bc1e-c1d748a3bb80'),15505,200582,12)
;

-- Jan 27, 2026, 2:51:00 PM CET
UPDATE AD_IndexColumn SET SeqNo=10,Updated=TO_TIMESTAMP('2026-01-27 14:51:00','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=1134855 WHERE AD_IndexColumn_ID=800074
;

-- Jan 27, 2026, 2:51:13 PM CET
CREATE UNIQUE INDEX cm_chat_record ON CM_Chat (AD_Table_ID,Record_ID,AD_Client_ID)
;

