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

/** Generated Model for AIG_IngestionMetadata
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AIG_IngestionMetadata")
public class X_AIG_IngestionMetadata extends PO implements I_AIG_IngestionMetadata, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_AIG_IngestionMetadata (Properties ctx, int AIG_IngestionMetadata_ID, String trxName)
    {
      super (ctx, AIG_IngestionMetadata_ID, trxName);
      /** if (AIG_IngestionMetadata_ID == 0)
        {
			setAIG_IngestionMetadata_UU (null);
			setsourcetype (null);
        } */
    }

    /** Standard Constructor */
    public X_AIG_IngestionMetadata (Properties ctx, int AIG_IngestionMetadata_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AIG_IngestionMetadata_ID, trxName, virtualColumns);
      /** if (AIG_IngestionMetadata_ID == 0)
        {
			setAIG_IngestionMetadata_UU (null);
			setsourcetype (null);
        } */
    }

    /** Load Constructor */
    public X_AIG_IngestionMetadata (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AIG_IngestionMetadata[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set AIG_IngestionMetadata_UU.
		@param AIG_IngestionMetadata_UU AIG_IngestionMetadata_UU
	*/
	public void setAIG_IngestionMetadata_UU (String AIG_IngestionMetadata_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AIG_IngestionMetadata_UU, AIG_IngestionMetadata_UU);
	}

	/** Get AIG_IngestionMetadata_UU.
		@return AIG_IngestionMetadata_UU	  */
	public String getAIG_IngestionMetadata_UU()
	{
		return (String)get_Value(COLUMNNAME_AIG_IngestionMetadata_UU);
	}

	/** Set durationms.
		@param durationms durationms
	*/
	public void setdurationms (String durationms)
	{
		set_Value (COLUMNNAME_durationms, durationms);
	}

	/** Get durationms.
		@return durationms	  */
	public String getdurationms()
	{
		return (String)get_Value(COLUMNNAME_durationms);
	}

	/** Set Error Message.
		@param ErrorMessage Error Message
	*/
	public void setErrorMessage (String ErrorMessage)
	{
		set_Value (COLUMNNAME_ErrorMessage, ErrorMessage);
	}

	/** Get Error Message.
		@return Error Message	  */
	public String getErrorMessage()
	{
		return (String)get_Value(COLUMNNAME_ErrorMessage);
	}

	/** Set lastingestion.
		@param lastingestion lastingestion
	*/
	public void setlastingestion (Timestamp lastingestion)
	{
		set_Value (COLUMNNAME_lastingestion, lastingestion);
	}

	/** Get lastingestion.
		@return lastingestion	  */
	public Timestamp getlastingestion()
	{
		return (Timestamp)get_Value(COLUMNNAME_lastingestion);
	}

	/** Set lastsuccessfulingestion.
		@param lastsuccessfulingestion lastsuccessfulingestion
	*/
	public void setlastsuccessfulingestion (Timestamp lastsuccessfulingestion)
	{
		set_Value (COLUMNNAME_lastsuccessfulingestion, lastsuccessfulingestion);
	}

	/** Get lastsuccessfulingestion.
		@return lastsuccessfulingestion	  */
	public Timestamp getlastsuccessfulingestion()
	{
		return (Timestamp)get_Value(COLUMNNAME_lastsuccessfulingestion);
	}

	/** Set recordcount.
		@param recordcount recordcount
	*/
	public void setrecordcount (int recordcount)
	{
		set_Value (COLUMNNAME_recordcount, Integer.valueOf(recordcount));
	}

	/** Get recordcount.
		@return recordcount	  */
	public int getrecordcount()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_recordcount);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Canceled = CA */
	public static final String STATUS_Canceled = "CA";
	/** Processed = PR */
	public static final String STATUS_Processed = "PR";
	/** Waiting = WA */
	public static final String STATUS_Waiting = "WA";
	/** Set Status.
		@param Status Status of the currently running check
	*/
	public void setStatus (String Status)
	{

		set_ValueNoCheck (COLUMNNAME_Status, Status);
	}

	/** Get Status.
		@return Status of the currently running check
	  */
	public String getStatus()
	{
		return (String)get_Value(COLUMNNAME_Status);
	}
}