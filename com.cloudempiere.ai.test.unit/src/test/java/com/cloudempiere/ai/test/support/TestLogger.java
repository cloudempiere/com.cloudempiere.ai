/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.test.support;

/**
 * Test logging utility for verbose test output.
 *
 * Enable verbose logging by running tests with -Dtest.verbose=true
 * or using ./run-unit-tests.sh -v
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class TestLogger {

    private static final boolean VERBOSE = Boolean.getBoolean("test.verbose");
    private static final boolean DEBUG = Boolean.getBoolean("test.debug");

    private final String testClass;
    private String currentTest;

    public TestLogger(Class<?> testClass) {
        this.testClass = testClass.getSimpleName();
    }

    /**
     * Check if verbose logging is enabled
     */
    public static boolean isVerbose() {
        return VERBOSE;
    }

    /**
     * Check if debug logging is enabled
     */
    public static boolean isDebug() {
        return DEBUG;
    }

    /**
     * Set current test name for context
     */
    public void setCurrentTest(String testName) {
        this.currentTest = testName;
    }

    /**
     * Log info message (only when verbose)
     */
    public void info(String message) {
        if (VERBOSE) {
            System.out.println("[INFO] " + prefix() + message);
        }
    }

    /**
     * Log info message with format args (only when verbose)
     */
    public void info(String format, Object... args) {
        if (VERBOSE) {
            System.out.println("[INFO] " + prefix() + String.format(format, args));
        }
    }

    /**
     * Log debug message (only when debug enabled)
     */
    public void debug(String message) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + prefix() + message);
        }
    }

    /**
     * Log debug message with format args (only when debug enabled)
     */
    public void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + prefix() + String.format(format, args));
        }
    }

    /**
     * Log a check being performed
     */
    public void check(String description, Object input, Object expected) {
        if (VERBOSE) {
            System.out.println("[CHECK] " + prefix() + description);
            System.out.println("        Input:    " + truncate(String.valueOf(input), 100));
            System.out.println("        Expected: " + truncate(String.valueOf(expected), 100));
        }
    }

    /**
     * Log a check result
     */
    public void result(String description, Object actual, boolean passed) {
        if (VERBOSE) {
            String status = passed ? "PASS" : "FAIL";
            System.out.println("[" + status + "] " + prefix() + description);
            System.out.println("        Actual:   " + truncate(String.valueOf(actual), 100));
        }
    }

    /**
     * Log input being tested
     */
    public void input(String label, Object value) {
        if (VERBOSE) {
            System.out.println("[INPUT] " + prefix() + label + ": " + truncate(String.valueOf(value), 150));
        }
    }

    /**
     * Log output/result
     */
    public void output(String label, Object value) {
        if (VERBOSE) {
            System.out.println("[OUTPUT] " + prefix() + label + ": " + truncate(String.valueOf(value), 150));
        }
    }

    /**
     * Log a variable value
     */
    public void var(String name, Object value) {
        if (DEBUG) {
            System.out.println("[VAR] " + prefix() + name + " = " + truncate(String.valueOf(value), 200));
        }
    }

    /**
     * Log section header
     */
    public void section(String title) {
        if (VERBOSE) {
            System.out.println();
            System.out.println("=== " + title + " ===");
        }
    }

    /**
     * Log test start
     */
    public void testStart(String testName) {
        this.currentTest = testName;
        if (VERBOSE) {
            System.out.println();
            System.out.println(">>> Starting: " + testClass + "." + testName);
        }
    }

    /**
     * Log test end
     */
    public void testEnd(boolean passed) {
        if (VERBOSE) {
            String status = passed ? "PASSED" : "FAILED";
            System.out.println("<<< " + status + ": " + testClass + "." + currentTest);
        }
    }

    private String prefix() {
        if (currentTest != null) {
            return "[" + testClass + "." + currentTest + "] ";
        }
        return "[" + testClass + "] ";
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
