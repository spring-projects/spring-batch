package org.springframework.batch.core.step.item;

/**
 * @author Dan Garrette
 * @since 2.0.2
 */
public class FatalRuntimeException extends SkippableRuntimeException {
	public FatalRuntimeException(String message) {
		super(message);
	}
}
