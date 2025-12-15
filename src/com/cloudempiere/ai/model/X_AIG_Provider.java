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

/** Generated Model for AIG_Provider
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_Provider")
public class X_AIG_Provider extends PO implements I_AIG_Provider, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251211L;

    /** Standard Constructor */
    public X_AIG_Provider (Properties ctx, int AIG_Provider_ID, String trxName)
    {
      super (ctx, AIG_Provider_ID, trxName);
      /** if (AIG_Provider_ID == 0)
        {
			setAIG_Provider_ID (0);
			setAIGProviderType (null);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_Provider (Properties ctx, int AIG_Provider_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_Provider_ID, trxName, virtualColumns);
      /** if (AIG_Provider_ID == 0)
        {
			setAIG_Provider_ID (0);
			setAIGProviderType (null);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_Provider (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_Provider[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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

	/** Set AIG_Provider_UU.
		@param AIG_Provider_UU AIG_Provider_UU
	*/
	public void setAIG_Provider_UU (String AIG_Provider_UU)
	{
		set_Value (COLUMNNAME_AIG_Provider_UU, AIG_Provider_UU);
	}

	/** Get AIG_Provider_UU.
		@return AIG_Provider_UU	  */
	public String getAIG_Provider_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_Provider_UU);
	}

	/** AIGProviderType AD_Reference_ID=800124 */
	public static final int AIGPROVIDERTYPE_AD_Reference_ID=800124;
	/** AWS Bedrock = ABE */
	public static final String AIGPROVIDERTYPE_AWSBedrock = "ABE";
	/** Anthropic Claude = ANT */
	public static final String AIGPROVIDERTYPE_AnthropicClaude = "ANT";
	/** Mock AI Hub = MOA */
	public static final String AIGPROVIDERTYPE_MockAIHub = "MOA";
	/** Ollama = OLL */
	public static final String AIGPROVIDERTYPE_Ollama = "OLL";
	/** Quarkus Satellite = SAT */
	public static final String AIGPROVIDERTYPE_QuarkusSatellite = "SAT";
	/** Set Provider Type.
		@param AIGProviderType The type of the AI Provider
	*/
	public void setAIGProviderType (String AIGProviderType)
	{

		set_Value (COLUMNNAME_AIGProviderType, AIGProviderType);
	}

	/** Get Provider Type.
		@return The type of the AI Provider
	  */
	public String getAIGProviderType()
	{
		return (String)get_Value(COLUMNNAME_AIGProviderType);
	}

	/** Set API Key.
		@param APIKey API Key
	*/
	public void setAPIKey (String APIKey)
	{
		set_Value (COLUMNNAME_APIKey, APIKey);
	}

	/** Get API Key.
		@return API Key	  */
	public String getAPIKey()
	{
		return (String)get_Value(COLUMNNAME_APIKey);
	}

	/** Set Default.
		@param IsDefault Default value
	*/
	public void setIsDefault (boolean IsDefault)
	{
		set_Value (COLUMNNAME_IsDefault, Boolean.valueOf(IsDefault));
	}

	/** Get Default.
		@return Default value
	  */
	public boolean isDefault()
	{
		Object oo = get_Value(COLUMNNAME_IsDefault);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** Set URL.
		@param URL URL
	*/
	public void setURL (String URL)
	{
		set_Value (COLUMNNAME_URL, URL);
	}

	/** Get URL.
		@return URL
	  */
	public String getURL()
	{
		return (String)get_Value(COLUMNNAME_URL);
	}
}