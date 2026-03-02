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
package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

/**
 * AI Chat Ownership Model - Business Logic
 * Implements ADR-036: Chat Ownership and Sharing Model
 *
 * @author Cloudempiere
 */
public class MAIChatOwnership extends X_AIG_ChatOwnership {

    private static final long serialVersionUID = 1L;

    /**
     * Standard Constructor
     * @param ctx context
     * @param AIG_ChatOwnership_ID id
     * @param trxName transaction
     */
    public MAIChatOwnership(Properties ctx, int AIG_ChatOwnership_ID, String trxName) {
        super(ctx, AIG_ChatOwnership_ID, trxName);
    }

    /**
     * Standard Constructor with virtual columns
     * @param ctx context
     * @param AIG_ChatOwnership_ID id
     * @param trxName transaction
     * @param virtualColumns virtual columns
     */
    public MAIChatOwnership(Properties ctx, int AIG_ChatOwnership_ID, String trxName, String... virtualColumns) {
        super(ctx, AIG_ChatOwnership_ID, trxName, virtualColumns);
    }

    /**
     * Load Constructor
     * @param ctx context
     * @param rs result set
     * @param trxName transaction
     */
    public MAIChatOwnership(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Get all ownership records for a chat
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return list of ownership records
     */
    public static List<MAIChatOwnership> getForChat(Properties ctx, int CM_Chat_ID, String trxName) {
        String whereClause = COLUMNNAME_CM_Chat_ID + "=? AND " + COLUMNNAME_IsActive + "='Y'";
        return new Query(ctx, Table_Name, whereClause, trxName)
                .setParameters(CM_Chat_ID)
                .setClient_ID()
                .list();
    }

    /**
     * Get ownership record for a specific user on a chat
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user ID
     * @param trxName transaction
     * @return ownership record or null if not found
     */
    public static MAIChatOwnership getForUser(Properties ctx, int CM_Chat_ID, int AD_User_ID, String trxName) {
        String whereClause = COLUMNNAME_CM_Chat_ID + "=? AND "
                + COLUMNNAME_AD_User_ID + "=? AND "
                + COLUMNNAME_IsActive + "='Y'";
        return new Query(ctx, Table_Name, whereClause, trxName)
                .setParameters(CM_Chat_ID, AD_User_ID)
                .setClient_ID()
                .first();
    }

    /**
     * Get all ownership records for a specific user (all chats shared with them)
     * @param ctx context
     * @param AD_User_ID user ID
     * @param trxName transaction
     * @return list of ownership records
     */
    public static List<MAIChatOwnership> getSharedWithUser(Properties ctx, int AD_User_ID, String trxName) {
        String whereClause = COLUMNNAME_AD_User_ID + "=? AND " + COLUMNNAME_IsActive + "='Y'";
        return new Query(ctx, Table_Name, whereClause, trxName)
                .setParameters(AD_User_ID)
                .setClient_ID()
                .list();
    }

    /**
     * Get all ownership records for a specific role on a chat
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_Role_ID role ID
     * @param trxName transaction
     * @return ownership record or null if not found
     */
    public static MAIChatOwnership getForRole(Properties ctx, int CM_Chat_ID, int AD_Role_ID, String trxName) {
        String whereClause = COLUMNNAME_CM_Chat_ID + "=? AND "
                + COLUMNNAME_AD_Role_ID + "=? AND "
                + COLUMNNAME_IsActive + "='Y'";
        return new Query(ctx, Table_Name, whereClause, trxName)
                .setParameters(CM_Chat_ID, AD_Role_ID)
                .setClient_ID()
                .first();
    }

    /**
     * Get all role-based ownership records for roles the user has
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param roleIds list of role IDs the user has
     * @param trxName transaction
     * @return list of ownership records
     */
    public static List<MAIChatOwnership> getForRoles(Properties ctx, int CM_Chat_ID,
            List<Integer> roleIds, String trxName) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(COLUMNNAME_CM_Chat_ID).append("=? AND ");
        sb.append(COLUMNNAME_AD_Role_ID).append(" IN (");
        for (int i = 0; i < roleIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(") AND ").append(COLUMNNAME_IsActive).append("='Y'");

        Object[] params = new Object[roleIds.size() + 1];
        params[0] = CM_Chat_ID;
        for (int i = 0; i < roleIds.size(); i++) {
            params[i + 1] = roleIds.get(i);
        }

        return new Query(ctx, Table_Name, sb.toString(), trxName)
                .setParameters(params)
                .setClient_ID()
                .list();
    }

    // ========================================================================
    // Business Logic
    // ========================================================================

    /**
     * Check if this ownership grant is currently valid (time-bound check)
     * @return true if valid (no time bounds or within bounds)
     */
    public boolean isCurrentlyValid() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Timestamp validFrom = getValidFrom();
        if (validFrom != null && now.before(validFrom)) {
            return false; // Not yet valid
        }

        Timestamp validTo = getValidTo();
        if (validTo != null && now.after(validTo)) {
            return false; // Expired
        }

        return true;
    }

    /**
     * Check if this is an owner-level access
     * @return true if owner
     */
    public boolean isOwner() {
        return OWNERSHIPTYPE_Owner.equals(getOwnershipType());
    }

    /**
     * Check if this grants write access
     * @return true if write or owner
     */
    public boolean hasWriteAccess() {
        String type = getOwnershipType();
        return OWNERSHIPTYPE_Owner.equals(type) || OWNERSHIPTYPE_Write.equals(type);
    }

    /**
     * Check if this grants read access
     * @return true if any access type (read, write, or owner)
     */
    public boolean hasReadAccess() {
        String type = getOwnershipType();
        return OWNERSHIPTYPE_Owner.equals(type)
                || OWNERSHIPTYPE_Write.equals(type)
                || OWNERSHIPTYPE_Read.equals(type);
    }

    // ========================================================================
    // Static Factory Methods for Creating Grants
    // ========================================================================

    /**
     * Create an owner grant for a user
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user to grant ownership
     * @param sharedByUserId user who is granting (null for system)
     * @param trxName transaction
     * @return created ownership record
     */
    public static MAIChatOwnership createOwnerGrant(Properties ctx, int CM_Chat_ID,
            int AD_User_ID, Integer sharedByUserId, String trxName) {
        return createGrant(ctx, CM_Chat_ID, AD_User_ID, 0, OWNERSHIPTYPE_Owner,
                sharedByUserId, null, null, trxName);
    }

    /**
     * Create a read grant for a user
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user to grant access
     * @param sharedByUserId user who is sharing
     * @param trxName transaction
     * @return created ownership record
     */
    public static MAIChatOwnership createReadGrant(Properties ctx, int CM_Chat_ID,
            int AD_User_ID, int sharedByUserId, String trxName) {
        return createGrant(ctx, CM_Chat_ID, AD_User_ID, 0, OWNERSHIPTYPE_Read,
                sharedByUserId, null, null, trxName);
    }

    /**
     * Create a write grant for a user
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user to grant access
     * @param sharedByUserId user who is sharing
     * @param trxName transaction
     * @return created ownership record
     */
    public static MAIChatOwnership createWriteGrant(Properties ctx, int CM_Chat_ID,
            int AD_User_ID, int sharedByUserId, String trxName) {
        return createGrant(ctx, CM_Chat_ID, AD_User_ID, 0, OWNERSHIPTYPE_Write,
                sharedByUserId, null, null, trxName);
    }

    /**
     * Create a role-based grant
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_Role_ID role to grant access
     * @param ownershipType type of access (R, W, O)
     * @param sharedByUserId user who is sharing
     * @param trxName transaction
     * @return created ownership record
     */
    public static MAIChatOwnership createRoleGrant(Properties ctx, int CM_Chat_ID,
            int AD_Role_ID, String ownershipType, int sharedByUserId, String trxName) {
        return createGrant(ctx, CM_Chat_ID, 0, AD_Role_ID, ownershipType,
                sharedByUserId, null, null, trxName);
    }

    /**
     * Create a time-bound grant
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user to grant access
     * @param ownershipType type of access (R, W, O)
     * @param sharedByUserId user who is sharing
     * @param validFrom valid from timestamp (can be null)
     * @param validTo valid until timestamp (can be null)
     * @param trxName transaction
     * @return created ownership record
     */
    public static MAIChatOwnership createTimeBoundGrant(Properties ctx, int CM_Chat_ID,
            int AD_User_ID, String ownershipType, int sharedByUserId,
            Timestamp validFrom, Timestamp validTo, String trxName) {
        return createGrant(ctx, CM_Chat_ID, AD_User_ID, 0, ownershipType,
                sharedByUserId, validFrom, validTo, trxName);
    }

    /**
     * Internal method to create a grant
     */
    private static MAIChatOwnership createGrant(Properties ctx, int CM_Chat_ID,
            int AD_User_ID, int AD_Role_ID, String ownershipType,
            Integer sharedByUserId, Timestamp validFrom, Timestamp validTo, String trxName) {

        MAIChatOwnership ownership = new MAIChatOwnership(ctx, 0, trxName);
        ownership.setCM_Chat_ID(CM_Chat_ID);

        if (AD_User_ID > 0) {
            ownership.setAD_User_ID(AD_User_ID);
        }
        if (AD_Role_ID > 0) {
            ownership.setAD_Role_ID(AD_Role_ID);
        }

        ownership.setOwnershipType(ownershipType);

        if (sharedByUserId != null && sharedByUserId > 0) {
            ownership.setSharedBy_User_ID(sharedByUserId);
        }

        if (validFrom != null) {
            ownership.setValidFrom(validFrom);
        }
        if (validTo != null) {
            ownership.setValidTo(validTo);
        }

        ownership.saveEx();
        return ownership;
    }

    /**
     * Revoke this access grant
     */
    public void revoke() {
        setIsActive(false);
        saveEx();
    }

    /**
     * Delete all ownership records for a chat (used when deleting chat)
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return number of records deleted
     */
    public static int deleteForChat(Properties ctx, int CM_Chat_ID, String trxName) {
        List<MAIChatOwnership> ownerships = getForChat(ctx, CM_Chat_ID, trxName);
        for (MAIChatOwnership ownership : ownerships) {
            ownership.deleteEx(true);
        }
        return ownerships.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MAIChatOwnership[");
        sb.append(get_ID());
        sb.append(", Chat=").append(getCM_Chat_ID());
        if (getAD_User_ID() > 0) {
            sb.append(", User=").append(getAD_User_ID());
        }
        if (getAD_Role_ID() > 0) {
            sb.append(", Role=").append(getAD_Role_ID());
        }
        sb.append(", Type=").append(getOwnershipType());
        if (!isCurrentlyValid()) {
            sb.append(" (EXPIRED)");
        }
        sb.append("]");
        return sb.toString();
    }
}
