package org.springframework.batch.core.job;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.UnexpectedJobExecutionException;

/**
 * Indicates the step's start limit has been exceeded.
 */
class StartLimitExceededException extends UnexpectedJobExecutionException {

	public StartLimitExceededException(Step step) {
		super("Maximum start limit exceeded for step: " + step.getName()
				+ "StartMax: " + step.getStartLimit());
	}
}
