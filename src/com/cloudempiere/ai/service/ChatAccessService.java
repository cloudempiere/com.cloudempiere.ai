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
package com.cloudempiere.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MChat;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUserRoles;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.MAIChatOwnership;
import com.cloudempiere.ai.model.X_AIG_ChatOwnership;

/**
 * Chat Access Service Implementation
 * Implements ADR-036: Chat Ownership and Sharing Model
 *
 * Access Resolution Algorithm:
 * 1. Creator is always owner
 * 2. Check explicit ownership grants (AIG_ChatOwnership by user)
 * 3. Check role-based grants
 * 4. Apply ConfidentialType baseline
 * 5. For context chats, check record access
 *
 * @author Cloudempiere
 */
public class ChatAccessService implements IChatAccessService {

    private static final CLogger log = CLogger.getCLogger(ChatAccessService.class);

    /** ConfidentialType values from CM_Chat */
    private static final String CONFIDENTIAL_PUBLIC = "A";      // Anyone
    private static final String CONFIDENTIAL_CLIENT = "C";      // Company/Client
    private static final String CONFIDENTIAL_INTERNAL = "I";    // Internal (Organization)
    private static final String CONFIDENTIAL_PRIVATE = "P";     // Private (Only Creator)

    /** Singleton instance */
    private static ChatAccessService instance;

    /**
     * Get singleton instance
     * @return ChatAccessService instance
     */
    public static synchronized ChatAccessService get() {
        if (instance == null) {
            instance = new ChatAccessService();
        }
        return instance;
    }

    /** Private constructor for singleton */
    private ChatAccessService() {
    }

    // ========================================================================
    // Access Resolution (ADR-036 Algorithm)
    // ========================================================================

    @Override
    public ChatAccess getAccess(Properties ctx, int CM_Chat_ID, String trxName) {
        MChat chat = new MChat(ctx, CM_Chat_ID, trxName);
        if (chat.get_ID() == 0) {
            return ChatAccess.NONE;
        }
        return getAccess(ctx, chat);
    }

    @Override
    public ChatAccess getAccess(Properties ctx, MChat chat) {
        int userId = Env.getAD_User_ID(ctx);
        int clientId = Env.getAD_Client_ID(ctx);
        int orgId = Env.getAD_Org_ID(ctx);
        int roleId = Env.getAD_Role_ID(ctx);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Resolving access for Chat=" + chat.get_ID()
                    + ", User=" + userId + ", Client=" + clientId
                    + ", Org=" + orgId + ", Role=" + roleId);
        }

        // Multi-tenancy check: chat must be in same client
        if (chat.getAD_Client_ID() != clientId && clientId != 0) {
            log.fine("Access denied: different client");
            return ChatAccess.NONE;
        }

        // 1. Creator is always owner
        if (chat.getCreatedBy() == userId) {
            log.fine("Access granted: creator is owner");
            return ChatAccess.OWNER;
        }

        // 2. Check explicit ownership grants (AIG_ChatOwnership)
        ChatAccess explicitAccess = checkExplicitOwnership(ctx, chat.get_ID(), userId);
        if (explicitAccess != ChatAccess.NONE) {
            log.fine("Access granted via explicit ownership: " + explicitAccess);
            return explicitAccess;
        }

        // 3. Check role-based grants
        ChatAccess roleAccess = checkRoleOwnership(ctx, chat.get_ID(), roleId, userId);
        if (roleAccess != ChatAccess.NONE) {
            log.fine("Access granted via role: " + roleAccess);
            return roleAccess;
        }

        // 4. Apply ConfidentialType baseline
        String confidential = chat.getConfidentialType();
        ChatAccess confidentialAccess = resolveConfidentialAccess(confidential, chat, clientId, orgId);
        if (confidentialAccess != ChatAccess.NONE) {
            log.fine("Access granted via ConfidentialType '" + confidential + "': " + confidentialAccess);
            return confidentialAccess;
        }

        // 5. For context chats, check record access
        if (chat.getAD_Table_ID() > 0 && chat.getRecord_ID() > 0) {
            if (hasRecordAccess(ctx, chat.getAD_Table_ID(), chat.getRecord_ID())) {
                log.fine("Access granted via record access");
                return ChatAccess.READ; // Can view record = can view chat
            }
        }

        log.fine("Access denied: no matching rule");
        return ChatAccess.NONE;
    }

    /**
     * Check explicit user ownership grants
     */
    private ChatAccess checkExplicitOwnership(Properties ctx, int CM_Chat_ID, int userId) {
        MAIChatOwnership ownership = MAIChatOwnership.getForUser(ctx, CM_Chat_ID, userId, null);

        if (ownership == null || !ownership.isCurrentlyValid()) {
            return ChatAccess.NONE;
        }

        return mapOwnershipTypeToAccess(ownership.getOwnershipType());
    }

    /**
     * Check role-based ownership grants
     */
    private ChatAccess checkRoleOwnership(Properties ctx, int CM_Chat_ID, int currentRoleId, int userId) {
        // Get all roles the user has
        List<Integer> userRoleIds = getUserRoleIds(ctx, userId);

        if (userRoleIds.isEmpty()) {
            return ChatAccess.NONE;
        }

        List<MAIChatOwnership> roleOwnerships = MAIChatOwnership.getForRoles(ctx, CM_Chat_ID, userRoleIds, null);

        ChatAccess bestAccess = ChatAccess.NONE;
        for (MAIChatOwnership ownership : roleOwnerships) {
            if (ownership.isCurrentlyValid()) {
                ChatAccess access = mapOwnershipTypeToAccess(ownership.getOwnershipType());
                if (access.ordinal() > bestAccess.ordinal()) {
                    bestAccess = access;
                }
            }
        }

        return bestAccess;
    }

    /**
     * Resolve access based on ConfidentialType
     */
    private ChatAccess resolveConfidentialAccess(String confidential, MChat chat, int clientId, int orgId) {
        if (CONFIDENTIAL_PUBLIC.equals(confidential)) {
            return ChatAccess.READ; // Anyone in client
        } else if (CONFIDENTIAL_CLIENT.equals(confidential) && chat.getAD_Client_ID() == clientId) {
            return ChatAccess.READ; // Same client
        } else if (CONFIDENTIAL_INTERNAL.equals(confidential)) {
            // Same organization or child organization
            if (chat.getAD_Org_ID() == orgId || chat.getAD_Org_ID() == 0) {
                return ChatAccess.READ;
            }
        }
        // CONFIDENTIAL_PRIVATE only creator has access (handled above)
        return ChatAccess.NONE;
    }

    /**
     * Check if user has access to the underlying record
     */
    private boolean hasRecordAccess(Properties ctx, int AD_Table_ID, int Record_ID) {
        try {
            MTable table = MTable.get(ctx, AD_Table_ID);
            if (table == null) {
                return false;
            }

            // Use role-based access check
            MRole role = MRole.get(ctx, Env.getAD_Role_ID(ctx));
            if (role == null) {
                return false;
            }

            // Check table access
            if (!role.isTableAccess(AD_Table_ID, true)) { // true = read-only
                return false;
            }

            // Try to load the record (will respect role access)
            Object po = table.getPO(Record_ID, null);
            return po != null;
        } catch (Exception e) {
            log.log(Level.WARNING, "Error checking record access: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all role IDs for a user
     */
    private List<Integer> getUserRoleIds(Properties ctx, int userId) {
        List<Integer> roleIds = new ArrayList<>();
        int clientId = Env.getAD_Client_ID(ctx);

        String whereClause = "AD_User_ID=? AND AD_Client_ID IN (0, ?) AND IsActive='Y'";
        List<MUserRoles> userRoles = new Query(ctx, MUserRoles.Table_Name, whereClause, null)
                .setParameters(userId, clientId)
                .list();

        for (MUserRoles ur : userRoles) {
            roleIds.add(ur.getAD_Role_ID());
        }

        return roleIds;
    }

    /**
     * Map OwnershipType to ChatAccess
     */
    private ChatAccess mapOwnershipTypeToAccess(String ownershipType) {
        if (X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner.equals(ownershipType)) {
            return ChatAccess.OWNER;
        } else if (X_AIG_ChatOwnership.OWNERSHIPTYPE_Write.equals(ownershipType)) {
            return ChatAccess.WRITE;
        } else if (X_AIG_ChatOwnership.OWNERSHIPTYPE_Read.equals(ownershipType)) {
            return ChatAccess.READ;
        }
        return ChatAccess.NONE;
    }

    // ========================================================================
    // Permission Checks
    // ========================================================================

    @Override
    public boolean canRead(Properties ctx, int CM_Chat_ID, String trxName) {
        ChatAccess access = getAccess(ctx, CM_Chat_ID, trxName);
        return access.ordinal() >= ChatAccess.READ.ordinal();
    }

    @Override
    public boolean canWrite(Properties ctx, int CM_Chat_ID, String trxName) {
        ChatAccess access = getAccess(ctx, CM_Chat_ID, trxName);
        return access.ordinal() >= ChatAccess.WRITE.ordinal();
    }

    @Override
    public boolean canShare(Properties ctx, int CM_Chat_ID, String trxName) {
        ChatAccess access = getAccess(ctx, CM_Chat_ID, trxName);
        return access == ChatAccess.OWNER;
    }

    @Override
    public boolean canDelete(Properties ctx, int CM_Chat_ID, String trxName) {
        ChatAccess access = getAccess(ctx, CM_Chat_ID, trxName);
        return access == ChatAccess.OWNER;
    }

    // ========================================================================
    // Sharing Management
    // ========================================================================

    @Override
    public MAIChatOwnership shareWithUser(Properties ctx, int CM_Chat_ID, int AD_User_ID,
            ChatAccess access, String trxName) {

        // Verify current user can share
        if (!canShare(ctx, CM_Chat_ID, trxName)) {
            throw new IllegalStateException("Current user does not have permission to share this chat");
        }

        // Cannot grant OWNER through sharing
        if (access == ChatAccess.OWNER) {
            throw new IllegalArgumentException("Cannot grant OWNER access through sharing. Use transferOwnership() instead.");
        }

        int currentUserId = Env.getAD_User_ID(ctx);

        // Check if grant already exists
        MAIChatOwnership existing = MAIChatOwnership.getForUser(ctx, CM_Chat_ID, AD_User_ID, trxName);
        if (existing != null) {
            // Update existing grant
            existing.setOwnershipType(access == ChatAccess.WRITE
                    ? X_AIG_ChatOwnership.OWNERSHIPTYPE_Write
                    : X_AIG_ChatOwnership.OWNERSHIPTYPE_Read);
            existing.setSharedBy_User_ID(currentUserId);
            existing.saveEx();
            return existing;
        }

        // Create new grant
        if (access == ChatAccess.WRITE) {
            return MAIChatOwnership.createWriteGrant(ctx, CM_Chat_ID, AD_User_ID, currentUserId, trxName);
        } else {
            return MAIChatOwnership.createReadGrant(ctx, CM_Chat_ID, AD_User_ID, currentUserId, trxName);
        }
    }

    @Override
    public MAIChatOwnership shareWithRole(Properties ctx, int CM_Chat_ID, int AD_Role_ID,
            ChatAccess access, String trxName) {

        // Verify current user can share
        if (!canShare(ctx, CM_Chat_ID, trxName)) {
            throw new IllegalStateException("Current user does not have permission to share this chat");
        }

        int currentUserId = Env.getAD_User_ID(ctx);

        // Check if grant already exists
        MAIChatOwnership existing = MAIChatOwnership.getForRole(ctx, CM_Chat_ID, AD_Role_ID, trxName);
        if (existing != null) {
            // Update existing grant
            String ownershipType;
            switch (access) {
                case OWNER:
                    ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner;
                    break;
                case WRITE:
                    ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Write;
                    break;
                default:
                    ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Read;
            }
            existing.setOwnershipType(ownershipType);
            existing.setSharedBy_User_ID(currentUserId);
            existing.saveEx();
            return existing;
        }

        // Create new grant
        String ownershipType;
        switch (access) {
            case OWNER:
                ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner;
                break;
            case WRITE:
                ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Write;
                break;
            default:
                ownershipType = X_AIG_ChatOwnership.OWNERSHIPTYPE_Read;
        }

        return MAIChatOwnership.createRoleGrant(ctx, CM_Chat_ID, AD_Role_ID,
                ownershipType, currentUserId, trxName);
    }

    @Override
    public void revokeAccess(Properties ctx, int AIG_ChatOwnership_ID, String trxName) {
        MAIChatOwnership ownership = new MAIChatOwnership(ctx, AIG_ChatOwnership_ID, trxName);
        if (ownership.get_ID() == 0) {
            throw new IllegalArgumentException("Ownership record not found: " + AIG_ChatOwnership_ID);
        }

        // Verify current user can revoke (must be owner of the chat)
        if (!canShare(ctx, ownership.getCM_Chat_ID(), trxName)) {
            throw new IllegalStateException("Current user does not have permission to revoke access");
        }

        // Cannot revoke creator's implicit ownership
        MChat chat = new MChat(ctx, ownership.getCM_Chat_ID(), trxName);
        if (ownership.getAD_User_ID() == chat.getCreatedBy()
                && X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner.equals(ownership.getOwnershipType())) {
            throw new IllegalStateException("Cannot revoke creator's owner access");
        }

        ownership.revoke();
    }

    @Override
    public void transferOwnership(Properties ctx, int CM_Chat_ID, int newOwner_User_ID, String trxName) {
        // Verify current user is owner
        ChatAccess currentAccess = getAccess(ctx, CM_Chat_ID, trxName);
        if (currentAccess != ChatAccess.OWNER) {
            throw new IllegalStateException("Only the owner can transfer ownership");
        }

        int currentUserId = Env.getAD_User_ID(ctx);
        MChat chat = new MChat(ctx, CM_Chat_ID, trxName);

        // Create owner grant for new owner
        MAIChatOwnership newOwnerGrant = MAIChatOwnership.getForUser(ctx, CM_Chat_ID, newOwner_User_ID, trxName);
        if (newOwnerGrant != null) {
            newOwnerGrant.setOwnershipType(X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner);
            newOwnerGrant.setSharedBy_User_ID(currentUserId);
            newOwnerGrant.saveEx();
        } else {
            MAIChatOwnership.createOwnerGrant(ctx, CM_Chat_ID, newOwner_User_ID, currentUserId, trxName);
        }

        // Downgrade current owner to WRITE (if they have an explicit grant)
        MAIChatOwnership currentOwnerGrant = MAIChatOwnership.getForUser(ctx, CM_Chat_ID, currentUserId, trxName);
        if (currentOwnerGrant != null) {
            currentOwnerGrant.setOwnershipType(X_AIG_ChatOwnership.OWNERSHIPTYPE_Write);
            currentOwnerGrant.saveEx();
        }

        // If current owner is the creator, create a WRITE grant for them
        if (chat.getCreatedBy() == currentUserId && currentOwnerGrant == null) {
            MAIChatOwnership.createWriteGrant(ctx, CM_Chat_ID, currentUserId, currentUserId, trxName);
        }

        log.info("Ownership transferred for Chat " + CM_Chat_ID
                + " from User " + currentUserId + " to User " + newOwner_User_ID);
    }

    // ========================================================================
    // Querying
    // ========================================================================

    @Override
    public List<MChat> getSharedChats(Properties ctx, String trxName) {
        int userId = Env.getAD_User_ID(ctx);
        List<MChat> result = new ArrayList<>();

        // Get all ownership records where user has access (but is not creator)
        List<MAIChatOwnership> ownerships = MAIChatOwnership.getSharedWithUser(ctx, userId, trxName);

        for (MAIChatOwnership ownership : ownerships) {
            if (ownership.isCurrentlyValid()) {
                MChat chat = new MChat(ctx, ownership.getCM_Chat_ID(), trxName);
                // Exclude chats where user is creator (those are "owned", not "shared")
                if (chat.get_ID() > 0 && chat.getCreatedBy() != userId) {
                    result.add(chat);
                }
            }
        }

        return result;
    }

    @Override
    public List<MChat> getOwnedChats(Properties ctx, String trxName) {
        int userId = Env.getAD_User_ID(ctx);

        String whereClause = "CreatedBy=? AND IsActive='Y'";
        return new Query(ctx, MChat.Table_Name, whereClause, trxName)
                .setParameters(userId)
                .setClient_ID()
                .list();
    }

    @Override
    public List<MAIChatOwnership> getSharingInfo(Properties ctx, int CM_Chat_ID, String trxName) {
        // Only owner can view sharing info
        if (!canShare(ctx, CM_Chat_ID, trxName)) {
            return new ArrayList<>();
        }

        return MAIChatOwnership.getForChat(ctx, CM_Chat_ID, trxName);
    }

    // ========================================================================
    // Owner Management
    // ========================================================================

    @Override
    public MAIChatOwnership ensureOwnerRecord(Properties ctx, int CM_Chat_ID, String trxName) {
        MChat chat = new MChat(ctx, CM_Chat_ID, trxName);
        if (chat.get_ID() == 0) {
            throw new IllegalArgumentException("Chat not found: " + CM_Chat_ID);
        }

        int creatorId = chat.getCreatedBy();

        // Check if owner record exists
        MAIChatOwnership existing = MAIChatOwnership.getForUser(ctx, CM_Chat_ID, creatorId, trxName);
        if (existing != null && X_AIG_ChatOwnership.OWNERSHIPTYPE_Owner.equals(existing.getOwnershipType())) {
            return existing;
        }

        // Create owner record
        return MAIChatOwnership.createOwnerGrant(ctx, CM_Chat_ID, creatorId, null, trxName);
    }
}
