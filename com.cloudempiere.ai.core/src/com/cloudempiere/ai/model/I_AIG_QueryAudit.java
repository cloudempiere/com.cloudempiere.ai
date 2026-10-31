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

import org.compiere.util.KeyNamePair;

/** Generated Interface for AIG_QueryAudit
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AIG_QueryAudit 
{

    /** TableName=AIG_QueryAudit */
    public static final String Table_Name = "AIG_QueryAudit";

    /** AD_Table_ID=800203 */
    public static final int Table_ID = 800203;

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

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

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

	public I_AIG_Provider getAIG_Provider() throws RuntimeException;

    /** Column name AIG_QueryAudit_ID */
    public static final String COLUMNNAME_AIG_QueryAudit_ID = "AIG_QueryAudit_ID";

	/** Set AI Query Audit	  */
	public void setAIG_QueryAudit_ID (int AIG_QueryAudit_ID);

	/** Get AI Query Audit	  */
	public int getAIG_QueryAudit_ID();

    /** Column name AIG_QueryAudit_UU */
    public static final String COLUMNNAME_AIG_QueryAudit_UU = "AIG_QueryAudit_UU";

	/** Set AIG_QueryAudit_UU	  */
	public void setAIG_QueryAudit_UU (String AIG_QueryAudit_UU);

	/** Get AIG_QueryAudit_UU	  */
	public String getAIG_QueryAudit_UU();

    /** Column name AIGContextType */
    public static final String COLUMNNAME_AIGContextType = "AIGContextType";

	/** Set Context Type	  */
	public void setAIGContextType (String AIGContextType);

	/** Get Context Type	  */
	public String getAIGContextType();

    /** Column name AIGErrorMessage */
    public static final String COLUMNNAME_AIGErrorMessage = "AIGErrorMessage";

	/** Set Error Message	  */
	public void setAIGErrorMessage (String AIGErrorMessage);

	/** Get Error Message	  */
	public String getAIGErrorMessage();

    /** Column name AIGExecutionTimeMs */
    public static final String COLUMNNAME_AIGExecutionTimeMs = "AIGExecutionTimeMs";

	/** Set Execution Time in ms	  */
	public void setAIGExecutionTimeMs (int AIGExecutionTimeMs);

	/** Get Execution Time in ms	  */
	public int getAIGExecutionTimeMs();

    /** Column name AIGPermissionDeniedReason */
    public static final String COLUMNNAME_AIGPermissionDeniedReason = "AIGPermissionDeniedReason";

	/** Set Permission Denied Reason	  */
	public void setAIGPermissionDeniedReason (String AIGPermissionDeniedReason);

	/** Get Permission Denied Reason	  */
	public String getAIGPermissionDeniedReason();

    /** Column name AIGQueryPurpose */
    public static final String COLUMNNAME_AIGQueryPurpose = "AIGQueryPurpose";

	/** Set Query Purpose	  */
	public void setAIGQueryPurpose (String AIGQueryPurpose);

	/** Get Query Purpose	  */
	public String getAIGQueryPurpose();

    /** Column name AIGQuerySQL */
    public static final String COLUMNNAME_AIGQuerySQL = "AIGQuerySQL";

	/** Set Query SQL	  */
	public void setAIGQuerySQL (String AIGQuerySQL);

	/** Get Query SQL	  */
	public String getAIGQuerySQL();

    /** Column name AIGQueryStatus */
    public static final String COLUMNNAME_AIGQueryStatus = "AIGQueryStatus";

	/** Set Query Status.
	  * Status of the database query of the AI agent
	  */
	public void setAIGQueryStatus (String AIGQueryStatus);

	/** Get Query Status.
	  * Status of the database query of the AI agent
	  */
	public String getAIGQueryStatus();

    /** Column name AIGRowCount */
    public static final String COLUMNNAME_AIGRowCount = "AIGRowCount";

	/** Set Row Count	  */
	public void setAIGRowCount (int AIGRowCount);

	/** Get Row Count	  */
	public int getAIGRowCount();

    /** Column name AIGSecuredSQL */
    public static final String COLUMNNAME_AIGSecuredSQL = "AIGSecuredSQL";

	/** Set Secured SQL	  */
	public void setAIGSecuredSQL (String AIGSecuredSQL);

	/** Get Secured SQL	  */
	public String getAIGSecuredSQL();

    /** Column name AIGTablesAccessed */
    public static final String COLUMNNAME_AIGTablesAccessed = "AIGTablesAccessed";

	/** Set Tables Accessed	  */
	public void setAIGTablesAccessed (String AIGTablesAccessed);

	/** Get Tables Accessed	  */
	public String getAIGTablesAccessed();

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
