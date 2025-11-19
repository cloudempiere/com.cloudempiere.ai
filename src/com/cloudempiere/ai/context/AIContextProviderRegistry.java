/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) CloudEmpiere, Inc. All Rights Reserved.                     *
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
package com.cloudempiere.ai.context;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.context.impl.ChartContextProvider;
import com.cloudempiere.ai.context.impl.WindowContextProvider;

/**
 * Registry for AI context providers
 *
 * <p>Manages different context provider implementations and allows
 * dynamic registration of new providers. This registry follows a
 * singleton pattern and is thread-safe.
 *
 * <p>Default providers registered at initialization:
 * <ul>
 *   <li>CHART - ChartContextProvider</li>
 *   <li>WINDOW - WindowContextProvider</li>
 * </ul>
 *
 * <p>Additional providers can be registered at runtime using the
 * {@link #register(IAIContextProvider)} method.
 *
 * <p>Example usage:
 * <pre>
 * AIContextProviderRegistry registry = AIContextProviderRegistry.getInstance();
 * IAIContextProvider chartProvider = registry.getProvider("CHART");
 * if (chartProvider != null) {
 *     JSONObject context = chartProvider.extractContext(ctx, windowNo, params);
 * }
 * </pre>
 *
 * @author CloudEmpiere
 * @version 1.0
 */
public class AIContextProviderRegistry {

    private static final CLogger log = CLogger.getCLogger(AIContextProviderRegistry.class);

    private static volatile AIContextProviderRegistry instance;

    private final Map<String, IAIContextProvider> providers;

    /**
     * Private constructor - use getInstance()
     */
    private AIContextProviderRegistry() {
        this.providers = new ConcurrentHashMap<>();
        registerDefaultProviders();
    }

    /**
     * Get singleton instance of the registry
     *
     * <p>Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return the registry instance
     */
    public static AIContextProviderRegistry getInstance() {
        if (instance == null) {
            synchronized (AIContextProviderRegistry.class) {
                if (instance == null) {
                    instance = new AIContextProviderRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Register default context providers
     */
    private void registerDefaultProviders() {
        try {
            // Register chart provider
            register(new ChartContextProvider());

            // Register window provider
            register(new WindowContextProvider());

            // Additional providers can be registered here as they are implemented:
            // register(new ProcessContextProvider());
            // register(new ReportContextProvider());
            // register(new FormContextProvider());
            // register(new InfoWindowContextProvider());
            // register(new DashboardContextProvider());

            log.info("Registered " + providers.size() + " AI context providers");

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to register default context providers", e);
        }
    }

    /**
     * Register a context provider
     *
     * <p>If a provider for the same context type already exists,
     * it will be replaced.
     *
     * @param provider the provider to register
     * @throws IllegalArgumentException if provider is null or context type is invalid
     */
    public void register(IAIContextProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Context provider cannot be null");
        }

        String type = provider.getContextType();
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Context type cannot be null or empty");
        }

        String normalizedType = type.toUpperCase();
        providers.put(normalizedType, provider);

        log.fine("Registered context provider: " + normalizedType +
                 " (" + provider.getClass().getSimpleName() + ")");
    }

    /**
     * Unregister a context provider
     *
     * @param contextType the context type to unregister
     * @return the removed provider, or null if not found
     */
    public IAIContextProvider unregister(String contextType) {
        if (contextType == null || contextType.trim().isEmpty()) {
            return null;
        }

        String normalizedType = contextType.toUpperCase();
        IAIContextProvider removed = providers.remove(normalizedType);

        if (removed != null) {
            log.fine("Unregistered context provider: " + normalizedType);
        }

        return removed;
    }

    /**
     * Get context provider by type
     *
     * <p>The context type lookup is case-insensitive.
     *
     * @param contextType the type of context (e.g., "CHART", "WINDOW")
     * @return the provider, or null if not found
     */
    public IAIContextProvider getProvider(String contextType) {
        if (contextType == null || contextType.trim().isEmpty()) {
            return null;
        }

        String normalizedType = contextType.toUpperCase();
        return providers.get(normalizedType);
    }

    /**
     * Check if provider exists for type
     *
     * <p>The context type lookup is case-insensitive.
     *
     * @param contextType the type of context to check
     * @return true if a provider exists for this type
     */
    public boolean hasProvider(String contextType) {
        if (contextType == null || contextType.trim().isEmpty()) {
            return false;
        }

        String normalizedType = contextType.toUpperCase();
        return providers.containsKey(normalizedType);
    }

    /**
     * Get all registered provider types
     *
     * @return unmodifiable collection of provider type names
     */
    public Collection<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    /**
     * Get all registered providers
     *
     * @return unmodifiable collection of providers
     */
    public Collection<IAIContextProvider> getProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Get count of registered providers
     *
     * @return number of registered providers
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Clear all providers (useful for testing)
     *
     * <p>WARNING: This will remove all registered providers.
     * Use with caution, typically only in test scenarios.
     */
    public void clear() {
        providers.clear();
        log.warning("All context providers have been cleared");
    }

    /**
     * Reset registry to default state
     *
     * <p>Clears all providers and re-registers defaults.
     */
    public void reset() {
        clear();
        registerDefaultProviders();
        log.info("Context provider registry has been reset to defaults");
    }

    /**
     * Get registry information for debugging
     *
     * @return string representation of registered providers
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AIContextProviderRegistry{");
        sb.append("providers=").append(providers.size());
        sb.append(", types=[");

        boolean first = true;
        for (String type : providers.keySet()) {
            if (!first) sb.append(", ");
            sb.append(type);
            first = false;
        }

        sb.append("]}");
        return sb.toString();
    }
}