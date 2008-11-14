package org.springframework.batch.core.scope.util;

/**
 * Interface to allow the context root for placeholder resolution to be switched
 * at runtime.  Useful for testing.
 * 
 * @author Dave Syer
 * 
 */
public interface ContextFactory {

	/**
	 * @return a root object to which placeholders resolve relatively
	 */
	Object getContext();

	/**
	 * @return a unique identifier for this context
	 */
	String getContextId();

}
