package com.xhochy.carameldb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use the specified fixture for this unit test.
 */
// Make this annotation accessible at runtime via reflection.
@Retention(RetentionPolicy.RUNTIME)
// This annotation can only be applied to class methods.
@Target({ElementType.METHOD })
public @interface CaramelFixture {
    /**
     * Path to a YAML resource (must be a resource).
     */
    String value();
}
