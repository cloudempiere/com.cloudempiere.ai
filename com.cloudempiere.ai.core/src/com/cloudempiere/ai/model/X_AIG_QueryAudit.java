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
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for AIG_QueryAudit
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_QueryAudit")
public class X_AIG_QueryAudit extends PO implements I_AIG_QueryAudit, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_AIG_QueryAudit (Properties ctx, int AIG_QueryAudit_ID, String trxName)
    {
      super (ctx, AIG_QueryAudit_ID, trxName);
      /** if (AIG_QueryAudit_ID == 0)
        {
			setAD_Role_ID (0);
			setAD_User_ID (0);
			setAIG_Provider_ID (0);
			setAIG_QueryAudit_ID (0);
			setAIGQuerySQL (null);
			setAIGQueryStatus (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_QueryAudit (Properties ctx, int AIG_QueryAudit_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_QueryAudit_ID, trxName, virtualColumns);
      /** if (AIG_QueryAudit_ID == 0)
        {
			setAD_Role_ID (0);
			setAD_User_ID (0);
			setAIG_Provider_ID (0);
			setAIG_QueryAudit_ID (0);
			setAIGQuerySQL (null);
			setAIGQueryStatus (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_QueryAudit (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_QueryAudit[")
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

	/** Set AI Query Audit.
		@param AIG_QueryAudit_ID AI Query Audit
	*/
	public void setAIG_QueryAudit_ID (int AIG_QueryAudit_ID)
	{
		if (AIG_QueryAudit_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_QueryAudit_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_QueryAudit_ID, Integer.valueOf(AIG_QueryAudit_ID));
	}

	/** Get AI Query Audit.
		@return AI Query Audit	  */
	public int getAIG_QueryAudit_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_QueryAudit_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_QueryAudit_UU.
		@param AIG_QueryAudit_UU AIG_QueryAudit_UU
	*/
	public void setAIG_QueryAudit_UU (String AIG_QueryAudit_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_QueryAudit_UU, AIG_QueryAudit_UU);
	}

	/** Get AIG_QueryAudit_UU.
		@return AIG_QueryAudit_UU	  */
	public String getAIG_QueryAudit_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_QueryAudit_UU);
	}

	/** Set Context Type.
		@param AIGContextType Context Type
	*/
	public void setAIGContextType (String AIGContextType)
	{
		set_Value (COLUMNNAME_AIGContextType, AIGContextType);
	}

	/** Get Context Type.
		@return Context Type	  */
	public String getAIGContextType()
	{
		return (String)get_Value(COLUMNNAME_AIGContextType);
	}

	/** Set Error Message.
		@param AIGErrorMessage Error Message
	*/
	public void setAIGErrorMessage (String AIGErrorMessage)
	{
		set_Value (COLUMNNAME_AIGErrorMessage, AIGErrorMessage);
	}

	/** Get Error Message.
		@return Error Message	  */
	public String getAIGErrorMessage()
	{
		return (String)get_Value(COLUMNNAME_AIGErrorMessage);
	}

	/** Set Execution Time in ms.
		@param AIGExecutionTimeMs Execution Time in ms
	*/
	public void setAIGExecutionTimeMs (int AIGExecutionTimeMs)
	{
		set_Value (COLUMNNAME_AIGExecutionTimeMs, Integer.valueOf(AIGExecutionTimeMs));
	}

	/** Get Execution Time in ms.
		@return Execution Time in ms	  */
	public int getAIGExecutionTimeMs()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIGExecutionTimeMs);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Permission Denied Reason.
		@param AIGPermissionDeniedReason Permission Denied Reason
	*/
	public void setAIGPermissionDeniedReason (String AIGPermissionDeniedReason)
	{
		set_Value (COLUMNNAME_AIGPermissionDeniedReason, AIGPermissionDeniedReason);
	}

	/** Get Permission Denied Reason.
		@return Permission Denied Reason	  */
	public String getAIGPermissionDeniedReason()
	{
		return (String)get_Value(COLUMNNAME_AIGPermissionDeniedReason);
	}

	/** Set Query Purpose.
		@param AIGQueryPurpose Query Purpose
	*/
	public void setAIGQueryPurpose (String AIGQueryPurpose)
	{
		set_Value (COLUMNNAME_AIGQueryPurpose, AIGQueryPurpose);
	}

	/** Get Query Purpose.
		@return Query Purpose	  */
	public String getAIGQueryPurpose()
	{
		return (String)get_Value(COLUMNNAME_AIGQueryPurpose);
	}

	/** Set Query SQL.
		@param AIGQuerySQL Query SQL
	*/
	public void setAIGQuerySQL (String AIGQuerySQL)
	{
		set_Value (COLUMNNAME_AIGQuerySQL, AIGQuerySQL);
	}

	/** Get Query SQL.
		@return Query SQL	  */
	public String getAIGQuerySQL()
	{
		return (String)get_Value(COLUMNNAME_AIGQuerySQL);
	}

	/** AIGQueryStatus AD_Reference_ID=800125 */
	public static final int AIGQUERYSTATUS_AD_Reference_ID=800125;
	/** Error = E */
	public static final String AIGQUERYSTATUS_Error = "E";
	/** Permission Denied = P */
	public static final String AIGQUERYSTATUS_PermissionDenied = "P";
	/** Success = S */
	public static final String AIGQUERYSTATUS_Success = "S";
	/** Set Query Status.
		@param AIGQueryStatus Status of the database query of the AI agent
	*/
	public void setAIGQueryStatus (String AIGQueryStatus)
	{

		set_ValueNoCheck (COLUMNNAME_AIGQueryStatus, AIGQueryStatus);
	}

	/** Get Query Status.
		@return Status of the database query of the AI agent
	  */
	public String getAIGQueryStatus()
	{
		return (String)get_Value(COLUMNNAME_AIGQueryStatus);
	}

	/** Set Row Count.
		@param AIGRowCount Row Count
	*/
	public void setAIGRowCount (int AIGRowCount)
	{
		set_Value (COLUMNNAME_AIGRowCount, Integer.valueOf(AIGRowCount));
	}

	/** Get Row Count.
		@return Row Count	  */
	public int getAIGRowCount()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIGRowCount);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Secured SQL.
		@param AIGSecuredSQL Secured SQL
	*/
	public void setAIGSecuredSQL (String AIGSecuredSQL)
	{
		set_Value (COLUMNNAME_AIGSecuredSQL, AIGSecuredSQL);
	}

	/** Get Secured SQL.
		@return Secured SQL	  */
	public String getAIGSecuredSQL()
	{
		return (String)get_Value(COLUMNNAME_AIGSecuredSQL);
	}

	/** Set Tables Accessed.
		@param AIGTablesAccessed Tables Accessed
	*/
	public void setAIGTablesAccessed (String AIGTablesAccessed)
	{
		set_Value (COLUMNNAME_AIGTablesAccessed, AIGTablesAccessed);
	}

	/** Get Tables Accessed.
		@return Tables Accessed	  */
	public String getAIGTablesAccessed()
	{
		return (String)get_Value(COLUMNNAME_AIGTablesAccessed);
	}
}