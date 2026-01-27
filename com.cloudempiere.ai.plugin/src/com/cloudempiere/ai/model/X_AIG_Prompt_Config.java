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

/** Generated Model for AIG_Prompt_Config
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_Prompt_Config")
public class X_AIG_Prompt_Config extends PO implements I_AIG_Prompt_Config, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251124L;

    /** Standard Constructor */
    public X_AIG_Prompt_Config (Properties ctx, int AIG_Prompt_Config_ID, String trxName)
    {
      super (ctx, AIG_Prompt_Config_ID, trxName);
      /** if (AIG_Prompt_Config_ID == 0)
        {
			setAIG_Prompt_Config_ID (0);
			setAIGPromptKey (null);
			setAIGPromptText (null);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_Prompt_Config (Properties ctx, int AIG_Prompt_Config_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_Prompt_Config_ID, trxName, virtualColumns);
      /** if (AIG_Prompt_Config_ID == 0)
        {
			setAIG_Prompt_Config_ID (0);
			setAIGPromptKey (null);
			setAIGPromptText (null);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_Prompt_Config (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_Prompt_Config[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Prompt Configuration.
		@param AIG_Prompt_Config_ID Prompt Configuration
	*/
	public void setAIG_Prompt_Config_ID (int AIG_Prompt_Config_ID)
	{
		if (AIG_Prompt_Config_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_Prompt_Config_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_Prompt_Config_ID, Integer.valueOf(AIG_Prompt_Config_ID));
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

	/** Set AIG_Prompt_Config_UU.
		@param AIG_Prompt_Config_UU AIG_Prompt_Config_UU
	*/
	public void setAIG_Prompt_Config_UU (String AIG_Prompt_Config_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_Prompt_Config_UU, AIG_Prompt_Config_UU);
	}

	/** Get AIG_Prompt_Config_UU.
		@return AIG_Prompt_Config_UU	  */
	public String getAIG_Prompt_Config_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_Prompt_Config_UU);
	}

	/** Set Prompt Key.
		@param AIGPromptKey Prompt Key
	*/
	public void setAIGPromptKey (String AIGPromptKey)
	{
		set_Value (COLUMNNAME_AIGPromptKey, AIGPromptKey);
	}

	/** Get Prompt Key.
		@return Prompt Key	  */
	public String getAIGPromptKey()
	{
		return (String)get_Value(COLUMNNAME_AIGPromptKey);
	}

	/** Set Prompt Text.
		@param AIGPromptText Prompt Text
	*/
	public void setAIGPromptText (String AIGPromptText)
	{
		set_Value (COLUMNNAME_AIGPromptText, AIGPromptText);
	}

	/** Get Prompt Text.
		@return Prompt Text	  */
	public String getAIGPromptText()
	{
		return (String)get_Value(COLUMNNAME_AIGPromptText);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}
}