package org.springframework.batch.core.step.skip;

import org.springframework.core.NestedRuntimeException;

public class NonSkippableReadException extends NestedRuntimeException {

	public NonSkippableReadException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public NonSkippableReadException(String msg) {
		super(msg);
	}

}
