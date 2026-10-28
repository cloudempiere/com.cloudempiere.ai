package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.compiere.model.I_AD_User;
import org.compiere.model.I_AD_UserPreference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Global AI chats are anchored on the user's AD_UserPreference of the tenant: CM_Chat.Record_UU is unique since
 * iDempiere 11 and is filled from the referenced record, so the AD_User anchor gave every tenant's chat of a user the
 * same Record_UU and a second tenant could not create one (CLD-2429). Chats still on the AD_User anchor stay global
 * until the migration has moved them.
 *
 * <p>MAIChat's methods are not called here: initialising it loads core's MChat, which links Apache ECS, not on the
 * unit runner's classpath. The global-chat lookup is exercised on the server.
 */
@UnitTest
@DisplayName("Global AI chat anchor")
class GlobalChatAnchorTest {

    @Test
    @DisplayName("new global chats are anchored on AD_UserPreference, one record per user and tenant")
    void anchorIsUserPreference() {
        assertThat(MAIChat.AI_GLOBAL_TABLE_ID).isEqualTo(I_AD_UserPreference.Table_ID);
        assertThat(MAIChat.LEGACY_GLOBAL_TABLE_ID).isEqualTo(I_AD_User.Table_ID);
    }
}
