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
/** Generated Model - DO NOT CHANGE */
package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for AIG_UsageMetrics
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_UsageMetrics")
public class X_AIG_UsageMetrics extends PO implements I_AIG_UsageMetrics, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_AIG_UsageMetrics (Properties ctx, int AIG_UsageMetrics_ID, String trxName)
    {
      super (ctx, AIG_UsageMetrics_ID, trxName);
      /** if (AIG_UsageMetrics_ID == 0)
        {
			setAD_User_ID (0);
			setAIG_UsageMetrics_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AIG_UsageMetrics (Properties ctx, int AIG_UsageMetrics_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_UsageMetrics_ID, trxName, virtualColumns);
      /** if (AIG_UsageMetrics_ID == 0)
        {
			setAD_User_ID (0);
			setAIG_UsageMetrics_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AIG_UsageMetrics (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_AIG_UsageMetrics[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Role)MTable.get(getCtx(), org.compiere.model.I_AD_Role.Table_ID)
			.getPO(getAD_Role_ID(), get_TrxName());
	}

	/** Set Role.
		@param AD_Role_ID Responsibility Role
	*/
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0)
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Agent Name.
		@param AgentName Agent Name
	*/
	public void setAgentName (String AgentName)
	{
		set_Value (COLUMNNAME_AgentName, AgentName);
	}

	/** Get Agent Name.
		@return Agent Name	  */
	public String getAgentName()
	{
		return (String)get_Value(COLUMNNAME_AgentName);
	}

	/** Set Agent Type.
		@param AgentType Agent Type
	*/
	public void setAgentType (String AgentType)
	{
		set_Value (COLUMNNAME_AgentType, AgentType);
	}

	/** Get Agent Type.
		@return Agent Type	  */
	public String getAgentType()
	{
		return (String)get_Value(COLUMNNAME_AgentType);
	}

	public I_AIG_Provider getAIG_Provider() throws RuntimeException
	{
		return (I_AIG_Provider)MTable.get(getCtx(), I_AIG_Provider.Table_ID)
			.getPO(getAIG_Provider_ID(), get_TrxName());
	}

	/** Set AI Provider.
		@param AIG_Provider_ID Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)
	*/
	public void setAIG_Provider_ID (int AIG_Provider_ID)
	{
		if (AIG_Provider_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_Provider_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_Provider_ID, Integer.valueOf(AIG_Provider_ID));
	}

	/** Get AI Provider.
		@return Central registry of AI service providers (OpenAI, Anthropic, AWS Bedrock, etc.)
	  */
	public int getAIG_Provider_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_Provider_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AI Usage Metrics.
		@param AIG_UsageMetrics_ID AI Usage Metrics
	*/
	public void setAIG_UsageMetrics_ID (int AIG_UsageMetrics_ID)
	{
		if (AIG_UsageMetrics_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_UsageMetrics_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_UsageMetrics_ID, Integer.valueOf(AIG_UsageMetrics_ID));
	}

	/** Get AI Usage Metrics.
		@return AI Usage Metrics	  */
	public int getAIG_UsageMetrics_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_UsageMetrics_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_UsageMetrics_UU.
		@param AIG_UsageMetrics_UU AIG_UsageMetrics_UU
	*/
	public void setAIG_UsageMetrics_UU (String AIG_UsageMetrics_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_UsageMetrics_UU, AIG_UsageMetrics_UU);
	}

	/** Get AIG_UsageMetrics_UU.
		@return AIG_UsageMetrics_UU	  */
	public String getAIG_UsageMetrics_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_UsageMetrics_UU);
	}

	/** Set Cost Value.
		@param CostAmt Value with Cost
	*/
	public void setCostAmt (int CostAmt)
	{
		set_Value (COLUMNNAME_CostAmt, Integer.valueOf(CostAmt));
	}

	/** Get Cost Value.
		@return Value with Cost
	  */
	public int getCostAmt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_CostAmt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Error Message.
		@param ErrorMessage Error Message
	*/
	public void setErrorMessage (String ErrorMessage)
	{
		set_Value (COLUMNNAME_ErrorMessage, ErrorMessage);
	}

	/** Get Error Message.
		@return Error Message	  */
	public String getErrorMessage()
	{
		return (String)get_Value(COLUMNNAME_ErrorMessage);
	}

	/** Set Input Tokens.
		@param InputTokens Input Tokens
	*/
	public void setInputTokens (int InputTokens)
	{
		set_Value (COLUMNNAME_InputTokens, Integer.valueOf(InputTokens));
	}

	/** Get Input Tokens.
		@return Input Tokens	  */
	public int getInputTokens()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_InputTokens);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Latency ms.
		@param LatencyMs Latency ms
	*/
	public void setLatencyMs (int LatencyMs)
	{
		set_Value (COLUMNNAME_LatencyMs, Integer.valueOf(LatencyMs));
	}

	/** Get Latency ms.
		@return Latency ms	  */
	public int getLatencyMs()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LatencyMs);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Model Name.
		@param ModelName Model Name
	*/
	public void setModelName (String ModelName)
	{
		set_Value (COLUMNNAME_ModelName, ModelName);
	}

	/** Get Model Name.
		@return Model Name	  */
	public String getModelName()
	{
		return (String)get_Value(COLUMNNAME_ModelName);
	}

	/** Set Output Tokens.
		@param OutputTokens Output Tokens
	*/
	public void setOutputTokens (int OutputTokens)
	{
		set_Value (COLUMNNAME_OutputTokens, Integer.valueOf(OutputTokens));
	}

	/** Get Output Tokens.
		@return Output Tokens	  */
	public int getOutputTokens()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OutputTokens);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Request Hash.
		@param RequestHash Request Hash
	*/
	public void setRequestHash (String RequestHash)
	{
		set_Value (COLUMNNAME_RequestHash, RequestHash);
	}

	/** Get Request Hash.
		@return Request Hash	  */
	public String getRequestHash()
	{
		return (String)get_Value(COLUMNNAME_RequestHash);
	}

	/** Set Request Timestamp.
		@param RequestTimestamp Request Timestamp
	*/
	public void setRequestTimestamp (Timestamp RequestTimestamp)
	{
		set_Value (COLUMNNAME_RequestTimestamp, RequestTimestamp);
	}

	/** Get Request Timestamp.
		@return Request Timestamp	  */
	public Timestamp getRequestTimestamp()
	{
		return (Timestamp)get_Value(COLUMNNAME_RequestTimestamp);
	}

	/** Set Request Type.
		@param RequestType Request Type
	*/
	public void setRequestType (String RequestType)
	{
		set_Value (COLUMNNAME_RequestType, RequestType);
	}

	/** Get Request Type.
		@return Request Type	  */
	public String getRequestType()
	{
		return (String)get_Value(COLUMNNAME_RequestType);
	}

	/** Set Session ID.
		@param SessionID Session ID
	*/
	public void setSessionID (String SessionID)
	{
		set_Value (COLUMNNAME_SessionID, SessionID);
	}

	/** Get Session ID.
		@return Session ID	  */
	public String getSessionID()
	{
		return (String)get_Value(COLUMNNAME_SessionID);
	}

	/** Set Success Flag.
		@param SuccessFlag Success Flag
	*/
	public void setSuccessFlag (boolean SuccessFlag)
	{
		set_Value (COLUMNNAME_SuccessFlag, Boolean.valueOf(SuccessFlag));
	}

	/** Get Success Flag.
		@return Success Flag	  */
	public boolean isSuccessFlag()
	{
		Object oo = get_Value(COLUMNNAME_SuccessFlag);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Total Tokens.
		@param TotalTokens Total Tokens
	*/
	public void setTotalTokens (int TotalTokens)
	{
		set_Value (COLUMNNAME_TotalTokens, Integer.valueOf(TotalTokens));
	}

	/** Get Total Tokens.
		@return Total Tokens	  */
	public int getTotalTokens()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_TotalTokens);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}