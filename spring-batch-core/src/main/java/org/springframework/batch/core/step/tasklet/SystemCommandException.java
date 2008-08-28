package org.springframework.batch.core.step.tasklet;

/**
 * Exception indicating failed execution of system command.
 */
public class SystemCommandException extends RuntimeException {
	
	// generated
	private static final long serialVersionUID = 5139355923336176733L;

	public SystemCommandException(String message) {
		super(message);
	}
	
	public SystemCommandException(String message, Throwable cause) {
		super(message, cause);
	}
}
