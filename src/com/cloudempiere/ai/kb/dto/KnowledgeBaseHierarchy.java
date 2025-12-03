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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the knowledge base hierarchy (navigation tree)
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseHierarchy {
    private String k_type; // Knowledge base type
    private List<KnowledgeBaseEntry> allEntries; // All entries flat list
    private Map<Integer, KnowledgeBaseEntry> entriesById;
    private Map<Integer, List<KnowledgeBaseEntry>> childrenByParent;
    private List<KnowledgeBaseEntry> rootEntries; // Top-level entries

    public KnowledgeBaseHierarchy(String k_type) {
        this.k_type = k_type;
        this.allEntries = new ArrayList<>();
        this.entriesById = new HashMap<>();
        this.childrenByParent = new HashMap<>();
        this.rootEntries = new ArrayList<>();
    }

    /**
     * Add entry to hierarchy
     */
    public void addEntry(KnowledgeBaseEntry entry) {
        allEntries.add(entry);
        entriesById.put(entry.getK_entry_id(), entry);

        int parentId = entry.getParent_id();
        if (parentId <= 0) {
            // Root entry
            rootEntries.add(entry);
        } else {
            // Child entry
            childrenByParent
                .computeIfAbsent(parentId, k -> new ArrayList<>())
                .add(entry);
        }
    }

    /**
     * Get children of an entry
     */
    public List<KnowledgeBaseEntry> getChildren(int parentId) {
        return childrenByParent.getOrDefault(parentId, new ArrayList<>());
    }

    /**
     * Get entry by ID
     */
    public KnowledgeBaseEntry getEntry(int id) {
        return entriesById.get(id);
    }

    /**
     * Get breadcrumb path for an entry
     */
    public List<KnowledgeBaseEntry> getBreadcrumbPath(int entryId) {
        List<KnowledgeBaseEntry> path = new ArrayList<>();
        KnowledgeBaseEntry entry = entriesById.get(entryId);

        while (entry != null) {
            path.add(0, entry); // Add to front
            entry = entriesById.get(entry.getParent_id());
        }

        return path;
    }

    /**
     * Get depth of entry in hierarchy
     */
    public int getDepth(int entryId) {
        int depth = 0;
        KnowledgeBaseEntry entry = entriesById.get(entryId);

        while (entry != null && entry.getParent_id() > 0) {
            depth++;
            entry = entriesById.get(entry.getParent_id());
        }

        return depth;
    }

    // Getters
    public String getK_type() {
        return k_type;
    }

    public List<KnowledgeBaseEntry> getAllEntries() {
        return allEntries;
    }

    public List<KnowledgeBaseEntry> getRootEntries() {
        return rootEntries;
    }

    public int getTotalEntries() {
        return allEntries.size();
    }

    /**
     * Build a simple tree structure as string (for debugging)
     */
    public String getTreeStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("Knowledge Base: ").append(k_type).append("\n");

        for (KnowledgeBaseEntry root : rootEntries) {
            buildTreeString(sb, root, 0);
        }

        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, KnowledgeBaseEntry entry, int depth) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append("├─ ").append(entry.getTitle()).append("\n");

        for (KnowledgeBaseEntry child : getChildren(entry.getK_entry_id())) {
            buildTreeString(sb, child, depth + 1);
        }
    }
}
