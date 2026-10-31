/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2026 Cloudempiere                                            *
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
package com.cloudempiere.ai.form;

import org.adempiere.webui.factory.IMappedFormFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * Registers the {@code @Form} classes of this package with the mapped form factory (CLD-2438).
 *
 * <p>Replaces: nothing; new form.
 * <p>Mechanism: form factory, annotation scan -
 * https://docs.idempiere.org/docs/new-features/v9/osgi-new-form-factory. The factory is a mandatory reference, so
 * the scan runs only once it is there ({@code Extensions.getMappedFormFactory()} in an activator can still be null).
 * <p>With this bundle stopped: the AI Assistant menu entry finds no form class.
 */
@Component(immediate = true)
public class AIChatFormRegistrar {

	@Reference(service = IMappedFormFactory.class, cardinality = ReferenceCardinality.MANDATORY)
	private IMappedFormFactory formFactory;

	@Activate
	void activate(BundleContext context) {
		formFactory.scan(context, AIChatFormRegistrar.class.getPackageName());
	}
}
