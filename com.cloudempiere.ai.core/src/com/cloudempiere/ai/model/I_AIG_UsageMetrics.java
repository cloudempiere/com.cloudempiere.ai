/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package com.cloudempiere.ai.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for AIG_UsageMetrics
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_AIG_UsageMetrics 
{

    /** TableName=AIG_UsageMetrics */
    public static final String Table_Name = "AIG_UsageMetrics";

    /** AD_Table_ID=800225 */
    public static final int Table_ID = 800225;

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name AD_Role_ID */
    public static final String COLUMNNAME_AD_Role_ID = "AD_Role_ID";

	/** Set Role.
	  * Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID);

	/** Get Role.
	  * Responsibility Role
	  */
	public int getAD_Role_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

    /** Column name AIGErrorMessage */
    public static final String COLUMNNAME_AIGErrorMessage = "AIGErrorMessage";

	/** Set Error Message	  */
	public void setAIGErrorMessage (String AIGErrorMessage);

	/** Get Error Message	  */
	public String getAIGErrorMessage();

    /** Column name AIG_Provider_ID */
    public static final String COLUMNNAME_AIG_Provider_ID = "AIG_Provider_ID";

	/** Set AI Provider.
	  * Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)
	  */
	public void setAIG_Provider_ID (int AIG_Provider_ID);

	/** Get AI Provider.
	  * Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)
	  */
	public int getAIG_Provider_ID();

	@Deprecated(since="13") // use better methods with cache
	public I_AIG_Provider getAIG_Provider() throws RuntimeException;

    /** Column name AIG_UsageMetrics_ID */
    public static final String COLUMNNAME_AIG_UsageMetrics_ID = "AIG_UsageMetrics_ID";

	/** Set AI Usage Metrics	  */
	public void setAIG_UsageMetrics_ID (int AIG_UsageMetrics_ID);

	/** Get AI Usage Metrics	  */
	public int getAIG_UsageMetrics_ID();

    /** Column name AIG_UsageMetrics_UU */
    public static final String COLUMNNAME_AIG_UsageMetrics_UU = "AIG_UsageMetrics_UU";

	/** Set AIG_UsageMetrics_UU	  */
	public void setAIG_UsageMetrics_UU (String AIG_UsageMetrics_UU);

	/** Get AIG_UsageMetrics_UU	  */
	public String getAIG_UsageMetrics_UU();

    /** Column name AgentName */
    public static final String COLUMNNAME_AgentName = "AgentName";

	/** Set Agent Name	  */
	public void setAgentName (String AgentName);

	/** Get Agent Name	  */
	public String getAgentName();

    /** Column name AgentType */
    public static final String COLUMNNAME_AgentType = "AgentType";

	/** Set Agent Type	  */
	public void setAgentType (String AgentType);

	/** Get Agent Type	  */
	public String getAgentType();

    /** Column name CostAmt */
    public static final String COLUMNNAME_CostAmt = "CostAmt";

	/** Set Cost Value.
	  * Value with Cost
	  */
	public void setCostAmt (int CostAmt);

	/** Get Cost Value.
	  * Value with Cost
	  */
	public int getCostAmt();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name InputTokens */
    public static final String COLUMNNAME_InputTokens = "InputTokens";

	/** Set Input Tokens	  */
	public void setInputTokens (int InputTokens);

	/** Get Input Tokens	  */
	public int getInputTokens();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name LatencyMs */
    public static final String COLUMNNAME_LatencyMs = "LatencyMs";

	/** Set Latency ms	  */
	public void setLatencyMs (int LatencyMs);

	/** Get Latency ms	  */
	public int getLatencyMs();

    /** Column name ModelName */
    public static final String COLUMNNAME_ModelName = "ModelName";

	/** Set Model Name	  */
	public void setModelName (String ModelName);

	/** Get Model Name	  */
	public String getModelName();

    /** Column name OutputTokens */
    public static final String COLUMNNAME_OutputTokens = "OutputTokens";

	/** Set Output Tokens	  */
	public void setOutputTokens (int OutputTokens);

	/** Get Output Tokens	  */
	public int getOutputTokens();

    /** Column name RequestHash */
    public static final String COLUMNNAME_RequestHash = "RequestHash";

	/** Set Request Hash	  */
	public void setRequestHash (String RequestHash);

	/** Get Request Hash	  */
	public String getRequestHash();

    /** Column name RequestTimestamp */
    public static final String COLUMNNAME_RequestTimestamp = "RequestTimestamp";

	/** Set Request Timestamp	  */
	public void setRequestTimestamp (Timestamp RequestTimestamp);

	/** Get Request Timestamp	  */
	public Timestamp getRequestTimestamp();

    /** Column name RequestType */
    public static final String COLUMNNAME_RequestType = "RequestType";

	/** Set Request Type	  */
	public void setRequestType (String RequestType);

	/** Get Request Type	  */
	public String getRequestType();

    /** Column name SessionID */
    public static final String COLUMNNAME_SessionID = "SessionID";

	/** Set Session ID	  */
	public void setSessionID (String SessionID);

	/** Get Session ID	  */
	public String getSessionID();

    /** Column name SuccessFlag */
    public static final String COLUMNNAME_SuccessFlag = "SuccessFlag";

	/** Set Success Flag	  */
	public void setSuccessFlag (boolean SuccessFlag);

	/** Get Success Flag	  */
	public boolean isSuccessFlag();

    /** Column name TotalTokens */
    public static final String COLUMNNAME_TotalTokens = "TotalTokens";

	/** Set Total Tokens	  */
	public void setTotalTokens (int TotalTokens);

	/** Get Total Tokens	  */
	public int getTotalTokens();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
