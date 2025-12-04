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

import java.util.List;
import java.util.Properties;

import org.compiere.model.MChat;

import com.cloudempiere.ai.model.MAIChatOwnership;

/**
 * Interface for Chat Access Service
 * Implements ADR-036: Chat Ownership and Sharing Model
 *
 * Provides methods for:
 * - Resolving user access levels to chats
 * - Checking specific access permissions
 * - Managing sharing between users/roles
 * - Querying shared chats
 *
 * @author Cloudempiere
 */
public interface IChatAccessService {

    /**
     * Chat access levels (ADR-036)
     */
    public enum ChatAccess {
        /** No access to the chat */
        NONE,
        /** View only - can read messages but not add new ones */
        READ,
        /** View and write - can read and add new messages */
        WRITE,
        /** Full control - view, edit, delete, share, transfer ownership */
        OWNER
    }

    // ========================================================================
    // Access Resolution
    // ========================================================================

    /**
     * Resolve user's access level to a chat using the hybrid algorithm:
     * 1. Creator is always owner
     * 2. Check explicit ownership grants (AIG_ChatOwnership)
     * 3. Check role-based grants
     * 4. Apply ConfidentialType baseline
     * 5. For context chats, check record access
     *
     * @param ctx context (contains current user, role, client, org)
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return access level (NONE, READ, WRITE, or OWNER)
     */
    ChatAccess getAccess(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Resolve user's access level to a chat (using MChat object)
     *
     * @param ctx context
     * @param chat the chat to check
     * @return access level
     */
    ChatAccess getAccess(Properties ctx, MChat chat);

    // ========================================================================
    // Permission Checks
    // ========================================================================

    /**
     * Check if current user can read the chat
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return true if user has READ, WRITE, or OWNER access
     */
    boolean canRead(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Check if current user can write to the chat (add messages)
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return true if user has WRITE or OWNER access
     */
    boolean canWrite(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Check if current user can share the chat with others
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return true if user has OWNER access
     */
    boolean canShare(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Check if current user can delete the chat
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return true if user has OWNER access
     */
    boolean canDelete(Properties ctx, int CM_Chat_ID, String trxName);

    // ========================================================================
    // Sharing Management
    // ========================================================================

    /**
     * Share chat with a specific user
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_User_ID user to share with
     * @param access access level to grant (READ or WRITE, not OWNER)
     * @param trxName transaction
     * @return created ownership record
     * @throws IllegalStateException if current user cannot share
     * @throws IllegalArgumentException if trying to grant OWNER access
     */
    MAIChatOwnership shareWithUser(Properties ctx, int CM_Chat_ID, int AD_User_ID,
            ChatAccess access, String trxName);

    /**
     * Share chat with a role (all users with that role get access)
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param AD_Role_ID role to share with
     * @param access access level to grant
     * @param trxName transaction
     * @return created ownership record
     * @throws IllegalStateException if current user cannot share
     */
    MAIChatOwnership shareWithRole(Properties ctx, int CM_Chat_ID, int AD_Role_ID,
            ChatAccess access, String trxName);

    /**
     * Revoke access for a specific ownership grant
     * @param ctx context
     * @param AIG_ChatOwnership_ID ownership record to revoke
     * @param trxName transaction
     * @throws IllegalStateException if current user cannot revoke
     */
    void revokeAccess(Properties ctx, int AIG_ChatOwnership_ID, String trxName);

    /**
     * Transfer ownership of a chat to another user
     * The new owner becomes the sole owner; current owner is downgraded to WRITE.
     *
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param newOwner_User_ID new owner user ID
     * @param trxName transaction
     * @throws IllegalStateException if current user is not owner
     */
    void transferOwnership(Properties ctx, int CM_Chat_ID, int newOwner_User_ID, String trxName);

    // ========================================================================
    // Querying
    // ========================================================================

    /**
     * Get all chats shared with the current user (not created by them)
     * @param ctx context
     * @param trxName transaction
     * @return list of chats shared with user
     */
    List<MChat> getSharedChats(Properties ctx, String trxName);

    /**
     * Get all chats the current user owns (created by them)
     * @param ctx context
     * @param trxName transaction
     * @return list of owned chats
     */
    List<MChat> getOwnedChats(Properties ctx, String trxName);

    /**
     * Get sharing information for a chat (who has access)
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return list of ownership records (empty if user cannot view sharing info)
     */
    List<MAIChatOwnership> getSharingInfo(Properties ctx, int CM_Chat_ID, String trxName);

    // ========================================================================
    // Owner Management
    // ========================================================================

    /**
     * Ensure creator has an owner record in AIG_ChatOwnership
     * Creates one if it doesn't exist (for migration of existing chats)
     *
     * @param ctx context
     * @param CM_Chat_ID chat ID
     * @param trxName transaction
     * @return the owner record (existing or newly created)
     */
    MAIChatOwnership ensureOwnerRecord(Properties ctx, int CM_Chat_ID, String trxName);
}
