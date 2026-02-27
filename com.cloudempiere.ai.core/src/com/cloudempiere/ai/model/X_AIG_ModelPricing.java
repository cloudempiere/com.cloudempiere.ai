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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for AIG_ModelPricing
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_ModelPricing")
public class X_AIG_ModelPricing extends PO implements I_AIG_ModelPricing, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_AIG_ModelPricing (Properties ctx, int AIG_ModelPricing_ID, String trxName)
    {
      super (ctx, AIG_ModelPricing_ID, trxName);
      /** if (AIG_ModelPricing_ID == 0)
        {
			setAIG_ModelPricing_ID (0);
			setC_Currency_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
// @#Date@
        } */
    }

    /** Standard Constructor */
    public X_AIG_ModelPricing (Properties ctx, int AIG_ModelPricing_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_ModelPricing_ID, trxName, virtualColumns);
      /** if (AIG_ModelPricing_ID == 0)
        {
			setAIG_ModelPricing_ID (0);
			setC_Currency_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
// @#Date@
        } */
    }

    /** Load Constructor */
    public X_AIG_ModelPricing (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_ModelPricing[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set AI Model Pricing.
		@param AIG_ModelPricing_ID AI Model Pricing
	*/
	public void setAIG_ModelPricing_ID (int AIG_ModelPricing_ID)
	{
		if (AIG_ModelPricing_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_ModelPricing_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_ModelPricing_ID, Integer.valueOf(AIG_ModelPricing_ID));
	}

	/** Get AI Model Pricing.
		@return AI Model Pricing	  */
	public int getAIG_ModelPricing_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_ModelPricing_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_ModelPricing_UU.
		@param AIG_ModelPricing_UU AIG_ModelPricing_UU
	*/
	public void setAIG_ModelPricing_UU (String AIG_ModelPricing_UU)
	{
		set_Value (COLUMNNAME_AIG_ModelPricing_UU, AIG_ModelPricing_UU);
	}

	/** Get AIG_ModelPricing_UU.
		@return AIG_ModelPricing_UU	  */
	public String getAIG_ModelPricing_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_ModelPricing_UU);
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

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
	{
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_ID)
			.getPO(getC_Currency_ID(), get_TrxName());
	}

	/** Set Currency.
		@param C_Currency_ID The Currency for this record
	*/
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1)
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Input Cost / Million Token.
		@param InputCostPerMToken Provider cost per million input tokens
	*/
	public void setInputCostPerMToken (BigDecimal InputCostPerMToken)
	{
		set_Value (COLUMNNAME_InputCostPerMToken, InputCostPerMToken);
	}

	/** Get Input Cost / Million Token.
		@return Provider cost per million input tokens
	  */
	public BigDecimal getInputCostPerMToken()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_InputCostPerMToken);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Output Cost / Million Token.
		@param OutputCostPerMToken Provider cost per million output tokens
	*/
	public void setOutputCostPerMToken (BigDecimal OutputCostPerMToken)
	{
		set_Value (COLUMNNAME_OutputCostPerMToken, OutputCostPerMToken);
	}

	/** Get Output Cost / Million Token.
		@return Provider cost per million output tokens
	  */
	public BigDecimal getOutputCostPerMToken()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_OutputCostPerMToken);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Valid from.
		@param ValidFrom Valid from including this date (first day)
	*/
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_Value (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom()
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}
}