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
package com.cloudempiere.ai.factory;

import org.adempiere.webui.factory.IAIChatWidgetFactory;
import org.osgi.service.component.annotations.Component;
import org.zkoss.zul.Div;

import org.compiere.util.Env;

import com.cloudempiere.ai.component.AIChatWidget;
import com.cloudempiere.ai.model.MAIProvider;

/**
 * OSGi Service implementation for AI Chat Widget Factory
 *
 * This factory is registered as an OSGi service and consumed by the UI plugin,
 * avoiding circular dependency issues.
 *
 * @author Cloudempiere
 */
@Component(
	service = IAIChatWidgetFactory.class,
	immediate = true,
	property = {
		"service.ranking:Integer=10"
	}
)
public class AIChatWidgetFactory implements IAIChatWidgetFactory {

	@Override
	public Div createChatWidget() {
		return createChatWidget(true);
	}

	@Override
	public Div createChatWidget(boolean enableContext) {
		return new AIChatWidget(enableContext);
	}

	@Override
	public boolean isAvailable() {
		// Check if a default AI provider is configured in the database
		// Uses LangChain4j path which supports all provider types (ANT, ABE, OLL, OAI, LLA)
		MAIProvider provider = MAIProvider.getDefault(Env.getCtx(), null);
		return provider != null && provider.isActive();
	}

	@Override
	public int getPriority() {
		return 10;
	}
}