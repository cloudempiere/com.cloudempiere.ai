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

/** Generated Interface for AIG_ModelPricing
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AIG_ModelPricing 
{

    /** TableName=AIG_ModelPricing */
    public static final String Table_Name = "AIG_ModelPricing";

    /** AD_Table_ID=800227 */
    public static final int Table_ID = 800227;

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

    /** Column name AIG_ModelPricing_ID */
    public static final String COLUMNNAME_AIG_ModelPricing_ID = "AIG_ModelPricing_ID";

	/** Set AI Model Pricing	  */
	public void setAIG_ModelPricing_ID (int AIG_ModelPricing_ID);

	/** Get AI Model Pricing	  */
	public int getAIG_ModelPricing_ID();

    /** Column name AIG_ModelPricing_UU */
    public static final String COLUMNNAME_AIG_ModelPricing_UU = "AIG_ModelPricing_UU";

	/** Set AIG_ModelPricing_UU	  */
	public void setAIG_ModelPricing_UU (String AIG_ModelPricing_UU);

	/** Get AIG_ModelPricing_UU	  */
	public String getAIG_ModelPricing_UU();

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

    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Currency.
	  * The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Currency.
	  * The Currency for this record
	  */
	public int getC_Currency_ID();

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException;

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

    /** Column name InputCostPerMToken */
    public static final String COLUMNNAME_InputCostPerMToken = "InputCostPerMToken";

	/** Set Input Cost / Million Token.
	  * Provider cost per million input tokens
	  */
	public void setInputCostPerMToken (BigDecimal InputCostPerMToken);

	/** Get Input Cost / Million Token.
	  * Provider cost per million input tokens
	  */
	public BigDecimal getInputCostPerMToken();

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

    /** Column name ModelName */
    public static final String COLUMNNAME_ModelName = "ModelName";

	/** Set Model Name	  */
	public void setModelName (String ModelName);

	/** Get Model Name	  */
	public String getModelName();

    /** Column name OutputCostPerMToken */
    public static final String COLUMNNAME_OutputCostPerMToken = "OutputCostPerMToken";

	/** Set Output Cost / Million Token.
	  * Provider cost per million output tokens
	  */
	public void setOutputCostPerMToken (BigDecimal OutputCostPerMToken);

	/** Get Output Cost / Million Token.
	  * Provider cost per million output tokens
	  */
	public BigDecimal getOutputCostPerMToken();

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

    /** Column name ValidFrom */
    public static final String COLUMNNAME_ValidFrom = "ValidFrom";

	/** Set Valid from.
	  * Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom);

	/** Get Valid from.
	  * Valid from including this date (first day)
	  */
	public Timestamp getValidFrom();
}
