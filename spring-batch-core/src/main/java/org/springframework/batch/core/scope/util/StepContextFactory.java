package org.springframework.batch.core.scope.util;

import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

/**
 * Implementation of {@link ContextFactory} that provides the current
 * {@link StepContext} as a contxt object.
 * 
 * @author Dave Syer
 * 
 */
public class StepContextFactory implements ContextFactory {

	public Object getContext() {
		return StepSynchronizationManager.getContext();
	}

	public String getContextId() {
		return (String) StepSynchronizationManager.getContext().getId();
	}

}
