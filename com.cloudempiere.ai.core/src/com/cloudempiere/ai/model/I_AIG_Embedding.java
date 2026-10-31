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

/** Generated Interface for AIG_Embedding
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AIG_Embedding 
{

    /** TableName=AIG_Embedding */
    public static final String Table_Name = "AIG_Embedding";

    /** AD_Table_ID=800208 */
    public static final int Table_ID = 800208;

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

    /** Column name AD_Language */
    public static final String COLUMNNAME_AD_Language = "AD_Language";

	/** Set Language.
	  * Language for this entity
	  */
	public void setAD_Language (String AD_Language);

	/** Get Language.
	  * Language for this entity
	  */
	public String getAD_Language();

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

    /** Column name AIG_Embedding_UU */
    public static final String COLUMNNAME_AIG_Embedding_UU = "AIG_Embedding_UU";

	/** Set AIG_Embedding_UU	  */
	public void setAIG_Embedding_UU (String AIG_Embedding_UU);

	/** Get AIG_Embedding_UU	  */
	public String getAIG_Embedding_UU();

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

    /** Column name Embedding */
    public static final String COLUMNNAME_Embedding = "Embedding";

	/** Set Embedding	  */
	public void setEmbedding (String Embedding);

	/** Get Embedding	  */
	public String getEmbedding();

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

    /** Column name metadata */
    public static final String COLUMNNAME_metadata = "metadata";

	/** Set metadata	  */
	public void setmetadata (String metadata);

	/** Get metadata	  */
	public String getmetadata();

    /** Column name sourceid */
    public static final String COLUMNNAME_sourceid = "sourceid";

	/** Set sourceid	  */
	public void setsourceid (String sourceid);

	/** Get sourceid	  */
	public String getsourceid();

    /** Column name sourcetable */
    public static final String COLUMNNAME_sourcetable = "sourcetable";

	/** Set sourcetable	  */
	public void setsourcetable (String sourcetable);

	/** Get sourcetable	  */
	public String getsourcetable();

    /** Column name sourcetype */
    public static final String COLUMNNAME_sourcetype = "sourcetype";

	/** Set sourcetype	  */
	public void setsourcetype (String sourcetype);

	/** Get sourcetype	  */
	public String getsourcetype();

    /** Column name textsegment */
    public static final String COLUMNNAME_textsegment = "textsegment";

	/** Set textsegment	  */
	public void settextsegment (String textsegment);

	/** Get textsegment	  */
	public String gettextsegment();

    /** Column name textsegmenthash */
    public static final String COLUMNNAME_textsegmenthash = "textsegmenthash";

	/** Set textsegmenthash	  */
	public void settextsegmenthash (String textsegmenthash);

	/** Get textsegmenthash	  */
	public String gettextsegmenthash();

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
