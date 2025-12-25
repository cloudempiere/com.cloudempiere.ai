package com.cloudempiere.ai.test.categories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marker annotation for slow tests with heavy I/O.
 *
 * <p>Slow tests should be:
 * <ul>
 *   <li>Heavy I/O: &gt; 5s per test</li>
 *   <li>Complex workflows: Multi-step AI scenarios</li>
 *   <li>External dependencies: AI providers, database</li>
 *   <li>Production-like scenarios: Comprehensive validation</li>
 * </ul>
 *
 * <p>Automatically applied to: All {@link E2ETest} tests
 *
 * <p>Tags: "slow"
 *
 * @see E2ETest
 * @see FastTest
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("slow")
public @interface SlowTest {
}
