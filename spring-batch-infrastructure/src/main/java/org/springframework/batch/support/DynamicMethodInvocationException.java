package org.springframework.batch.support;

import org.springframework.util.MethodInvoker;

/**
 * Indicates an error has been encountered
 * while trying to dynamically call a method e.g. using {@link MethodInvoker}.
 * 
 * @author Robert Kasanicky
 */
public class DynamicMethodInvocationException extends RuntimeException {
	
	//generated value
	private static final long serialVersionUID = -6056786139731564040L;

	public DynamicMethodInvocationException(Throwable cause){
		super(cause);
	}
	
	public DynamicMethodInvocationException(String message, Throwable cause) {
		super(message, cause);
	}
}
