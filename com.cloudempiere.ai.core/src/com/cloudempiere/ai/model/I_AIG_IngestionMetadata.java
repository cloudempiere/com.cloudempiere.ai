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

/** Generated Interface for AIG_IngestionMetadata
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AIG_IngestionMetadata 
{

    /** TableName=AIG_IngestionMetadata */
    public static final String Table_Name = "AIG_IngestionMetadata";

    /** AD_Table_ID=800210 */
    public static final int Table_ID = 800210;

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

    /** Column name AIG_IngestionMetadata_UU */
    public static final String COLUMNNAME_AIG_IngestionMetadata_UU = "AIG_IngestionMetadata_UU";

	/** Set AIG_IngestionMetadata_UU	  */
	public void setAIG_IngestionMetadata_UU (String AIG_IngestionMetadata_UU);

	/** Get AIG_IngestionMetadata_UU	  */
	public String getAIG_IngestionMetadata_UU();

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

    /** Column name durationms */
    public static final String COLUMNNAME_durationms = "durationms";

	/** Set durationms	  */
	public void setdurationms (String durationms);

	/** Get durationms	  */
	public String getdurationms();

    /** Column name ErrorMessage */
    public static final String COLUMNNAME_ErrorMessage = "ErrorMessage";

	/** Set Error Message	  */
	public void setErrorMessage (String ErrorMessage);

	/** Get Error Message	  */
	public String getErrorMessage();

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

    /** Column name lastingestion */
    public static final String COLUMNNAME_lastingestion = "lastingestion";

	/** Set lastingestion	  */
	public void setlastingestion (Timestamp lastingestion);

	/** Get lastingestion	  */
	public Timestamp getlastingestion();

    /** Column name lastsuccessfulingestion */
    public static final String COLUMNNAME_lastsuccessfulingestion = "lastsuccessfulingestion";

	/** Set lastsuccessfulingestion	  */
	public void setlastsuccessfulingestion (Timestamp lastsuccessfulingestion);

	/** Get lastsuccessfulingestion	  */
	public Timestamp getlastsuccessfulingestion();

    /** Column name recordcount */
    public static final String COLUMNNAME_recordcount = "recordcount";

	/** Set recordcount	  */
	public void setrecordcount (int recordcount);

	/** Get recordcount	  */
	public int getrecordcount();

    /** Column name sourcetype */
    public static final String COLUMNNAME_sourcetype = "sourcetype";

	/** Set sourcetype	  */
	public void setsourcetype (String sourcetype);

	/** Get sourcetype	  */
	public String getsourcetype();

    /** Column name Status */
    public static final String COLUMNNAME_Status = "Status";

	/** Set Status.
	  * Status of the currently running check
	  */
	public void setStatus (String Status);

	/** Get Status.
	  * Status of the currently running check
	  */
	public String getStatus();

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
