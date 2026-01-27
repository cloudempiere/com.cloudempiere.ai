/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.routing;

/**
 * Data type categories for iDempiere entities
 * Used to determine appropriate TTL for conversation context caching
 *
 * @author Cloudempiere
 */
public enum DataType {
    /** Customer/Business Partner data */
    CUSTOMER,

    /** Sales/Purchase Order data */
    ORDER,

    /** Product/Item data */
    PRODUCT,

    /** Invoice data */
    INVOICE,

    /** Shipment/Delivery data */
    SHIPMENT,

    /** Payment data */
    PAYMENT,

    /** Request/Ticket data */
    REQUEST,

    /** Generic Business Partner data */
    PARTNER,

    /** Unknown or mixed data type */
    OTHER
}
