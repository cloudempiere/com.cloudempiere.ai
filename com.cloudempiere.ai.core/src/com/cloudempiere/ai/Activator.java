package com.cloudempiere.ai;

import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.idempiere.model.IMappedModelFactory;
import org.idempiere.process.IMappedProcessFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(immediate = true)
public class Activator extends Incremental2PackActivator {

    @Reference(service = IMappedModelFactory.class, cardinality = ReferenceCardinality.MANDATORY)
    private IMappedModelFactory mappedModelFactory;
    @Reference(service = IMappedProcessFactory.class, cardinality = ReferenceCardinality.MANDATORY)
    private IMappedProcessFactory mappedProcessFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    @Activate
    public void activate(BundleContext context) {
        try {
            mappedModelFactory.scan(context, "com.cloudempiere.ai.model");
            mappedProcessFactory.scan(context, "com.cloudempiere.ai.process");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
