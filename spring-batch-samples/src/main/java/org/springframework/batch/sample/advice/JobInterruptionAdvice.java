package org.springframework.batch.sample.advice;

import org.springframework.batch.core.domain.JobInterruptedException;

/**
 * Interface for monitors that check whether a job was interrupted by user.
 * 
 * Interruption checking is a cross-cutting concern, therefore favorably handled
 * by AOP. The implementation if expected to be used as 'before advice' or
 * 'after advice' or both.
 * 
 * @see DefaultJobInterruptionAdvice
 * 
 * @author Robert Kasanicky
 */
public interface JobInterruptionAdvice {

	/**
	 * No-op unless job was interrupted by user.
	 * 
	 * @throws JobInterruptedException if job was interrupted
	 */
	void checkInterrupt() throws JobInterruptedException;

}