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

package org.springframework.batch.retry.support;

import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryOperations;

/**
 * Global variable support for retry clients. Normally it is not necessary for
 * clients to be aware of the surrounding environment because a
 * {@link RetryCallback} can always use the context it is passed by the
 * enclosing {@link RetryOperations}. But occasionally it might be helpful to
 * have lower level access to the ongoing {@link RetryContext} so we provide a
 * global accessor here. The mutator methods ({@link #clear()} and
 * {@link #register(RetryContext)} should not be used except internally by
 * {@link RetryOperations} implementations.
 * 
 * @author Dave Syer
 * 
 */
public final class RetrySynchronizationManager {

	private RetrySynchronizationManager() {}

	private static final ThreadLocal<RetryContext> context = new ThreadLocal<RetryContext>();

	/**
	 * Public accessor for the locally enclosing {@link RetryContext}.
	 * 
	 * @return the current retry context, or null if there isn't one
	 */
	public static RetryContext getContext() {
		RetryContext result = (RetryContext) context.get();
		return result;
	}

	/**
	 * Method for registering a context - should only be used by
	 * {@link RetryOperations} implementations to ensure that
	 * {@link #getContext()} always returns the correct value.
	 * 
	 * @param context the new context to register
	 * @return the old context if there was one
	 */
	public static RetryContext register(RetryContext context) {
		RetryContext oldContext = getContext();
		RetrySynchronizationManager.context.set(context);
		return oldContext;
	}

	/**
	 * Clear the current context at the end of a batch - should only be used by
	 * {@link RepeatOperations} implementations.
	 * 
	 * @return the old value if there was one.
	 */
	public static RetryContext clear() {
		RetryContext value = getContext();
		RetryContext parent = value == null ? null : value.getParent();
		RetrySynchronizationManager.context.set(parent);
		return value;
	}

}
