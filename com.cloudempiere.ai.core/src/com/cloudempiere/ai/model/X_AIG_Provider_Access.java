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

/** Generated Model for AIG_Provider_Access
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_Provider_Access")
public class X_AIG_Provider_Access extends PO implements I_AIG_Provider_Access, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260312L;

    /** Standard Constructor */
    public X_AIG_Provider_Access (Properties ctx, int AIG_Provider_Access_ID, String trxName)
    {
      super (ctx, AIG_Provider_Access_ID, trxName);
      /** if (AIG_Provider_Access_ID == 0)
        {
			setAIG_Provider_Access_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AIG_Provider_Access (Properties ctx, int AIG_Provider_Access_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_Provider_Access_ID, trxName, virtualColumns);
      /** if (AIG_Provider_Access_ID == 0)
        {
			setAIG_Provider_Access_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AIG_Provider_Access (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_Provider_Access[")
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
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
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

	public I_AIG_Prompt_Config getAIG_Prompt_Config() throws RuntimeException
	{
		return (I_AIG_Prompt_Config)MTable.get(getCtx(), I_AIG_Prompt_Config.Table_ID)
			.getPO(getAIG_Prompt_Config_ID(), get_TrxName());
	}

	/** Set Prompt Configuration.
		@param AIG_Prompt_Config_ID Prompt Configuration
	*/
	public void setAIG_Prompt_Config_ID (int AIG_Prompt_Config_ID)
	{
		if (AIG_Prompt_Config_ID < 1)
			set_Value (COLUMNNAME_AIG_Prompt_Config_ID, null);
		else
			set_Value (COLUMNNAME_AIG_Prompt_Config_ID, Integer.valueOf(AIG_Prompt_Config_ID));
	}

	/** Get Prompt Configuration.
		@return Prompt Configuration	  */
	public int getAIG_Prompt_Config_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_Prompt_Config_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AI Provider Access.
		@param AIG_Provider_Access_ID AI Provider Access
	*/
	public void setAIG_Provider_Access_ID (int AIG_Provider_Access_ID)
	{
		if (AIG_Provider_Access_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_Provider_Access_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_Provider_Access_ID, Integer.valueOf(AIG_Provider_Access_ID));
	}

	/** Get AI Provider Access.
		@return AI Provider Access	  */
	public int getAIG_Provider_Access_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_Provider_Access_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_Provider_Access_UU.
		@param AIG_Provider_Access_UU AIG_Provider_Access_UU
	*/
	public void setAIG_Provider_Access_UU (String AIG_Provider_Access_UU)
	{
		set_Value (COLUMNNAME_AIG_Provider_Access_UU, AIG_Provider_Access_UU);
	}

	/** Get AIG_Provider_Access_UU.
		@return AIG_Provider_Access_UU	  */
	public String getAIG_Provider_Access_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_Provider_Access_UU);
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
			set_Value (COLUMNNAME_AIG_Provider_ID, null);
		else
			set_Value (COLUMNNAME_AIG_Provider_ID, Integer.valueOf(AIG_Provider_ID));
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
}