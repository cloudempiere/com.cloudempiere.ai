package com.cloudempiere.ai;

import org.adempiere.base.Core;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.osgi.framework.BundleContext;

public class Activator extends Incremental2PackActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("=== CloudEmpiere AI Activator START ===");

		try {
			System.out.println("Scanning models: com.cloudempiere.ai.model");
			Core.getMappedModelFactory().scan(context, "com.cloudempiere.ai.model");
			System.out.println("✓ Model scan complete");
		} catch (Exception e) {
			System.err.println("✗ Model scan FAILED: " + e.getMessage());
			e.printStackTrace();
		}

		try {
			System.out.println("Scanning processes: com.cloudempiere.ai.process");
			Core.getMappedProcessFactory().scan(context, "com.cloudempiere.ai.process");
			System.out.println("✓ Process scan complete");
		} catch (Exception e) {
			System.err.println("✗ Process scan FAILED: " + e.getMessage());
			e.printStackTrace();
		}

		super.start(context);
		System.out.println("=== CloudEmpiere AI Activator COMPLETE ===");
	}

}
