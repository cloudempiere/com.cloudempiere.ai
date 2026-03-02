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

/** Generated Model for AIG_Embedding
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_Embedding")
public class X_AIG_Embedding extends PO implements I_AIG_Embedding, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_AIG_Embedding (Properties ctx, int AIG_Embedding_ID, String trxName)
    {
      super (ctx, AIG_Embedding_ID, trxName);
      /** if (AIG_Embedding_ID == 0)
        {
			setAIG_Embedding_UU (null);
			setsourcetype (null);
			settextsegment (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_Embedding (Properties ctx, int AIG_Embedding_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_Embedding_ID, trxName, virtualColumns);
      /** if (AIG_Embedding_ID == 0)
        {
			setAIG_Embedding_UU (null);
			setsourcetype (null);
			settextsegment (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_Embedding (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_Embedding[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** AD_Language AD_Reference_ID=106 */
	public static final int AD_LANGUAGE_AD_Reference_ID=106;
	/** Set Language.
		@param AD_Language Language for this entity
	*/
	public void setAD_Language (String AD_Language)
	{

		set_ValueNoCheck (COLUMNNAME_AD_Language, AD_Language);
	}

	/** Get Language.
		@return Language for this entity
	  */
	public String getAD_Language()
	{
		return (String)get_Value(COLUMNNAME_AD_Language);
	}

	/** Set AIG_Embedding_UU.
		@param AIG_Embedding_UU AIG_Embedding_UU
	*/
	public void setAIG_Embedding_UU (String AIG_Embedding_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_Embedding_UU, AIG_Embedding_UU);
	}

	/** Get AIG_Embedding_UU.
		@return AIG_Embedding_UU	  */
	public String getAIG_Embedding_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_Embedding_UU);
	}

	/** Set Embedding.
		@param Embedding Embedding
	*/
	public void setEmbedding (String Embedding)
	{
		set_Value (COLUMNNAME_Embedding, Embedding);
	}

	/** Get Embedding.
		@return Embedding	  */
	public String getEmbedding()
	{
		return (String)get_Value(COLUMNNAME_Embedding);
	}

	/** Set metadata.
		@param metadata metadata
	*/
	public void setmetadata (String metadata)
	{
		set_Value (COLUMNNAME_metadata, metadata);
	}

	/** Get metadata.
		@return metadata	  */
	public String getmetadata()
	{
		return (String)get_Value(COLUMNNAME_metadata);
	}

	/** Set sourceid.
		@param sourceid sourceid
	*/
	public void setsourceid (String sourceid)
	{
		set_Value (COLUMNNAME_sourceid, sourceid);
	}

	/** Get sourceid.
		@return sourceid	  */
	public String getsourceid()
	{
		return (String)get_Value(COLUMNNAME_sourceid);
	}

	/** Set sourcetable.
		@param sourcetable sourcetable
	*/
	public void setsourcetable (String sourcetable)
	{
		set_Value (COLUMNNAME_sourcetable, sourcetable);
	}

	/** Get sourcetable.
		@return sourcetable	  */
	public String getsourcetable()
	{
		return (String)get_Value(COLUMNNAME_sourcetable);
	}

	/** Set sourcetype.
		@param sourcetype sourcetype
	*/
	public void setsourcetype (String sourcetype)
	{
		set_Value (COLUMNNAME_sourcetype, sourcetype);
	}

	/** Get sourcetype.
		@return sourcetype	  */
	public String getsourcetype()
	{
		return (String)get_Value(COLUMNNAME_sourcetype);
	}

	/** Set textsegment.
		@param textsegment textsegment
	*/
	public void settextsegment (String textsegment)
	{
		set_Value (COLUMNNAME_textsegment, textsegment);
	}

	/** Get textsegment.
		@return textsegment	  */
	public String gettextsegment()
	{
		return (String)get_Value(COLUMNNAME_textsegment);
	}

	/** Set textsegmenthash.
		@param textsegmenthash textsegmenthash
	*/
	public void settextsegmenthash (String textsegmenthash)
	{
		set_Value (COLUMNNAME_textsegmenthash, textsegmenthash);
	}

	/** Get textsegmenthash.
		@return textsegmenthash	  */
	public String gettextsegmenthash()
	{
		return (String)get_Value(COLUMNNAME_textsegmenthash);
	}
}