-- CLD-1601: Plugin Health and Prerequisite Verification (ADR-050, ADR-051)
SELECT register_migration_script('202512251200_CLD-1601.sql') FROM dual;

-- Dec 25, 2025: AD_Message entries for AI plugin health and defensive programming
-- Service unavailable message (shown when AI service is not healthy)
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'I','AI features are currently unavailable. Please try again later.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_ServiceUnavailable','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_ServiceUnavailable');

-- Generic error message (fallback for unexpected errors)
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'E','An unexpected error occurred. Please try again.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_Generic','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Generic');

-- AI Assistant Unavailable title
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'I','AI Assistant Unavailable',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIServiceUnavailable','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIServiceUnavailable');

-- Prerequisites not met message
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'W','AI features unavailable - critical prerequisites missing',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_PrerequisitesFailed','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_PrerequisitesFailed');

-- Partially available message (degraded mode)
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'W','AI features partially available - some prerequisites missing',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_PartiallyAvailable','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_PartiallyAvailable');

-- Rate limit error
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'E','Too many requests. Please wait a moment and try again.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_RateLimit','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_RateLimit');

-- Timeout error
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'E','Request timed out. Please try again.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_Timeout','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Timeout');

-- Content filter error
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'W','Content blocked by safety filters.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_ContentFilter','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ContentFilter');

-- Configuration error
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'E','AI service configuration error. Please contact administrator.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_Configuration','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Configuration');

-- Context length error
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'W','Conversation too long. Please start a new thread.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_ContextLength','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ContextLength');

-- Service unavailable error (provider down)
INSERT INTO AD_Message (MsgType,MsgText,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Message_ID,Value,EntityType,AD_Message_UU)
SELECT 'E','AI service is temporarily unavailable. Please try again later.',0,0,'Y',NOW(),100,NOW(),100,nextval('ad_message_sq'),'AIG_Error_ServiceUnavailable','MM02',generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ServiceUnavailable');
