package org.springframework.batch.core.step.item;

/**
 * @author Dan Garrette
 * @since 2.0.2
 */
public class FatalException extends SkippableException {
	public FatalException(String message) {
		super(message);
	}
}
