package org.springframework.batch.core.step.skip;

import org.springframework.core.NestedRuntimeException;

/**
 * Wrapper type for exceptions that cannot be skipped.
 * 
 * @author Robert Kasanicky
 *
 */
public class NonSkippableException extends NestedRuntimeException {

	public NonSkippableException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
