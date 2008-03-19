package org.springframework.batch.core;

/**
 * Indicates the step's start limit has been exceeded.
 */
public class StartLimitExceededException extends RuntimeException {

	public StartLimitExceededException(String message) {
		super(message);
	}
}
