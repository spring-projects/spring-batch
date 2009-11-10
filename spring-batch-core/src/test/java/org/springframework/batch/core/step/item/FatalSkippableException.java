package org.springframework.batch.core.step.item;

/**
 * @author Dan Garrette
 * @since 2.0.2
 */
public class FatalSkippableException extends SkippableException {
	public FatalSkippableException(String message) {
		super(message);
	}
}
