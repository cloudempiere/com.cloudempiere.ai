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

import java.util.regex.Pattern;

/**
 * Detects SQL-injection-shaped text.
 *
 * <p>Shared by both {@code InputGuard} and {@code OutputGuard} - previously each had
 * its own copy, and the output-side one only caught DROP/DELETE-WHERE-1=1/TRUNCATE
 * while the input-side one also caught UNION SELECT, EXEC, and comment-based injection.
 * A payload the input guard blocked coming in could pass the output guard undetected
 * if the model echoed it back. One shared pattern means that can't happen again.
 */
public final class SqlInjectionDetector {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(" +
        "'\\s*OR\\s+'?\\d*'?\\s*=\\s*'?\\d*" +
        "|'\\s*OR\\s+'[^']*'\\s*=\\s*'[^']*'" +
        "|;\\s*(DROP|DELETE|TRUNCATE|UPDATE|INSERT|ALTER)\\s+" +
        "|--\\s*$" +
        "|/\\*.*\\*/" +
        "|UNION\\s+(ALL\\s+)?SELECT" +
        "|\\bEXEC(UTE)?\\s*\\(" +
        "|\\bxp_\\w+" +
        ")"
    );

    private SqlInjectionDetector() {
    }

    public static boolean matches(String text) {
        return text != null && SQL_INJECTION_PATTERN.matcher(text).find();
    }
}
