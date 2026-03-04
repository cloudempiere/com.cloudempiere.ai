package com.cloudempiere.ai;

import org.adempiere.base.Core;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.osgi.framework.BundleContext;

public class Activator extends Incremental2PackActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		try {
			Core.getMappedModelFactory().scan(context, "com.cloudempiere.ai.model");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Core.getMappedProcessFactory().scan(context, "com.cloudempiere.ai.process");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
