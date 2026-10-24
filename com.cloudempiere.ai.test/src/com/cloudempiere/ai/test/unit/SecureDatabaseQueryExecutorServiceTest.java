package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * The domain {@code *Tools} components take a mandatory {@code @Reference SecureDatabaseQueryExecutor}. If it is not an
 * OSGi service they never activate, and neither do the agents that need them (CLD-2353). OSGi's {@code @Component} has
 * class retention and cannot be read by reflection, so this pins what the framework actually reads: the descriptor in
 * {@code OSGI-INF/}, which the bundle manifest lists under {@code Service-Component}.
 */
@UnitTest
@DisplayName("SecureDatabaseQueryExecutor is an OSGi service")
class SecureDatabaseQueryExecutorServiceTest {

    private static final String DESCRIPTOR = "/OSGI-INF/com.cloudempiere.ai.database.SecureDatabaseQueryExecutor.xml";
    private static final String CLASS_NAME = SecureDatabaseQueryExecutor.class.getName();

    private Element component() throws Exception {
        try (InputStream in = SecureDatabaseQueryExecutor.class.getResourceAsStream(DESCRIPTOR)) {
            assertThat(in).as("descriptor %s must be packaged in the bundle", DESCRIPTOR).isNotNull();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(in);
            return doc.getDocumentElement();
        }
    }

    @Test
    @DisplayName("a descriptor for the component is packaged")
    void descriptorIsPackaged() throws Exception {
        assertThat(component().getLocalName()).isEqualTo("component");
    }

    @Test
    @DisplayName("the component provides the class as its service interface")
    void providesTheService() throws Exception {
        Element provide = (Element) component().getElementsByTagName("provide").item(0);
        assertThat(provide).as("<provide> element").isNotNull();
        assertThat(provide.getAttribute("interface")).isEqualTo(CLASS_NAME);
    }

    @Test
    @DisplayName("the implementation is the class itself")
    void implementationIsTheClass() throws Exception {
        Element impl = (Element) component().getElementsByTagName("implementation").item(0);
        assertThat(impl.getAttribute("class")).isEqualTo(CLASS_NAME);
    }

    @Test
    @DisplayName("it needs no references of its own (nothing can leave it unsatisfied)")
    void hasNoReferences() throws Exception {
        assertThat(component().getElementsByTagName("reference").getLength()).isZero();
    }

    @Test
    @DisplayName("it can be created by the framework: public class with a public no-argument constructor")
    void hasPublicNoArgConstructor() throws Exception {
        assertThat(java.lang.reflect.Modifier.isPublic(SecureDatabaseQueryExecutor.class.getModifiers())).isTrue();
        assertThat(SecureDatabaseQueryExecutor.class.getConstructor()).isNotNull();
    }
}
