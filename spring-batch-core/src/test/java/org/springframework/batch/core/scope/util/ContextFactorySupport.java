package org.springframework.batch.core.scope.util;

public class ContextFactorySupport implements ContextFactory {

	private int count = 0;

	/**
	 * Returns this.  Override for more sensible behaviour.
	 * 
	 * @see org.springframework.batch.core.scope.util.ContextFactory#getContext()
	 */
	public Object getContext() {
		return this;
	}

	/**
	 * Returns the context plus a counter, so each call is unique.
	 * 
	 * @see org.springframework.batch.core.scope.util.ContextFactory#getContextId()
	 */
	public String getContextId() {
		return getContext()+"#"+(count ++);
	}

}
