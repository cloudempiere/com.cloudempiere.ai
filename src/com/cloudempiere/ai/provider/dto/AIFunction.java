package com.cloudempiere.ai.provider.dto;

import java.util.Map;

/**
 * Function definition for function calling
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIFunction {
    private String name;
    private String description;
    private Map<String, Object> parameters;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}