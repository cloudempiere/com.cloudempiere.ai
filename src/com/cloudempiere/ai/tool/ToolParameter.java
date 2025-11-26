/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.tool;

/**
 * Definition of a tool parameter
 *
 * <p>Describes a parameter that a tool accepts, including its type,
 * description, whether it's required, and default value.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ToolParameter {

    /** Parameter type */
    public enum Type {
        STRING,
        INTEGER,
        NUMBER,
        BOOLEAN,
        ARRAY,
        OBJECT
    }

    private final String description;
    private final Type type;
    private final boolean required;
    private final Object defaultValue;

    /**
     * Create a required string parameter
     *
     * @param description parameter description
     * @return new ToolParameter
     */
    public static ToolParameter requiredString(String description) {
        return new ToolParameter(description, Type.STRING, true, null);
    }

    /**
     * Create an optional string parameter
     *
     * @param description parameter description
     * @param defaultValue default value if not provided
     * @return new ToolParameter
     */
    public static ToolParameter optionalString(String description, String defaultValue) {
        return new ToolParameter(description, Type.STRING, false, defaultValue);
    }

    /**
     * Create a required integer parameter
     *
     * @param description parameter description
     * @return new ToolParameter
     */
    public static ToolParameter requiredInteger(String description) {
        return new ToolParameter(description, Type.INTEGER, true, null);
    }

    /**
     * Create an optional integer parameter
     *
     * @param description parameter description
     * @param defaultValue default value if not provided
     * @return new ToolParameter
     */
    public static ToolParameter optionalInteger(String description, int defaultValue) {
        return new ToolParameter(description, Type.INTEGER, false, defaultValue);
    }

    /**
     * Create a required boolean parameter
     *
     * @param description parameter description
     * @return new ToolParameter
     */
    public static ToolParameter requiredBoolean(String description) {
        return new ToolParameter(description, Type.BOOLEAN, true, null);
    }

    /**
     * Create an optional boolean parameter
     *
     * @param description parameter description
     * @param defaultValue default value if not provided
     * @return new ToolParameter
     */
    public static ToolParameter optionalBoolean(String description, boolean defaultValue) {
        return new ToolParameter(description, Type.BOOLEAN, false, defaultValue);
    }

    /**
     * Create a custom parameter
     *
     * @param description parameter description
     * @param type parameter type
     * @param required whether parameter is required
     * @param defaultValue default value (null for required parameters)
     */
    public ToolParameter(String description, Type type, boolean required, Object defaultValue) {
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get JSON schema type string
     *
     * @return JSON schema type
     */
    public String getJsonType() {
        switch (type) {
            case STRING:
                return "string";
            case INTEGER:
                return "integer";
            case NUMBER:
                return "number";
            case BOOLEAN:
                return "boolean";
            case ARRAY:
                return "array";
            case OBJECT:
                return "object";
            default:
                return "string";
        }
    }

    @Override
    public String toString() {
        return "ToolParameter{" +
               "type=" + type +
               ", required=" + required +
               ", description='" + description + '\'' +
               '}';
    }
}
