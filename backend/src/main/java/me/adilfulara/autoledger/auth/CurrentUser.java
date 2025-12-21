package me.adilfulara.autoledger.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the currently authenticated user into controller methods.
 *
 * <p>Example usage:
 * <pre>
 * &#64;GetMapping("/api/cars")
 * public ResponseEntity&lt;List&lt;CarResponse&gt;&gt; listCars(&#64;CurrentUser AuthenticatedUser user) {
 *     // user is automatically injected from request attribute
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
