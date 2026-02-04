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
package com.cloudempiere.ai.util;

/**
 * Instance-based configuration for markdown sanitization.
 *
 * <p><b>Purpose:</b> Provide per-provider, multi-tenant-safe configuration for
 * {@link MarkdownSyntaxSanitizer}. Replaces static configuration to enable:
 * <ul>
 *   <li>Different settings per AI provider (Anthropic vs Bedrock vs Ollama)</li>
 *   <li>Tenant-specific security policies</li>
 *   <li>Runtime configuration changes without global side effects</li>
 * </ul>
 *
 * <p><b>Multi-Tenancy:</b> Each {@code MarkdownConfig} instance is isolated.
 * Changes to one tenant's config do not affect other tenants.
 *
 * <p><b>Usage:</b>
 * <pre>
 * // Create provider-specific config
 * MarkdownConfig config = MarkdownConfig.builder()
 *     .allowBase64Images(false)  // Strict security
 *     .maxTableRows(100)
 *     .maxTableColumns(20)
 *     .maxCodeBlockLines(50)
 *     .build();
 *
 * // Use with sanitizer
 * MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
 * String safe = sanitizer.sanitize(aiResponse);
 * </pre>
 *
 * <p><b>Future Enhancement:</b> Load configuration from {@code MAIProvider} database
 * table to enable UI-based configuration management.
 *
 * @see MarkdownSyntaxSanitizer
 * @author Cloudempiere
 * @version 1.0
 * @since v0.32.0
 */
public class MarkdownConfig {

    // ============================================================================
    // Configuration Fields (Instance-based, NOT static)
    // ============================================================================

    /** Allow base64 data URLs in images (default: false for security) */
    private final boolean allowBase64Images;

    /** Maximum table rows to prevent DoS attacks (default: 100) */
    private final int maxTableRows;

    /** Maximum table columns to prevent DoS attacks (default: 20) */
    private final int maxTableColumns;

    /** Maximum code block lines to prevent DoS attacks (default: 50) */
    private final int maxCodeBlockLines;

    // ============================================================================
    // Constructors
    // ============================================================================

    /**
     * Create configuration with default values (secure defaults).
     */
    public MarkdownConfig() {
        this(false, 100, 20, 50);
    }

    /**
     * Create configuration with custom values.
     *
     * @param allowBase64Images allow data: URLs in images (security risk - only enable if SVG sanitization available)
     * @param maxTableRows maximum table rows (prevents DoS)
     * @param maxTableColumns maximum table columns (prevents DoS)
     * @param maxCodeBlockLines maximum code block lines (prevents DoS)
     */
    public MarkdownConfig(boolean allowBase64Images, int maxTableRows, int maxTableColumns, int maxCodeBlockLines) {
        this.allowBase64Images = allowBase64Images;
        this.maxTableRows = maxTableRows;
        this.maxTableColumns = maxTableColumns;
        this.maxCodeBlockLines = maxCodeBlockLines;
    }

    // ============================================================================
    // Builder Pattern (Fluent API)
    // ============================================================================

    /**
     * Create a builder for fluent configuration.
     *
     * @return new builder with default values
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for MarkdownConfig (fluent API).
     */
    public static class Builder {
        private boolean allowBase64Images = false;
        private int maxTableRows = 100;
        private int maxTableColumns = 20;
        private int maxCodeBlockLines = 50;

        /**
         * Allow base64 data URLs in images.
         *
         * <p><b>Security Warning:</b> Base64 images can contain malicious SVG.
         * Only enable if you have SVG sanitization in place.
         *
         * @param allow true to allow data: URLs
         * @return this builder
         */
        public Builder allowBase64Images(boolean allow) {
            this.allowBase64Images = allow;
            return this;
        }

        /**
         * Set maximum table rows (prevents DoS).
         *
         * @param max maximum rows (default: 100)
         * @return this builder
         */
        public Builder maxTableRows(int max) {
            this.maxTableRows = max;
            return this;
        }

        /**
         * Set maximum table columns (prevents DoS).
         *
         * @param max maximum columns (default: 20)
         * @return this builder
         */
        public Builder maxTableColumns(int max) {
            this.maxTableColumns = max;
            return this;
        }

        /**
         * Set maximum code block lines (prevents DoS).
         *
         * @param max maximum lines (default: 50)
         * @return this builder
         */
        public Builder maxCodeBlockLines(int max) {
            this.maxCodeBlockLines = max;
            return this;
        }

        /**
         * Build the configuration instance.
         *
         * @return immutable MarkdownConfig
         */
        public MarkdownConfig build() {
            return new MarkdownConfig(allowBase64Images, maxTableRows, maxTableColumns, maxCodeBlockLines);
        }
    }

    // ============================================================================
    // Getters (Immutable)
    // ============================================================================

    /**
     * Check if base64 data URLs are allowed in images.
     *
     * @return true if data: URLs allowed
     */
    public boolean isBase64ImagesAllowed() {
        return allowBase64Images;
    }

    /**
     * Get maximum table rows.
     *
     * @return maximum rows
     */
    public int getMaxTableRows() {
        return maxTableRows;
    }

    /**
     * Get maximum table columns.
     *
     * @return maximum columns
     */
    public int getMaxTableColumns() {
        return maxTableColumns;
    }

    /**
     * Get maximum code block lines.
     *
     * @return maximum lines
     */
    public int getMaxCodeBlockLines() {
        return maxCodeBlockLines;
    }

    // ============================================================================
    // Presets (Common Configurations)
    // ============================================================================

    /**
     * Get default secure configuration.
     *
     * <p>Recommended for production:
     * <ul>
     *   <li>Base64 images: disabled</li>
     *   <li>Table rows: 100 max</li>
     *   <li>Table columns: 20 max</li>
     *   <li>Code blocks: 50 lines max</li>
     * </ul>
     *
     * @return secure default config
     */
    public static MarkdownConfig secure() {
        return new MarkdownConfig();
    }

    /**
     * Get permissive configuration (development/testing only).
     *
     * <p><b>Warning:</b> Not recommended for production!
     * <ul>
     *   <li>Base64 images: enabled</li>
     *   <li>Table rows: 500 max</li>
     *   <li>Table columns: 50 max</li>
     *   <li>Code blocks: 200 lines max</li>
     * </ul>
     *
     * @return permissive config for development
     */
    public static MarkdownConfig permissive() {
        return new Builder()
            .allowBase64Images(true)
            .maxTableRows(500)
            .maxTableColumns(50)
            .maxCodeBlockLines(200)
            .build();
    }

    /**
     * Get strict configuration (high security).
     *
     * <p>Recommended for sensitive environments:
     * <ul>
     *   <li>Base64 images: disabled</li>
     *   <li>Table rows: 50 max</li>
     *   <li>Table columns: 10 max</li>
     *   <li>Code blocks: 20 lines max</li>
     * </ul>
     *
     * @return strict config for high security
     */
    public static MarkdownConfig strict() {
        return new Builder()
            .allowBase64Images(false)
            .maxTableRows(50)
            .maxTableColumns(10)
            .maxCodeBlockLines(20)
            .build();
    }

    // ============================================================================
    // Object Overrides
    // ============================================================================

    @Override
    public String toString() {
        return "MarkdownConfig{" +
            "allowBase64Images=" + allowBase64Images +
            ", maxTableRows=" + maxTableRows +
            ", maxTableColumns=" + maxTableColumns +
            ", maxCodeBlockLines=" + maxCodeBlockLines +
            '}';
    }
}
