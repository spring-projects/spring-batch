package org.springframework.batch.repeat.support;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Interface for result holder. 
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
	RepeatStatus getResult();

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