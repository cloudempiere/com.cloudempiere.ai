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
import java.util.List;

/**
 * Recommendation for where to place new content in knowledge base
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class PlacementRecommendation {

    public enum Action {
        CREATE_NEW("Create new entry"),
        EXTEND_EXISTING("Add to existing entry"),
        UPDATE_EXISTING("Update existing entry"),
        REPLACE_EXISTING("Replace existing entry"),
        DUPLICATE_WARNING("Duplicate detected");

        private final String description;

        Action(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private Action action;
    private int targetEntryId; // If extending/updating existing
    private int suggestedParentId; // If creating new
    private int sequenceNo; // Position in parent
    private double confidence; // 0.0 to 1.0
    private String title; // Suggested title for new entry
    private String reasoning; // Why this recommendation
    private List<String> bestPractices; // KB best practices applied
    private String warningMessage; // If duplicate or conflict

    public PlacementRecommendation() {
        this.bestPractices = new ArrayList<>();
    }

    // Getters and Setters
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getTargetEntryId() {
        return targetEntryId;
    }

    public void setTargetEntryId(int targetEntryId) {
        this.targetEntryId = targetEntryId;
    }

    public int getSuggestedParentId() {
        return suggestedParentId;
    }

    public void setSuggestedParentId(int suggestedParentId) {
        this.suggestedParentId = suggestedParentId;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.min(1.0, Math.max(0.0, confidence));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getBestPractices() {
        return bestPractices;
    }

    public void addBestPractice(String practice) {
        this.bestPractices.add(practice);
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    /**
     * Get confidence as percentage
     */
    public int getConfidencePercent() {
        return (int) (confidence * 100);
    }

    /**
     * Determine if this is a high-confidence recommendation
     */
    public boolean isHighConfidence() {
        return confidence >= 0.75;
    }

    @Override
    public String toString() {
        return "PlacementRecommendation{" +
                "action=" + action +
                ", targetEntryId=" + targetEntryId +
                ", suggestedParentId=" + suggestedParentId +
                ", confidence=" + String.format("%.2f", confidence) +
                ", title='" + title + '\'' +
                '}';
    }
}
