/**
 * 
 */
package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Marks a method to be called after a {@link Job} has completed.  Annotated methods
 * will be called regardless of the status of the {@link JobExecution}.  
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see JobExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AfterJob {

}
