package com.cloudempiere.ai.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Utility for looking up OSGi services.
 *
 * <p>Provides a simple API for obtaining OSGi service references
 * from non-OSGi managed classes (like ZK components).</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
public class ServiceLocator {

    /**
     * Get an OSGi service by class.
     *
     * @param <T> service type
     * @param serviceClass service class
     * @return service instance or null if not found
     */
    public static <T> T getService(Class<T> serviceClass) {
        try {
            Bundle bundle = FrameworkUtil.getBundle(ServiceLocator.class);
            if (bundle == null) {
                return null;
            }

            BundleContext context = bundle.getBundleContext();
            if (context == null) {
                return null;
            }

            ServiceReference<T> serviceRef = context.getServiceReference(serviceClass);
            if (serviceRef == null) {
                return null;
            }

            return context.getService(serviceRef);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get an OSGi service by class name.
     *
     * @param serviceClassName fully qualified service class name
     * @return service instance or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(String serviceClassName) {
        try {
            Class<T> serviceClass = (Class<T>) Class.forName(serviceClassName);
            return getService(serviceClass);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
