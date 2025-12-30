package com.cloudempiere.ai.test.categories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marker annotation for fast tests suitable for watch mode.
 *
 * <p>Fast tests should be:
 * <ul>
 *   <li>Very fast: &lt; 1s per test</li>
 *   <li>Suitable for watch mode: Can run on every file save</li>
 *   <li>Provide rapid feedback: Immediate failure signals</li>
 * </ul>
 *
 * <p>Automatically applied to: All {@link UnitTest} tests
 *
 * <p>Tags: "fast"
 *
 * @see UnitTest
 * @see SlowTest
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("fast")
public @interface FastTest {
}
