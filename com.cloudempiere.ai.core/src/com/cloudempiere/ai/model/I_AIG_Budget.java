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

/** Generated Interface for AIG_Budget
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AIG_Budget 
{

    /** TableName=AIG_Budget */
    public static final String Table_Name = "AIG_Budget";

    /** AD_Table_ID=800226 */
    public static final int Table_ID = 800226;

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

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

    /** Column name AgentName */
    public static final String COLUMNNAME_AgentName = "AgentName";

	/** Set Agent Name	  */
	public void setAgentName (String AgentName);

	/** Get Agent Name	  */
	public String getAgentName();

    /** Column name AIG_Budget_ID */
    public static final String COLUMNNAME_AIG_Budget_ID = "AIG_Budget_ID";

	/** Set AI Budget	  */
	public void setAIG_Budget_ID (int AIG_Budget_ID);

	/** Get AI Budget	  */
	public int getAIG_Budget_ID();

    /** Column name AIG_Budget_UU */
    public static final String COLUMNNAME_AIG_Budget_UU = "AIG_Budget_UU";

	/** Set AIG_Budget_UU	  */
	public void setAIG_Budget_UU (String AIG_Budget_UU);

	/** Get AIG_Budget_UU	  */
	public String getAIG_Budget_UU();

    /** Column name BudgetScope */
    public static final String COLUMNNAME_BudgetScope = "BudgetScope";

	/** Set Budget Scope	  */
	public void setBudgetScope (String BudgetScope);

	/** Get Budget Scope	  */
	public String getBudgetScope();

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

    /** Column name CurrentDailyAmt */
    public static final String COLUMNNAME_CurrentDailyAmt = "CurrentDailyAmt";

	/** Set Current Daily Amt	  */
	public void setCurrentDailyAmt (int CurrentDailyAmt);

	/** Get Current Daily Amt	  */
	public int getCurrentDailyAmt();

    /** Column name CurrentMonthlyAmt */
    public static final String COLUMNNAME_CurrentMonthlyAmt = "CurrentMonthlyAmt";

	/** Set Current Monthly Amt	  */
	public void setCurrentMonthlyAmt (int CurrentMonthlyAmt);

	/** Get Current Monthly Amt	  */
	public int getCurrentMonthlyAmt();

    /** Column name DailyLimit */
    public static final String COLUMNNAME_DailyLimit = "DailyLimit";

	/** Set Daily Limit	  */
	public void setDailyLimit (int DailyLimit);

	/** Get Daily Limit	  */
	public int getDailyLimit();

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

    /** Column name LastResetDaily */
    public static final String COLUMNNAME_LastResetDaily = "LastResetDaily";

	/** Set Last Reset Daily	  */
	public void setLastResetDaily (Timestamp LastResetDaily);

	/** Get Last Reset Daily	  */
	public Timestamp getLastResetDaily();

    /** Column name LastResetMonthly */
    public static final String COLUMNNAME_LastResetMonthly = "LastResetMonthly";

	/** Set Last Reset Monthly	  */
	public void setLastResetMonthly (Timestamp LastResetMonthly);

	/** Get Last Reset Monthly	  */
	public Timestamp getLastResetMonthly();

    /** Column name MonthlyLimit */
    public static final String COLUMNNAME_MonthlyLimit = "MonthlyLimit";

	/** Set Monthly Limit	  */
	public void setMonthlyLimit (int MonthlyLimit);

	/** Get Monthly Limit	  */
	public int getMonthlyLimit();

    /** Column name RequestsPerMinute */
    public static final String COLUMNNAME_RequestsPerMinute = "RequestsPerMinute";

	/** Set Requests/Minute	  */
	public void setRequestsPerMinute (int RequestsPerMinute);

	/** Get Requests/Minute	  */
	public int getRequestsPerMinute();

    /** Column name TokenLimitPerRequest */
    public static final String COLUMNNAME_TokenLimitPerRequest = "TokenLimitPerRequest";

	/** Set Token Limit Per Request	  */
	public void setTokenLimitPerRequest (int TokenLimitPerRequest);

	/** Get Token Limit Per Request	  */
	public int getTokenLimitPerRequest();

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
