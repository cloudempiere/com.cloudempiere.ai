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

/** Generated Model for AIG_ChatOwnership
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_ChatOwnership")
public class X_AIG_ChatOwnership extends PO implements I_AIG_ChatOwnership, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251204L;

    /** Standard Constructor */
    public X_AIG_ChatOwnership (Properties ctx, int AIG_ChatOwnership_ID, String trxName)
    {
      super (ctx, AIG_ChatOwnership_ID, trxName);
      /** if (AIG_ChatOwnership_ID == 0)
        {
			setAIG_ChatOwnership_ID (0);
			setCM_Chat_ID (0);
			setOwnershipType (null);
// R
        } */
    }

    /** Standard Constructor */
    public X_AIG_ChatOwnership (Properties ctx, int AIG_ChatOwnership_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_ChatOwnership_ID, trxName, virtualColumns);
      /** if (AIG_ChatOwnership_ID == 0)
        {
			setAIG_ChatOwnership_ID (0);
			setCM_Chat_ID (0);
			setOwnershipType (null);
// R
        } */
    }

    /** Load Constructor */
    public X_AIG_ChatOwnership (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_ChatOwnership[")
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

	/** Set AI Chat Ownership.
		@param AIG_ChatOwnership_ID AI Chat Ownership
	*/
	public void setAIG_ChatOwnership_ID (int AIG_ChatOwnership_ID)
	{
		if (AIG_ChatOwnership_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AIG_ChatOwnership_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AIG_ChatOwnership_ID, Integer.valueOf(AIG_ChatOwnership_ID));
	}

	/** Get AI Chat Ownership.
		@return AI Chat Ownership	  */
	public int getAIG_ChatOwnership_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AIG_ChatOwnership_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AIG_ChatOwnership_UU.
		@param AIG_ChatOwnership_UU AIG_ChatOwnership_UU
	*/
	public void setAIG_ChatOwnership_UU (String AIG_ChatOwnership_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_ChatOwnership_UU, AIG_ChatOwnership_UU);
	}

	/** Get AIG_ChatOwnership_UU.
		@return AIG_ChatOwnership_UU	  */
	public String getAIG_ChatOwnership_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_ChatOwnership_UU);
	}

	public org.compiere.model.I_CM_Chat getCM_Chat() throws RuntimeException
	{
		return (org.compiere.model.I_CM_Chat)MTable.get(getCtx(), org.compiere.model.I_CM_Chat.Table_ID)
			.getPO(getCM_Chat_ID(), get_TrxName());
	}

	/** Set Chat.
		@param CM_Chat_ID Chat or discussion thread
	*/
	public void setCM_Chat_ID (int CM_Chat_ID)
	{
		if (CM_Chat_ID < 1)
			set_ValueNoCheck (COLUMNNAME_CM_Chat_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_CM_Chat_ID, Integer.valueOf(CM_Chat_ID));
	}

	/** Get Chat.
		@return Chat or discussion thread
	  */
	public int getCM_Chat_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_CM_Chat_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** OwnershipType AD_Reference_ID=800126 */
	public static final int OWNERSHIPTYPE_AD_Reference_ID=800126;
	/** Owner = O */
	public static final String OWNERSHIPTYPE_Owner = "O";
	/** Read = R */
	public static final String OWNERSHIPTYPE_Read = "R";
	/** Write = W */
	public static final String OWNERSHIPTYPE_Write = "W";
	/** Set Ownership Type.
		@param OwnershipType Ownership Type
	*/
	public void setOwnershipType (String OwnershipType)
	{

		set_Value (COLUMNNAME_OwnershipType, OwnershipType);
	}

	/** Get Ownership Type.
		@return Ownership Type	  */
	public String getOwnershipType()
	{
		return (String)get_Value(COLUMNNAME_OwnershipType);
	}

	public org.compiere.model.I_AD_User getSharedBy_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getSharedBy_User_ID(), get_TrxName());
	}

	/** Set Shared by User.
		@param SharedBy_User_ID Shared by User
	*/
	public void setSharedBy_User_ID (int SharedBy_User_ID)
	{
		if (SharedBy_User_ID < 1)
			set_Value (COLUMNNAME_SharedBy_User_ID, null);
		else
			set_Value (COLUMNNAME_SharedBy_User_ID, Integer.valueOf(SharedBy_User_ID));
	}

	/** Get Shared by User.
		@return Shared by User	  */
	public int getSharedBy_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SharedBy_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Valid from.
		@param ValidFrom Valid from including this date (first day)
	*/
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_ValueNoCheck (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom()
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo Valid to including this date (last day)
	*/
	public void setValidTo (Timestamp ValidTo)
	{
		set_Value (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo()
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}
}