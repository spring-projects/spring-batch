package org.springframework.batch.repeat.support;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Interface for result holder. Should be implemented by subclasses so that
 * the contract for
 * {@link AbstracBatchemplate#getNextResult(RepeatContext, RepeatCallback, TerminationContext, List)}
 * can be satisfied.
 * 
 * @author Dave Syer
 */
interface ResultHolder {
	/**
	 * Get the result for client from this holder. Does not block if none is
	 * available yet.
	 * 
	 * @return the result, or null if there is none.
	 * @throws IllegalStateException
	 */
	ExitStatus getResult();

	/**
	 * Get the error for client from this holder if any. Does not block if
	 * none is available yet.
	 * 
	 * @return the error, or null if there is none.
	 * @throws IllegalStateException
	 */
	Throwable getError();

	/**
	 * Get the context in which the result evaluation is executing.
	 * 
	 * @return the context of the result evaluation.
	 */
	RepeatContext getContext();
}