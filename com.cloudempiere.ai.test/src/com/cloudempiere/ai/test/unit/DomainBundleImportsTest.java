package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * A domain bundle that imports {@code dev.langchain4j.memory.chat} must also import {@code dev.langchain4j.memory}.
 * Its agent builds a {@code MessageWindowChatMemory} and hands it to LangChain4j as a {@code ChatMemory}; when the
 * parent package is not wired, loading the agent class fails with {@code NoClassDefFoundError: ...memory/ChatMemory}.
 * Felix SCR does not report that, it only shows FAILED ACTIVATION with a NullPointerException (CLD-2353).
 *
 * <p>Reads the manifests from the source tree, next to this test bundle. A missing manifest fails the test: a skipped
 * check would let the problem back in unnoticed.
 */
@UnitTest
@DisplayName("domain bundles import the LangChain4j packages their agents load")
class DomainBundleImportsTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "inventory", "kb", "purchasing", "sales", "support" })
    void importsChatMemoryParentPackage(String domain) throws IOException {
        // the working directory is the repository root (run-unit-tests.sh) or the test bundle (Tycho)
        Path rel = Paths.get("com.cloudempiere.ai." + domain, "META-INF", "MANIFEST.MF");
        Path manifest = Files.exists(rel) ? rel : Paths.get("..").resolve(rel);
        assertThat(manifest).as("manifest of the %s bundle", domain).exists();
        String imports;
        try (InputStream in = Files.newInputStream(manifest)) {
            imports = new Manifest(in).getMainAttributes().getValue("Import-Package");
        }
        assertThat(imports).contains("dev.langchain4j.memory.chat");
        assertThat(imports).containsPattern("dev\\.langchain4j\\.memory;");
    }
}
