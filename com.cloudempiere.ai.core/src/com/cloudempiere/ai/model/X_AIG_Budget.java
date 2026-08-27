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

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for AIG_Budget
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_Budget")
public class X_AIG_Budget extends PO implements I_AIG_Budget, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260302L;

    /** Standard Constructor */
    public X_AIG_Budget (Properties ctx, int AIG_Budget_ID, String trxName)
    {
      super (ctx, AIG_Budget_ID, trxName);
      /** if (AIG_Budget_ID == 0)
        {
			setAIG_Budget_ID (0);
			setBudgetScope (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_Budget (Properties ctx, int AIG_Budget_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_Budget_ID, trxName, virtualColumns);
      /** if (AIG_Budget_ID == 0)
        {
			setAIG_Budget_ID (0);
			setBudgetScope (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_Budget (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_Budget[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set AI Budget.
		@param AIG_Budget_ID AI Budget
	*/
	public void setAIG_Budget_ID (int AIG_Budget_ID)
	{
		if (AIG_Budget_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_Budget_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_Budget_ID, Integer.valueOf(AIG_Budget_ID));
	}

	/** Get AI Budget.
		@return AI Budget	  */
	public int getAIG_Budget_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_Budget_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_Budget_UU.
		@param AIG_Budget_UU AIG_Budget_UU
	*/
	public void setAIG_Budget_UU (String AIG_Budget_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_Budget_UU, AIG_Budget_UU);
	}

	/** Get AIG_Budget_UU.
		@return AIG_Budget_UU	  */
	public String getAIG_Budget_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_Budget_UU);
	}

	/** BudgetScope AD_Reference_ID=800138 */
	public static final int BUDGETSCOPE_AD_Reference_ID=800138;
	/** Agent = A */
	public static final String BUDGETSCOPE_Agent = "A";
	/** Client = C */
	public static final String BUDGETSCOPE_Client = "C";
	/** User = U */
	public static final String BUDGETSCOPE_User = "U";
	/** Set Budget Scope.
		@param BudgetScope Budget Scope
	*/
	public void setBudgetScope (String BudgetScope)
	{

		set_Value (COLUMNNAME_BudgetScope, BudgetScope);
	}

	/** Get Budget Scope.
		@return Budget Scope	  */
	public String getBudgetScope()
	{
		return (String)get_Value(COLUMNNAME_BudgetScope);
	}

	/** Set Current Daily Amt.
		@param CurrentDailyAmt Current Daily Amt
	*/
	public void setCurrentDailyAmt (int CurrentDailyAmt)
	{
		set_Value (COLUMNNAME_CurrentDailyAmt, Integer.valueOf(CurrentDailyAmt));
	}

	/** Get Current Daily Amt.
		@return Current Daily Amt	  */
	public int getCurrentDailyAmt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_CurrentDailyAmt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Current Monthly Amt.
		@param CurrentMonthlyAmt Current Monthly Amt
	*/
	public void setCurrentMonthlyAmt (int CurrentMonthlyAmt)
	{
		set_Value (COLUMNNAME_CurrentMonthlyAmt, Integer.valueOf(CurrentMonthlyAmt));
	}

	/** Get Current Monthly Amt.
		@return Current Monthly Amt	  */
	public int getCurrentMonthlyAmt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_CurrentMonthlyAmt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Daily Limit.
		@param DailyLimit Daily Limit
	*/
	public void setDailyLimit (int DailyLimit)
	{
		set_Value (COLUMNNAME_DailyLimit, Integer.valueOf(DailyLimit));
	}

	/** Get Daily Limit.
		@return Daily Limit	  */
	public int getDailyLimit()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DailyLimit);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Last Reset Daily.
		@param LastResetDaily Last Reset Daily
	*/
	public void setLastResetDaily (Timestamp LastResetDaily)
	{
		set_Value (COLUMNNAME_LastResetDaily, LastResetDaily);
	}

	/** Get Last Reset Daily.
		@return Last Reset Daily	  */
	public Timestamp getLastResetDaily()
	{
		return (Timestamp)get_Value(COLUMNNAME_LastResetDaily);
	}

	/** Set Last Reset Monthly.
		@param LastResetMonthly Last Reset Monthly
	*/
	public void setLastResetMonthly (Timestamp LastResetMonthly)
	{
		set_Value (COLUMNNAME_LastResetMonthly, LastResetMonthly);
	}

	/** Get Last Reset Monthly.
		@return Last Reset Monthly	  */
	public Timestamp getLastResetMonthly()
	{
		return (Timestamp)get_Value(COLUMNNAME_LastResetMonthly);
	}

	/** Set Monthly Limit.
		@param MonthlyLimit Monthly Limit
	*/
	public void setMonthlyLimit (int MonthlyLimit)
	{
		set_Value (COLUMNNAME_MonthlyLimit, Integer.valueOf(MonthlyLimit));
	}

	/** Get Monthly Limit.
		@return Monthly Limit	  */
	public int getMonthlyLimit()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_MonthlyLimit);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Requests/Minute.
		@param RequestsPerMinute Requests/Minute
	*/
	public void setRequestsPerMinute (int RequestsPerMinute)
	{
		set_Value (COLUMNNAME_RequestsPerMinute, Integer.valueOf(RequestsPerMinute));
	}

	/** Get Requests/Minute.
		@return Requests/Minute	  */
	public int getRequestsPerMinute()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_RequestsPerMinute);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Token Limit Per Request.
		@param TokenLimitPerRequest Token Limit Per Request
	*/
	public void setTokenLimitPerRequest (int TokenLimitPerRequest)
	{
		set_Value (COLUMNNAME_TokenLimitPerRequest, Integer.valueOf(TokenLimitPerRequest));
	}

	/** Get Token Limit Per Request.
		@return Token Limit Per Request	  */
	public int getTokenLimitPerRequest()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_TokenLimitPerRequest);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}