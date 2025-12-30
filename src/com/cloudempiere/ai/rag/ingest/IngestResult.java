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
package com.cloudempiere.ai.rag.ingest;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a knowledge ingestion operation.
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
public class IngestResult {

    private int documentsAdded = 0;
    private int documentsUpdated = 0;
    private int documentsDeleted = 0;
    private int documentsSkipped = 0;
    private long durationMs = 0;
    private boolean success = true;
    private String errorMessage;
    private List<String> errors = new ArrayList<>();

    public IngestResult() {
    }

    public int getDocumentsAdded() {
        return documentsAdded;
    }

    public void setDocumentsAdded(int documentsAdded) {
        this.documentsAdded = documentsAdded;
    }

    public void incrementAdded() {
        this.documentsAdded++;
    }

    public int getDocumentsUpdated() {
        return documentsUpdated;
    }

    public void setDocumentsUpdated(int documentsUpdated) {
        this.documentsUpdated = documentsUpdated;
    }

    public void incrementUpdated() {
        this.documentsUpdated++;
    }

    public int getDocumentsDeleted() {
        return documentsDeleted;
    }

    public void setDocumentsDeleted(int documentsDeleted) {
        this.documentsDeleted = documentsDeleted;
    }

    public int getDocumentsSkipped() {
        return documentsSkipped;
    }

    public void setDocumentsSkipped(int documentsSkipped) {
        this.documentsSkipped = documentsSkipped;
    }

    public void incrementSkipped() {
        this.documentsSkipped++;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * Get total documents processed.
     */
    public int getTotalProcessed() {
        return documentsAdded + documentsUpdated + documentsSkipped;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IngestResult[");
        sb.append("added=").append(documentsAdded);
        sb.append(", updated=").append(documentsUpdated);
        sb.append(", deleted=").append(documentsDeleted);
        sb.append(", skipped=").append(documentsSkipped);
        sb.append(", duration=").append(durationMs).append("ms");
        if (!success) {
            sb.append(", FAILED: ").append(errorMessage);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Create a failed result with error message.
     */
    public static IngestResult failed(String errorMessage) {
        IngestResult result = new IngestResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
