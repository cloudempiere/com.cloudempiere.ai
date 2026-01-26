-- CLD-1601: Plugin Health and Prerequisite Verification (ADR-050, ADR-051)
SELECT register_migration_script('202512251200_CLD-1601.sql') FROM dual;

-- Dec 25, 2025: AD_Message entries for AI plugin health and defensive programming

-- Service unavailable message (shown when AI service is not healthy)
MERGE INTO AD_Message m USING (SELECT 'AIG_ServiceUnavailable' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('I','AI features are currently unavailable. Please try again later.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_ServiceUnavailable','MM02',SYS_GUID());

-- Generic error message (fallback for unexpected errors)
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_Generic' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('E','An unexpected error occurred. Please try again.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_Generic','MM02',SYS_GUID());

-- AI Assistant Unavailable title
MERGE INTO AD_Message m USING (SELECT 'AIServiceUnavailable' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('I','AI Assistant Unavailable',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIServiceUnavailable','MM02',SYS_GUID());

-- Prerequisites not met message
MERGE INTO AD_Message m USING (SELECT 'AIG_PrerequisitesFailed' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('W','AI features unavailable - critical prerequisites missing',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_PrerequisitesFailed','MM02',SYS_GUID());

-- Partially available message (degraded mode)
MERGE INTO AD_Message m USING (SELECT 'AIG_PartiallyAvailable' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('W','AI features partially available - some prerequisites missing',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_PartiallyAvailable','MM02',SYS_GUID());

-- Rate limit error
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_RateLimit' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('E','Too many requests. Please wait a moment and try again.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_RateLimit','MM02',SYS_GUID());

-- Timeout error
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_Timeout' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('E','Request timed out. Please try again.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_Timeout','MM02',SYS_GUID());

-- Content filter error
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_ContentFilter' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('W','Content blocked by safety filters.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_ContentFilter','MM02',SYS_GUID());

-- Configuration error
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_Configuration' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('E','AI service configuration error. Please contact administrator.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_Configuration','MM02',SYS_GUID());

-- Context length error
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_ContextLength' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('W','Conversation too long. Please start a new thread.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_ContextLength','MM02',SYS_GUID());

-- Service unavailable error (provider down)
MERGE INTO AD_Message m USING (SELECT 'AIG_Error_ServiceUnavailable' AS Value FROM DUAL) s
ON (m.Value = s.Value)
WHEN NOT MATCHED THEN INSERT (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
VALUES ('E','AI service is temporarily unavailable. Please try again later.',0,0,'Y',SYSDATE,100,SYSDATE,100,AD_Message_SQ.NEXTVAL,'AIG_Error_ServiceUnavailable','MM02',SYS_GUID());
