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
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Marks a method to be called before a {@link Job} is executed, which comes
 * after a {@link JobExecution} is created and persisted, but before the first
 * {@link Step} is executed.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see JobExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Qualifier("JobExecutionListener")
public @interface BeforeJob {

}
