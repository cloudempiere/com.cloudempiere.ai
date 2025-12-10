-- Migration script for CLD-1601
-- Description: Add AI error handling messages for chat panel
-- Author: Claude Code
-- Date: 2025-12-10
-- Ticket: https://cloudempiereai.atlassian.net/browse/CLD-1601

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Add AI error handling messages
-- These messages provide user-friendly error feedback in the chat panel

-- Rate Limit Error (800100)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800100, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_RateLimit', 'E',
    'I''m receiving too many requests right now. What you can do: Wait about 30 seconds and try again.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_RateLimit');

-- Timeout Error (800101)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800101, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_Timeout', 'E',
    'That request took too long to process. What you can do: Try asking a simpler or more specific question.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Timeout');

-- Content Filter Error (800102)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800102, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_ContentFilter', 'E',
    'I can''t respond to that request due to content guidelines. What you can do: Please rephrase your question differently.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ContentFilter');

-- Configuration Error (800103)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800103, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_Configuration', 'E',
    'I''m having trouble connecting to the AI service. What you can do: Please contact your system administrator to check the AI provider configuration.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Configuration');

-- Context Length Error (800104)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800104, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_ContextLength', 'E',
    'Our conversation has become too long for me to process. What you can do: Start a new conversation using the ''New Chat'' button.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ContextLength');

-- Service Unavailable Error (800105)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800105, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_ServiceUnavailable', 'E',
    'The AI service is temporarily unavailable. What you can do: Please wait a few minutes and try again. If the problem persists, contact support.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_ServiceUnavailable');

-- Generic Error (800106)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800106, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_Generic', 'E',
    'Something unexpected went wrong while processing your request. What you can do: Try again, rephrase your question, or contact support with the error reference shown.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_Generic');

-- Session/Context Error (800107)
INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, MsgType, MsgText, EntityType, AD_Message_UU)
SELECT 800107, 0, 0, 'Y', SYSDATE, 100, SYSDATE, 100,
    'AIG_Error_SessionContext', 'E',
    'Your session context could not be verified. What you can do: Please log out and log back in, then try again.',
    'CLDE', SYS_GUID()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_Message WHERE Value = 'AIG_Error_SessionContext');

-- Verification: Display created messages
SELECT AD_Message_ID, Value, MsgType, MsgText
FROM AD_Message
WHERE Value LIKE 'AIG_Error_%'
ORDER BY AD_Message_ID;

SELECT register_migration_script('202512101430_CLD-1601.sql') FROM dual;
