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
package com.cloudempiere.ai.kb.dto;

/**
 * Represents a knowledge base entry (k_entry)
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseEntry {
    private int k_entry_id;
    private String name;
    private String title;
    private String description;
    private String content; // Original content (editor.js or markdown)
    private String contentMarkdown; // Parsed markdown
    private int parent_id;
    private int sequence_no;
    private String k_type; // Knowledge base type
    private boolean is_active;
    private String language;

    public KnowledgeBaseEntry() {
    }

    public KnowledgeBaseEntry(int k_entry_id, String name, String title) {
        this.k_entry_id = k_entry_id;
        this.name = name;
        this.title = title;
    }

    // Getters and Setters
    public int getK_entry_id() {
        return k_entry_id;
    }

    public void setK_entry_id(int k_entry_id) {
        this.k_entry_id = k_entry_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public int getParent_id() {
        return parent_id;
    }

    public void setParent_id(int parent_id) {
        this.parent_id = parent_id;
    }

    public int getSequence_no() {
        return sequence_no;
    }

    public void setSequence_no(int sequence_no) {
        this.sequence_no = sequence_no;
    }

    public String getK_type() {
        return k_type;
    }

    public void setK_type(String k_type) {
        this.k_type = k_type;
    }

    public boolean isIs_active() {
        return is_active;
    }

    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get hierarchy path (breadcrumb) - populated by context provider
     */
    public String getHierarchyPath() {
        if (parent_id > 0) {
            return "[Parent: " + parent_id + "] " + title;
        }
        return title;
    }

    @Override
    public String toString() {
        return "KnowledgeBaseEntry{" +
                "k_entry_id=" + k_entry_id +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", parent_id=" + parent_id +
                ", sequence_no=" + sequence_no +
                ", k_type='" + k_type + '\'' +
                ", is_active=" + is_active +
                '}';
    }
}
