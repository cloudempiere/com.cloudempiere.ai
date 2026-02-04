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
package com.cloudempiere.ai.event;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.cloudempiere.ai.model.I_AIG_Budget;
import com.cloudempiere.ai.model.MAIBudget;
import com.cloudempiere.ai.observability.CostGuard;

/**
 * Event Handler for AIG_Budget table changes.
 *
 * <p>Clears budget caches when budget records are created, modified, or deleted
 * to ensure CostGuard always uses current budget limits.
 *
 * <p><b>Tags:</b> #event-handler #cache #budget #observability
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.14.0
 * @see MAIBudget
 * @see CostGuard
 */
@Component(
    reference = @Reference(
        name = "IEventManager",
        bind = "bindEventManager",
        unbind = "unbindEventManager",
        policy = ReferencePolicy.STATIC,
        cardinality = ReferenceCardinality.MANDATORY,
        service = IEventManager.class
    )
)
public class AIBudgetEventHandler extends AbstractEventHandler {

    private static final CLogger log = CLogger.getCLogger(AIBudgetEventHandler.class);

    @Override
    protected void initialize() {
        // Register for AIG_Budget table events
        registerTableEvent(IEventTopics.PO_AFTER_NEW, I_AIG_Budget.Table_Name);
        registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_AIG_Budget.Table_Name);
        registerTableEvent(IEventTopics.PO_AFTER_DELETE, I_AIG_Budget.Table_Name);

        log.info("AIBudgetEventHandler initialized - listening for AIG_Budget changes");
    }

    @Override
    protected void doHandleEvent(Event event) {
        PO po = getPO(event);
        if (po == null) {
            return;
        }

        String topic = event.getTopic();
        int clientId = po.getAD_Client_ID();

        log.fine("AIG_Budget changed (topic=" + topic + ", clientId=" + clientId + "), clearing caches");

        // Clear MAIBudget CCache
        MAIBudget.clearCache();

        // Clear CostGuard static cache for this client
        CostGuard.clearBudgetCache(clientId);

        log.info("Budget caches cleared for client " + clientId + " (event: " + topic + ")");
    }
}
