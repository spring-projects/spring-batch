/**
 * 
 */
package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.core.scope.StepScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Marker interface for components that wish to participate
 * in the batch lifecycle.  By default, any class annotated
 * with this annotation will also has a scope of 'Step'
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see StepScope
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Scope("step")
public @interface BatchComponent {

}
