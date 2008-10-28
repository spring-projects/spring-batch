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
 * Marks a method to be called after a {@link Step} has completed.  Annotated methods
 * will be called regardless of the status of the {@link StepExecution}.  
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see StepExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AfterStep {

}
