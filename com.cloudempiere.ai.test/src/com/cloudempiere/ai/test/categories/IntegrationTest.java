package com.cloudempiere.ai.test.categories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marker annotation for integration tests in the Testing Pyramid.
 *
 * <p>Integration tests should be:
 * <ul>
 *   <li>Moderately fast: &lt; 5s per test</li>
 *   <li>May access database with transaction rollback</li>
 *   <li>Test component boundaries and interactions</li>
 *   <li>May test external systems: APIs, databases</li>
 * </ul>
 *
 * <p>MUST extend AbstractTestCase for database access.
 *
 * <p>Target: 20% of test suite
 *
 * <p>Tags: "integration"
 *
 * @see UnitTest
 * @see E2ETest
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("integration")
public @interface IntegrationTest {
}
