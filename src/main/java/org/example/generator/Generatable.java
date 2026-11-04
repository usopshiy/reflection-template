package org.example.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Opts a user type into generation; abstract types must list concrete implementations. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Generatable {
    Class<?>[] implementations() default {};
}
