package org.springframework.batch.core.step;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.UnexpectedJobExecutionException;

/**
 * Signals a fatal exception in step - e.g. unable to persist batch metadata or
 * rollback transaction. Throwing this exception in a step implementation will
 * result in the step having a status of {@link BatchStatus#UNKNOWN}.
 */
public class FatalException extends UnexpectedJobExecutionException {
	public FatalException(String string, Throwable e) {
		super(string, e);
	}
}