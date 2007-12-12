/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.execution.scope;

/**
 * @author Dave Syer
 * 
 */
public class StepSynchronizationManager {

	private static final ThreadLocal contextHolder = new InheritableThreadLocal();

	/**
	 * Getter for the current context..
	 * 
	 * @return the current {@link SimpleStepContext} or null if there is none
	 *         (if we are not in a step).
	 */
	public static SimpleStepContext getContext() {
		return (SimpleStepContext) contextHolder.get();
	}

	/**
	 * Method for registering a context - should only be used by
	 * {@link StepExecutor} implementations to ensure that {@link #getContext()}
	 * always returns the correct value.
	 * 
	 * @return a new context at the start of a batch.
	 */
	public static SimpleStepContext open() {
		StepContext oldSession = getContext();
		SimpleStepContext context = new SimpleStepContext(oldSession);
		StepSynchronizationManager.contextHolder.set(context);
		return context;
	}

	/**
	 * Method for de-registering the current context - should only be used by
	 * {@link StepExecutor} implementations to ensure that {@link #getContext()}
	 * always returns the correct value.
	 * 
	 * @return the old value if there was one.
	 */
	public static StepContext close() {
		SimpleStepContext oldSession = getContext();
		if (oldSession == null) {
			return null;
		}
		oldSession.close();
		StepContext context = oldSession.getParent();
		StepSynchronizationManager.contextHolder.set(context);
		return context;
	}

	/**
	 * Used internally by {@link StepExecutor} implementations to clear the
	 * current context at the end of a batch.
	 * 
	 * @return the old value if there was one.
	 */
	public static StepContext clear() {
		StepContext context = getContext();
		StepSynchronizationManager.contextHolder.set(null);
		return context;
	}

}
