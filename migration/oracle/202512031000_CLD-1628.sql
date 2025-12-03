-- CLD-1628 - AI Observability and Cost Tracking (ADR-013)
SELECT register_migration_script('202512031000_CLD-1628.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Purpose: Add usage metrics and budget tracking tables for AI agents
-- Author: Cloudempiere
-- Date: 2025-12-03

-- =====================================================================
-- 1. Create AIG_UsageMetrics table
-- =====================================================================

CREATE TABLE AIG_UsageMetrics (
    AIG_UsageMetrics_ID     NUMBER(10) NOT NULL,
    AD_Client_ID            NUMBER(10) NOT NULL,
    AD_Org_ID               NUMBER(10) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')),
    Created                 DATE DEFAULT SYSDATE NOT NULL,
    CreatedBy               NUMBER(10) NOT NULL,
    Updated                 DATE DEFAULT SYSDATE NOT NULL,
    UpdatedBy               NUMBER(10) NOT NULL,
    AIG_UsageMetrics_UU     VARCHAR2(36) DEFAULT SYS_GUID(),

    -- User context
    AD_User_ID              NUMBER(10) NOT NULL,
    AD_Role_ID              NUMBER(10),

    -- Agent identification
    AgentName               VARCHAR2(100),
    AgentType               VARCHAR2(60),
    SessionID               VARCHAR2(100),

    -- Model info
    AIG_Provider_ID         NUMBER(10),
    ModelName               VARCHAR2(100),

    -- Token usage
    InputTokens             NUMBER(10) DEFAULT 0,
    OutputTokens            NUMBER(10) DEFAULT 0,
    TotalTokens             NUMBER(10) DEFAULT 0,

    -- Cost tracking (USD with 6 decimal precision)
    CostUSD                 NUMBER(10,6) DEFAULT 0,

    -- Performance
    LatencyMs               NUMBER(10),
    RequestTimestamp        DATE DEFAULT SYSDATE,

    -- Request metadata
    RequestType             VARCHAR2(60),
    RequestHash             VARCHAR2(64),
    SuccessFlag             CHAR(1) DEFAULT 'Y' CHECK (SuccessFlag IN ('Y','N')),
    ErrorMessage            VARCHAR2(2000),

    CONSTRAINT AIG_UsageMetrics_Key PRIMARY KEY (AIG_UsageMetrics_ID),
    CONSTRAINT AIG_UsageMetrics_UU_idx UNIQUE (AIG_UsageMetrics_UU),
    CONSTRAINT AIG_UsageMetrics_Client FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID),
    CONSTRAINT AIG_UsageMetrics_User FOREIGN KEY (AD_User_ID)
        REFERENCES AD_User(AD_User_ID),
    CONSTRAINT AIG_UsageMetrics_Provider FOREIGN KEY (AIG_Provider_ID)
        REFERENCES AIG_Provider(AIG_Provider_ID)
);

-- Add indexes for performance
CREATE INDEX AIG_UsageMetrics_User_Idx ON AIG_UsageMetrics(AD_User_ID, Created);
CREATE INDEX AIG_UsageMetrics_Provider_Idx ON AIG_UsageMetrics(AIG_Provider_ID, Created);
CREATE INDEX AIG_UsageMetrics_Agent_Idx ON AIG_UsageMetrics(AgentName, Created);
CREATE INDEX AIG_UsageMetrics_Request_Idx ON AIG_UsageMetrics(RequestTimestamp);

-- Add comments
COMMENT ON TABLE AIG_UsageMetrics IS 'Usage metrics for AI agent operations';
COMMENT ON COLUMN AIG_UsageMetrics.AgentName IS 'Name of the agent that made the request';
COMMENT ON COLUMN AIG_UsageMetrics.InputTokens IS 'Number of input tokens in the request';
COMMENT ON COLUMN AIG_UsageMetrics.OutputTokens IS 'Number of output tokens in the response';
COMMENT ON COLUMN AIG_UsageMetrics.CostUSD IS 'Estimated cost in USD';
COMMENT ON COLUMN AIG_UsageMetrics.LatencyMs IS 'Request latency in milliseconds';

-- =====================================================================
-- 2. Create AIG_Budget table
-- =====================================================================

CREATE TABLE AIG_Budget (
    AIG_Budget_ID           NUMBER(10) NOT NULL,
    AD_Client_ID            NUMBER(10) NOT NULL,
    AD_Org_ID               NUMBER(10) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y' CHECK (IsActive IN ('Y','N')),
    Created                 DATE DEFAULT SYSDATE NOT NULL,
    CreatedBy               NUMBER(10) NOT NULL,
    Updated                 DATE DEFAULT SYSDATE NOT NULL,
    UpdatedBy               NUMBER(10) NOT NULL,
    AIG_Budget_UU           VARCHAR2(36) DEFAULT SYS_GUID(),

    -- Scope (Client, User, or Agent level)
    AD_User_ID              NUMBER(10),
    AgentName               VARCHAR2(100),
    BudgetScope             VARCHAR2(60) NOT NULL,

    -- Limits
    DailyLimitUSD           NUMBER(10,2),
    MonthlyLimitUSD         NUMBER(10,2),
    TokenLimitPerRequest    NUMBER(10),
    RequestsPerMinute       NUMBER(10),

    -- Current usage (updated by triggers or scheduled job)
    CurrentDailyUSD         NUMBER(10,6) DEFAULT 0,
    CurrentMonthlyUSD       NUMBER(10,6) DEFAULT 0,
    LastResetDaily          DATE,
    LastResetMonthly        DATE,

    CONSTRAINT AIG_Budget_Key PRIMARY KEY (AIG_Budget_ID),
    CONSTRAINT AIG_Budget_UU_idx UNIQUE (AIG_Budget_UU),
    CONSTRAINT AIG_Budget_Client FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID),
    CONSTRAINT AIG_Budget_User FOREIGN KEY (AD_User_ID)
        REFERENCES AD_User(AD_User_ID)
);

-- Add indexes
CREATE INDEX AIG_Budget_User_Idx ON AIG_Budget(AD_User_ID);
CREATE INDEX AIG_Budget_Agent_Idx ON AIG_Budget(AgentName);
CREATE INDEX AIG_Budget_Scope_Idx ON AIG_Budget(BudgetScope, AD_Client_ID);

-- Add comments
COMMENT ON TABLE AIG_Budget IS 'Budget limits and current usage for AI operations';
COMMENT ON COLUMN AIG_Budget.BudgetScope IS 'CLIENT, USER, or AGENT';
COMMENT ON COLUMN AIG_Budget.DailyLimitUSD IS 'Maximum daily spend in USD';
COMMENT ON COLUMN AIG_Budget.MonthlyLimitUSD IS 'Maximum monthly spend in USD';

-- =====================================================================
-- 3. Create summary views
-- =====================================================================

-- Daily usage summary view
CREATE OR REPLACE VIEW AIG_UsageSummary_Daily AS
SELECT
    AD_Client_ID,
    AD_User_ID,
    AgentName,
    ModelName,
    TRUNC(RequestTimestamp) as UsageDate,
    COUNT(*) as RequestCount,
    SUM(InputTokens) as TotalInputTokens,
    SUM(OutputTokens) as TotalOutputTokens,
    SUM(TotalTokens) as TotalTokens,
    SUM(CostUSD) as TotalCostUSD,
    AVG(LatencyMs) as AvgLatencyMs,
    SUM(CASE WHEN SuccessFlag = 'Y' THEN 1 ELSE 0 END) as SuccessCount,
    SUM(CASE WHEN SuccessFlag = 'N' THEN 1 ELSE 0 END) as ErrorCount
FROM AIG_UsageMetrics
WHERE IsActive = 'Y'
GROUP BY AD_Client_ID, AD_User_ID, AgentName, ModelName, TRUNC(RequestTimestamp);

COMMENT ON TABLE AIG_UsageSummary_Daily IS 'Daily aggregated AI usage statistics';

-- Agent performance view
CREATE OR REPLACE VIEW AIG_AgentPerformance AS
SELECT
    AD_Client_ID,
    AgentName,
    AgentType,
    COUNT(*) as TotalRequests,
    SUM(TotalTokens) as TotalTokens,
    SUM(CostUSD) as TotalCostUSD,
    AVG(LatencyMs) as AvgLatencyMs,
    MIN(LatencyMs) as MinLatencyMs,
    MAX(LatencyMs) as MaxLatencyMs,
    ROUND(100.0 * SUM(CASE WHEN SuccessFlag = 'Y' THEN 1 ELSE 0 END) / COUNT(*), 2) as SuccessRate,
    MIN(RequestTimestamp) as FirstRequest,
    MAX(RequestTimestamp) as LastRequest
FROM AIG_UsageMetrics
WHERE IsActive = 'Y'
GROUP BY AD_Client_ID, AgentName, AgentType;

COMMENT ON TABLE AIG_AgentPerformance IS 'Aggregated performance metrics per AI agent';

-- Budget status view
CREATE OR REPLACE VIEW AIG_BudgetStatus AS
SELECT
    b.AIG_Budget_ID,
    b.AD_Client_ID,
    b.AD_User_ID,
    b.AgentName,
    b.BudgetScope,
    b.DailyLimitUSD,
    b.MonthlyLimitUSD,
    b.CurrentDailyUSD,
    b.CurrentMonthlyUSD,
    CASE
        WHEN b.DailyLimitUSD > 0 THEN ROUND(100.0 * b.CurrentDailyUSD / b.DailyLimitUSD, 2)
        ELSE 0
    END as DailyUsagePercent,
    CASE
        WHEN b.MonthlyLimitUSD > 0 THEN ROUND(100.0 * b.CurrentMonthlyUSD / b.MonthlyLimitUSD, 2)
        ELSE 0
    END as MonthlyUsagePercent,
    CASE
        WHEN b.DailyLimitUSD > 0 AND b.CurrentDailyUSD >= b.DailyLimitUSD THEN 'DAILY_EXCEEDED'
        WHEN b.MonthlyLimitUSD > 0 AND b.CurrentMonthlyUSD >= b.MonthlyLimitUSD THEN 'MONTHLY_EXCEEDED'
        WHEN b.DailyLimitUSD > 0 AND b.CurrentDailyUSD >= 0.9 * b.DailyLimitUSD THEN 'DAILY_WARNING'
        WHEN b.MonthlyLimitUSD > 0 AND b.CurrentMonthlyUSD >= 0.9 * b.MonthlyLimitUSD THEN 'MONTHLY_WARNING'
        ELSE 'OK'
    END as BudgetStatus
FROM AIG_Budget b
WHERE b.IsActive = 'Y';

COMMENT ON TABLE AIG_BudgetStatus IS 'Current budget status with usage percentages and warnings';

COMMIT;
