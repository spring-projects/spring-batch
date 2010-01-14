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

package org.springframework.batch.repeat.support;

import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * Global variable support for repeat clients. Normally it is not necessary for
 * clients to be aware of the surrounding environment because a
 * {@link RepeatCallback} can always use the context it is passed by the
 * enclosing {@link RepeatOperations}. But occasionally it might be helpful to
 * have lower level access to the ongoing {@link RepeatContext} so we provide a
 * global accessor here. The mutator methods ({@link #clear()} and
 * {@link #register(RepeatContext)} should not be used except internally by
 * {@link RepeatOperations} implementations.
 * 
 * @author Dave Syer
 * 
 */
public final class RepeatSynchronizationManager {

	private static final ThreadLocal<RepeatContext> contextHolder = new ThreadLocal<RepeatContext>();

	private RepeatSynchronizationManager() {
	}

	/**
	 * Getter for the current context. A context is shared by all items in the
	 * batch, so this method is intended to return the same context object
	 * independent of whether the callback is running synchronously or
	 * asynchronously with the surrounding {@link RepeatOperations}.
	 * 
	 * @return the current {@link RepeatContext} or null if there is none (if we
	 * are not in a batch).
	 */
	public static RepeatContext getContext() {
		return contextHolder.get();
	}

	/**
	 * Convenience method to set the current repeat operation to complete if it
	 * exists.
	 */
	public static void setCompleteOnly() {
		RepeatContext context = getContext();
		if (context != null) {
			context.setCompleteOnly();
		}
	}

	/**
	 * Method for registering a context - should only be used by
	 * {@link RepeatOperations} implementations to ensure that
	 * {@link #getContext()} always returns the correct value.
	 * 
	 * @param context a new context at the start of a batch.
	 * @return the old value if there was one.
	 */
	public static RepeatContext register(RepeatContext context) {
		RepeatContext oldSession = getContext();
		RepeatSynchronizationManager.contextHolder.set(context);
		return oldSession;
	}

	/**
	 * Clear the current context at the end of a batch - should only be used by
	 * {@link RepeatOperations} implementations.
	 * 
	 * @return the old value if there was one.
	 */
	public static RepeatContext clear() {
		RepeatContext context = getContext();
		RepeatSynchronizationManager.contextHolder.set(null);
		return context;
	}

	/**
	 * Set current session and all ancestors (via parent) to complete.,
	 */
	public static void setAncestorsCompleteOnly() {
		RepeatContext context = getContext();
		while (context != null) {
			context.setCompleteOnly();
			context = context.getParent();
		}
	}

}
