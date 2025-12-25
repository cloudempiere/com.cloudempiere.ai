package com.cloudempiere.ai.test.categories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marker annotation for end-to-end tests in the Testing Pyramid.
 *
 * <p>E2E tests should be:
 * <ul>
 *   <li>Comprehensive: &lt; 30s per test</li>
 *   <li>Test complete workflows across multiple components</li>
 *   <li>Simulate production scenarios: Full AI chat lifecycle</li>
 *   <li>Validate system behavior end-to-end</li>
 * </ul>
 *
 * <p>MUST extend AbstractTestCase for database access.
 *
 * <p>Target: 10% of test suite
 *
 * <p>Tags: "e2e", "slow"
 *
 * @see UnitTest
 * @see IntegrationTest
 * @see SlowTest
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("e2e")
@Tag("slow")
public @interface E2ETest {
}
