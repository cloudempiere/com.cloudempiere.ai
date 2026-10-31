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
package com.cloudempiere.ai.guardrails.detect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks internal iDempiere ID references (AD_Client_ID, AD_User_ID, etc.) that
 * shouldn't be exposed to end users.
 */
public final class InternalIdMasker {

    private static final Pattern INTERNAL_ID_PATTERN = Pattern.compile(
        "\\b(AD_Client_ID|AD_Org_ID|AD_User_ID|AD_Role_ID|AD_Table_ID|AD_Column_ID)\\s*[:=]\\s*\\d+");

    public static final class Result {
        public final boolean wasMasked;
        public final String maskedOutput;

        Result(boolean wasMasked, String maskedOutput) {
            this.wasMasked = wasMasked;
            this.maskedOutput = maskedOutput;
        }
    }

    private InternalIdMasker() {
    }

    public static Result mask(String output) {
        Matcher m = INTERNAL_ID_PATTERN.matcher(output);
        if (!m.find()) {
            return new Result(false, output);
        }
        return new Result(true, m.replaceAll("[INTERNAL_REF]"));
    }
}
