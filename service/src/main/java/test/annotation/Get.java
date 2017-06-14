package test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as handling get requests
 * Arguments should either be {@link com.twitter.finagle.http.Request} or annotated with
 * one of {@link Body}, {@link Header}, or {@link Param}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Get {

    /**
     * The path value
     */
    String value();

}
