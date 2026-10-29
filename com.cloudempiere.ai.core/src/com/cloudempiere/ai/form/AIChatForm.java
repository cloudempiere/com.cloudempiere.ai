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

import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.idempiere.ui.zk.annotation.Form;

import com.cloudempiere.ai.component.AIChatWidget;

/**
 * The AI chat as an iDempiere form (AD_Form "AI Assistant", CLD-2438).
 *
 * <p>One component for every entry point: the help panel embeds it ({@code AIChatGadgetFactory}), the menu opens it
 * as a tab, and later callers open it with {@code ADForm.openForm(formId, gridTab)} to give the chat their record.
 * Opening it from the menu follows the role's form access; the help panel keeps the AI provider access as its gate.
 *
 * <p>Registered by {@link AIChatFormRegistrar}.
 */
@Form
public class AIChatForm extends ADForm {

	private static final long serialVersionUID = 1L;

	private AIChatWidget chatWidget;

	@Override
	protected void initForm() {
		chatWidget = new AIChatWidget(true);
		ZKUpdateUtil.setVflex(chatWidget, "1");
		ZKUpdateUtil.setHflex(chatWidget, "1");
		appendChild(chatWidget);
		ZKUpdateUtil.setHeight(this, "100%");
	}

	/**
	 * @return the chat shown by this form
	 */
	public AIChatWidget getChatWidget() {
		return chatWidget;
	}
}
