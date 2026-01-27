package com.cloudempiere.ai.test.categories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marker annotation for unit tests in the Testing Pyramid.
 *
 * <p>Unit tests should be:
 * <ul>
 *   <li>Fast: &lt; 100ms per test</li>
 *   <li>Isolated: No external dependencies (DB, network, file system)</li>
 *   <li>No iDempiere context required</li>
 *   <li>Test individual classes/methods in isolation</li>
 * </ul>
 *
 * <p>DO NOT extend AbstractTestCase for unit tests.
 *
 * <p>Target: 70% of test suite
 *
 * <p>Tags: "unit", "fast"
 *
 * @see IntegrationTest
 * @see E2ETest
 * @see FastTest
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("unit")
@Tag("fast")
public @interface UnitTest {
}
