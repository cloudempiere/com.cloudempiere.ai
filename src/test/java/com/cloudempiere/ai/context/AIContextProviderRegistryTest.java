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
package com.cloudempiere.ai.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AIContextProviderRegistry.
 *
 * Tests the singleton pattern, provider registration,
 * and thread-safety of the context provider registry.
 *
 * @author CloudEmpiere
 * @version 1.0
 */
@DisplayName("AIContextProviderRegistry Tests")
class AIContextProviderRegistryTest {

    private AIContextProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = AIContextProviderRegistry.getInstance();
        // Reset to ensure clean state for each test
        registry.reset();
    }

    @AfterEach
    void tearDown() {
        // Restore default state after tests
        registry.reset();
    }

    @Nested
    @DisplayName("Singleton Pattern")
    class SingletonPattern {

        @Test
        @DisplayName("Should return same instance on multiple calls")
        void shouldReturnSameInstance() {
            AIContextProviderRegistry instance1 = AIContextProviderRegistry.getInstance();
            AIContextProviderRegistry instance2 = AIContextProviderRegistry.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }

        @Test
        @DisplayName("Instance should not be null")
        void instanceShouldNotBeNull() {
            AIContextProviderRegistry instance = AIContextProviderRegistry.getInstance();
            assertThat(instance).isNotNull();
        }
    }

    @Nested
    @DisplayName("Default Providers")
    class DefaultProviders {

        @Test
        @DisplayName("Should register default providers on initialization")
        void shouldRegisterDefaultProviders() {
            assertThat(registry.getProviderCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have CHART provider")
        void shouldHaveChartProvider() {
            assertThat(registry.hasProvider("CHART")).isTrue();
            assertThat(registry.getProvider("CHART")).isNotNull();
        }

        @Test
        @DisplayName("Should have WINDOW provider")
        void shouldHaveWindowProvider() {
            assertThat(registry.hasProvider("WINDOW")).isTrue();
            assertThat(registry.getProvider("WINDOW")).isNotNull();
        }

        @Test
        @DisplayName("Should have KNOWLEDGE_BASE provider")
        void shouldHaveKnowledgeBaseProvider() {
            assertThat(registry.hasProvider("KNOWLEDGE_BASE")).isTrue();
            assertThat(registry.getProvider("KNOWLEDGE_BASE")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Provider Registration")
    class ProviderRegistration {

        @Test
        @DisplayName("Should register new provider")
        void shouldRegisterNewProvider() {
            IAIContextProvider mockProvider = createMockProvider("TEST_PROVIDER");

            int countBefore = registry.getProviderCount();
            registry.register(mockProvider);

            assertThat(registry.getProviderCount()).isEqualTo(countBefore + 1);
            assertThat(registry.hasProvider("TEST_PROVIDER")).isTrue();
        }

        @Test
        @DisplayName("Should replace existing provider with same type")
        void shouldReplaceExistingProvider() {
            IAIContextProvider provider1 = createMockProvider("DUPLICATE");
            IAIContextProvider provider2 = createMockProvider("DUPLICATE");

            registry.register(provider1);
            int countAfterFirst = registry.getProviderCount();

            registry.register(provider2);
            int countAfterSecond = registry.getProviderCount();

            assertThat(countAfterSecond).isEqualTo(countAfterFirst);
            assertThat(registry.getProvider("DUPLICATE")).isSameAs(provider2);
        }

        @Test
        @DisplayName("Should throw exception for null provider")
        void shouldThrowForNullProvider() {
            assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Should throw exception for provider with null context type")
        void shouldThrowForNullContextType() {
            IAIContextProvider mockProvider = mock(IAIContextProvider.class);
            when(mockProvider.getContextType()).thenReturn(null);

            assertThatThrownBy(() -> registry.register(mockProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Should throw exception for provider with empty context type")
        void shouldThrowForEmptyContextType() {
            IAIContextProvider mockProvider = mock(IAIContextProvider.class);
            when(mockProvider.getContextType()).thenReturn("   ");

            assertThatThrownBy(() -> registry.register(mockProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        }
    }

    @Nested
    @DisplayName("Provider Unregistration")
    class ProviderUnregistration {

        @Test
        @DisplayName("Should unregister existing provider")
        void shouldUnregisterExistingProvider() {
            IAIContextProvider mockProvider = createMockProvider("TO_REMOVE");
            registry.register(mockProvider);

            assertThat(registry.hasProvider("TO_REMOVE")).isTrue();

            IAIContextProvider removed = registry.unregister("TO_REMOVE");

            assertThat(removed).isSameAs(mockProvider);
            assertThat(registry.hasProvider("TO_REMOVE")).isFalse();
        }

        @Test
        @DisplayName("Should return null when unregistering non-existent provider")
        void shouldReturnNullForNonExistent() {
            IAIContextProvider removed = registry.unregister("NON_EXISTENT");
            assertThat(removed).isNull();
        }

        @Test
        @DisplayName("Should return null when unregistering null type")
        void shouldReturnNullForNullType() {
            IAIContextProvider removed = registry.unregister(null);
            assertThat(removed).isNull();
        }

        @Test
        @DisplayName("Should return null when unregistering empty type")
        void shouldReturnNullForEmptyType() {
            IAIContextProvider removed = registry.unregister("   ");
            assertThat(removed).isNull();
        }
    }

    @Nested
    @DisplayName("Provider Lookup")
    class ProviderLookup {

        @Test
        @DisplayName("Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            IAIContextProvider provider1 = registry.getProvider("CHART");
            IAIContextProvider provider2 = registry.getProvider("chart");
            IAIContextProvider provider3 = registry.getProvider("Chart");

            assertThat(provider1).isNotNull();
            assertThat(provider1).isSameAs(provider2);
            assertThat(provider1).isSameAs(provider3);
        }

        @Test
        @DisplayName("Should return null for non-existent provider")
        void shouldReturnNullForNonExistent() {
            IAIContextProvider provider = registry.getProvider("NON_EXISTENT");
            assertThat(provider).isNull();
        }

        @Test
        @DisplayName("Should return null for null type")
        void shouldReturnNullForNullType() {
            IAIContextProvider provider = registry.getProvider(null);
            assertThat(provider).isNull();
        }

        @Test
        @DisplayName("Should return null for empty type")
        void shouldReturnNullForEmptyType() {
            IAIContextProvider provider = registry.getProvider("   ");
            assertThat(provider).isNull();
        }

        @Test
        @DisplayName("hasProvider should be case-insensitive")
        void hasProviderShouldBeCaseInsensitive() {
            assertThat(registry.hasProvider("CHART")).isTrue();
            assertThat(registry.hasProvider("chart")).isTrue();
            assertThat(registry.hasProvider("Chart")).isTrue();
        }
    }

    @Nested
    @DisplayName("Registry Collections")
    class RegistryCollections {

        @Test
        @DisplayName("Should return registered types as unmodifiable collection")
        void shouldReturnUnmodifiableTypes() {
            Collection<String> types = registry.getRegisteredTypes();

            assertThat(types).isNotEmpty();
            assertThatThrownBy(() -> types.add("TEST"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should return providers as unmodifiable collection")
        void shouldReturnUnmodifiableProviders() {
            Collection<IAIContextProvider> providers = registry.getProviders();

            assertThat(providers).isNotEmpty();
            assertThatThrownBy(() -> providers.add(createMockProvider("TEST")))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should contain expected default types")
        void shouldContainExpectedDefaultTypes() {
            Collection<String> types = registry.getRegisteredTypes();

            assertThat(types).contains("CHART", "WINDOW", "KNOWLEDGE_BASE");
        }
    }

    @Nested
    @DisplayName("Registry Management")
    class RegistryManagement {

        @Test
        @DisplayName("Should clear all providers")
        void shouldClearAllProviders() {
            assertThat(registry.getProviderCount()).isGreaterThan(0);

            registry.clear();

            assertThat(registry.getProviderCount()).isZero();
        }

        @Test
        @DisplayName("Should reset to default state")
        void shouldResetToDefaults() {
            // Add custom provider
            registry.register(createMockProvider("CUSTOM"));
            assertThat(registry.hasProvider("CUSTOM")).isTrue();

            // Reset
            registry.reset();

            // Custom provider should be gone
            assertThat(registry.hasProvider("CUSTOM")).isFalse();
            // Default providers should be back
            assertThat(registry.hasProvider("CHART")).isTrue();
            assertThat(registry.hasProvider("WINDOW")).isTrue();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("Should return meaningful string representation")
        void shouldReturnMeaningfulString() {
            String str = registry.toString();

            assertThat(str).contains("AIContextProviderRegistry");
            assertThat(str).contains("providers=");
            assertThat(str).contains("types=");
        }
    }

    /**
     * Helper method to create a mock provider
     */
    private IAIContextProvider createMockProvider(String contextType) {
        IAIContextProvider mockProvider = mock(IAIContextProvider.class);
        when(mockProvider.getContextType()).thenReturn(contextType);
        when(mockProvider.extractContext(
            org.mockito.ArgumentMatchers.any(Properties.class),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.any(ContextParameters.class)))
            .thenReturn(new JSONObject());
        when(mockProvider.validateContext(org.mockito.ArgumentMatchers.any(JSONObject.class)))
            .thenReturn(true);
        when(mockProvider.getSensitiveFields()).thenReturn(new String[0]);
        return mockProvider;
    }
}
