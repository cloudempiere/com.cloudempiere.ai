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

import java.time.Duration;

/**
 * TTL (Time-To-Live) configuration by iDempiere data type
 *
 * Guidelines based on data volatility:
 * - Master data: 24 hours (customers, products - change rarely)
 * - Reference data: 4 hours (price lists, payment terms - change occasionally)
 * - Transactional: 5 minutes (orders, invoices - change frequently)
 * - Volatile: 1 minute (inventory, real-time pricing - change constantly)
 *
 * @author Cloudempiere
 */
public class TTLConfig {

    // Master data (changes rarely)
    /** Master data TTL - 24 hours */
    public static final Duration MASTER_DATA_TTL = Duration.ofHours(24);

    // Reference data (changes occasionally)
    /** Reference data TTL - 4 hours */
    public static final Duration REFERENCE_DATA_TTL = Duration.ofHours(4);

    // Transactional data (changes frequently)
    /** Transactional data TTL - 5 minutes */
    public static final Duration TRANSACTIONAL_TTL = Duration.ofMinutes(5);

    // Volatile data (changes constantly)
    /** Volatile data TTL - 1 minute */
    public static final Duration VOLATILE_DATA_TTL = Duration.ofMinutes(1);

    /**
     * Get appropriate TTL for data category
     * @param category data category
     * @return TTL duration
     */
    public static Duration getTTL(DataCategory category) {
        // Master data (rarely changes)
        if (category == DataCategory.CUSTOMER ||
            category == DataCategory.PARTNER ||
            category == DataCategory.PRODUCT_CATALOG ||
            category == DataCategory.USER) {
            return MASTER_DATA_TTL;
        }

        // Reference data (changes occasionally)
        if (category == DataCategory.PRICE_LIST ||
            category == DataCategory.PAYMENT_TERMS ||
            category == DataCategory.TAX_RATES ||
            category == DataCategory.WAREHOUSE) {
            return REFERENCE_DATA_TTL;
        }

        // Transactional data (changes frequently)
        if (category == DataCategory.ORDER ||
            category == DataCategory.INVOICE ||
            category == DataCategory.SHIPMENT ||
            category == DataCategory.PAYMENT ||
            category == DataCategory.REQUEST) {
            return TRANSACTIONAL_TTL;
        }

        // Volatile data (changes constantly)
        if (category == DataCategory.INVENTORY ||
            category == DataCategory.STOCK_LEVEL ||
            category == DataCategory.REAL_TIME_PRICE) {
            return VOLATILE_DATA_TTL;
        }

        // Default fallback
        return TRANSACTIONAL_TTL;
    }

    /**
     * Get TTL by iDempiere table name
     * Maps specific iDempiere tables to appropriate TTL
     *
     * @param tableName table name (e.g., C_Order, M_Product)
     * @return TTL duration
     */
    public static Duration getTTLByTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return TRANSACTIONAL_TTL; // Safe default
        }

        String table = tableName.trim();

        // Master data tables (24 hours)
        if (table.equals("C_BPartner") ||
            table.equals("M_Product") ||
            table.equals("M_Product_Category") ||
            table.equals("AD_User") ||
            table.equals("AD_Org") ||
            table.equals("AD_Client")) {
            return MASTER_DATA_TTL;
        }

        // Reference data tables (4 hours)
        if (table.equals("M_PriceList") ||
            table.equals("M_PriceList_Version") ||
            table.equals("C_PaymentTerm") ||
            table.equals("C_Tax") ||
            table.equals("C_TaxCategory") ||
            table.equals("M_Warehouse") ||
            table.equals("M_Locator") ||
            table.equals("C_Currency") ||
            table.equals("C_UOM")) {
            return REFERENCE_DATA_TTL;
        }

        // Transactional tables (5 minutes)
        if (table.equals("C_Order") ||
            table.equals("C_OrderLine") ||
            table.equals("C_Invoice") ||
            table.equals("C_InvoiceLine") ||
            table.equals("M_InOut") ||
            table.equals("M_InOutLine") ||
            table.equals("C_Payment") ||
            table.equals("R_Request") ||
            table.equals("C_CashLine") ||
            table.equals("C_BankStatement")) {
            return TRANSACTIONAL_TTL;
        }

        // Volatile data tables (1 minute)
        if (table.equals("M_Storage") ||
            table.equals("M_StorageOnHand") ||
            table.equals("M_StorageReservation") ||
            table.equals("M_Transaction")) {
            return VOLATILE_DATA_TTL;
        }

        // Default to transactional (conservative)
        return TRANSACTIONAL_TTL;
    }

    /**
     * Get TTL by data type
     * @param dataType data type enum
     * @return TTL duration
     */
    public static Duration getTTLByDataType(DataType dataType) {
        // Master data
        if (dataType == DataType.CUSTOMER ||
            dataType == DataType.PARTNER ||
            dataType == DataType.PRODUCT) {
            return MASTER_DATA_TTL;
        }

        // Transactional data
        if (dataType == DataType.ORDER ||
            dataType == DataType.INVOICE ||
            dataType == DataType.SHIPMENT ||
            dataType == DataType.PAYMENT ||
            dataType == DataType.REQUEST) {
            return TRANSACTIONAL_TTL;
        }

        // Default to reference data TTL
        return REFERENCE_DATA_TTL;
    }

    /**
     * Data categories for TTL determination
     */
    public enum DataCategory {
        // Master data (rarely changes)
        /** Customer/Vendor data */
        CUSTOMER,
        /** Business Partner data */
        PARTNER,
        /** Product catalog */
        PRODUCT_CATALOG,
        /** User data */
        USER,

        // Reference data (changes occasionally)
        /** Price lists */
        PRICE_LIST,
        /** Payment terms */
        PAYMENT_TERMS,
        /** Tax rates */
        TAX_RATES,
        /** Warehouse locations */
        WAREHOUSE,

        // Transactional data (changes frequently)
        /** Sales/Purchase orders */
        ORDER,
        /** Invoices */
        INVOICE,
        /** Shipments */
        SHIPMENT,
        /** Payments */
        PAYMENT,
        /** Requests/Tickets */
        REQUEST,

        // Volatile data (changes constantly)
        /** Inventory levels */
        INVENTORY,
        /** Stock on hand */
        STOCK_LEVEL,
        /** Real-time pricing */
        REAL_TIME_PRICE
    }
}
