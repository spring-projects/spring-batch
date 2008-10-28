/**
 * 
 */
package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Marks a method to be called before a {@link Step} is executed, which comes
 * after a {@link StepExecution} is created and persisted, but before the first
 * item is read.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see StepExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BeforeStep {

}
