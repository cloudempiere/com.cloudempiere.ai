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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts entity references from user prompts
 *
 * Recognizes patterns like:
 * - "order #12345" → order_12345
 * - "customer C-1000" → customer_C-1000
 * - "product SKU-123" → product_SKU-123
 * - "invoice INV-456" → invoice_INV-456
 * - "What's its status?" → LAST_ENTITY marker
 *
 * @author Cloudempiere
 */
public class EntityExtractor {

    /** Pattern for order references */
    private static final Pattern ORDER_PATTERN =
        Pattern.compile("\\border[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for customer/business partner references */
    private static final Pattern CUSTOMER_PATTERN =
        Pattern.compile("\\b(customer|bpartner|partner)[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for product references */
    private static final Pattern PRODUCT_PATTERN =
        Pattern.compile("\\bproduct[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for invoice references */
    private static final Pattern INVOICE_PATTERN =
        Pattern.compile("\\binvoice[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for shipment references */
    private static final Pattern SHIPMENT_PATTERN =
        Pattern.compile("\\b(shipment|delivery)[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for payment references */
    private static final Pattern PAYMENT_PATTERN =
        Pattern.compile("\\bpayment[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern for request references */
    private static final Pattern REQUEST_PATTERN =
        Pattern.compile("\\brequest[\\s#-]*(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Extract entity references from prompt
     * @param prompt user's prompt text
     * @return set of entity keys (e.g., "order_12345", "customer_C-1000")
     */
    public Set<String> extractEntities(String prompt) {
        Set<String> entities = new HashSet<>();

        if (prompt == null || prompt.trim().isEmpty()) {
            return entities;
        }

        // Extract order references
        Matcher orderMatcher = ORDER_PATTERN.matcher(prompt);
        while (orderMatcher.find()) {
            entities.add("order_" + orderMatcher.group(1));
        }

        // Extract customer references
        Matcher customerMatcher = CUSTOMER_PATTERN.matcher(prompt);
        while (customerMatcher.find()) {
            entities.add("customer_" + customerMatcher.group(2));
        }

        // Extract product references
        Matcher productMatcher = PRODUCT_PATTERN.matcher(prompt);
        while (productMatcher.find()) {
            entities.add("product_" + productMatcher.group(1));
        }

        // Extract invoice references
        Matcher invoiceMatcher = INVOICE_PATTERN.matcher(prompt);
        while (invoiceMatcher.find()) {
            entities.add("invoice_" + invoiceMatcher.group(1));
        }

        // Extract shipment references
        Matcher shipmentMatcher = SHIPMENT_PATTERN.matcher(prompt);
        while (shipmentMatcher.find()) {
            entities.add("shipment_" + shipmentMatcher.group(2));
        }

        // Extract payment references
        Matcher paymentMatcher = PAYMENT_PATTERN.matcher(prompt);
        while (paymentMatcher.find()) {
            entities.add("payment_" + paymentMatcher.group(1));
        }

        // Extract request references
        Matcher requestMatcher = REQUEST_PATTERN.matcher(prompt);
        while (requestMatcher.find()) {
            entities.add("request_" + requestMatcher.group(1));
        }

        // Check for pronoun references
        if (containsPronounReference(prompt)) {
            entities.add("LAST_ENTITY"); // Special marker for context resolution
        }

        return entities;
    }

    /**
     * Extract entity types from prompt
     * @param prompt user's prompt text
     * @return set of entity types detected
     */
    public Set<EntityType> extractEntityTypes(String prompt) {
        Set<EntityType> types = new HashSet<>();

        if (prompt == null || prompt.trim().isEmpty()) {
            return types;
        }

        String lowerPrompt = prompt.toLowerCase();

        if (ORDER_PATTERN.matcher(prompt).find() || lowerPrompt.contains("order")) {
            types.add(EntityType.ORDER);
        }

        if (CUSTOMER_PATTERN.matcher(prompt).find() ||
            lowerPrompt.contains("customer") ||
            lowerPrompt.contains("partner")) {
            types.add(EntityType.CUSTOMER);
        }

        if (PRODUCT_PATTERN.matcher(prompt).find() || lowerPrompt.contains("product")) {
            types.add(EntityType.PRODUCT);
        }

        if (INVOICE_PATTERN.matcher(prompt).find() || lowerPrompt.contains("invoice")) {
            types.add(EntityType.INVOICE);
        }

        if (SHIPMENT_PATTERN.matcher(prompt).find() ||
            lowerPrompt.contains("shipment") ||
            lowerPrompt.contains("delivery")) {
            types.add(EntityType.SHIPMENT);
        }

        if (PAYMENT_PATTERN.matcher(prompt).find() || lowerPrompt.contains("payment")) {
            types.add(EntityType.PAYMENT);
        }

        if (REQUEST_PATTERN.matcher(prompt).find() || lowerPrompt.contains("request")) {
            types.add(EntityType.REQUEST);
        }

        return types;
    }

    /**
     * Check if prompt contains pronoun reference to previous entity
     * @param prompt user's prompt
     * @return true if contains pronouns like "it", "that", "this"
     */
    private boolean containsPronounReference(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        return lowerPrompt.matches(".*\\b(it|that|this|its|their)\\b.*");
    }

    /**
     * Entity types in iDempiere
     */
    public enum EntityType {
        /** Sales/Purchase Order (C_Order) */
        ORDER,

        /** Business Partner Customer (C_BPartner) */
        CUSTOMER,

        /** Product/Item (M_Product) */
        PRODUCT,

        /** Invoice (C_Invoice) */
        INVOICE,

        /** Shipment/Delivery (M_InOut) */
        SHIPMENT,

        /** Payment (C_Payment) */
        PAYMENT,

        /** Request/Ticket (R_Request) */
        REQUEST,

        /** Generic Business Partner (C_BPartner) */
        PARTNER,

        /** Warehouse (M_Warehouse) */
        WAREHOUSE
    }
}
