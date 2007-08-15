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

package org.springframework.batch.repeat;

import org.springframework.core.AttributeAccessor;

/**
 * Base interface for context which controls the state and completion /
 * termination of a batch step. A new context is created for each call to the
 * {@link RepeatOperations}. Within a batch callback code can communicate via
 * the {@link AttributeAccessor} interface.
 * 
 * @author Dave Syer
 * 
 * @see RepeatOperations#iterate(RepeatCallback)
 * 
 */
public interface RepeatContext extends AttributeAccessor {

	/**
	 * If batches are nested, then the inner batch will be created with the
	 * outer one as a parent. This is an accessor for the parent if it exists.
	 * 
	 * @return the parent context or null if there is none
	 */
	RepeatContext getParent();

	/**
	 * Public access to a counter for the number of operations attempted.
	 * 
	 * @return the number of batch operations started.
	 */
	int getStartedCount();

	/**
	 * Signal to the framework that the current batch should complete normally,
	 * independent of the current {@link CompletionPolicy}.
	 */
	void setCompleteOnly();

	/**
	 * Public accessor for the complete flag.
	 */
	boolean isCompleteOnly();

	/**
	 * Signal to the framework that the current batch should complete
	 * abnormally, independent of the current {@link CompletionPolicy}.
	 */
	void setTerminateOnly();

	/**
	 * Public accessor for the termination flag. If this flag is set then the
	 * complete flag will also be.
	 */
	boolean isTerminateOnly();

	/**
	 * Register a callback to be executed on close, associated with the
	 * attribute having the given name. The {@link Runnable} callback should not
	 * throw any exceptions.
	 * 
	 * @param name the name of the attribute to associated this callback with.
	 * If this attribute is removed the callback should never be called.
	 * @param callback a {@link Runnable} to execute when the context is closed.
	 */
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * Allow resources to be cleared, especially in destruction callbacks.
	 * Implementations should ensure that any registered destruction callbacks
	 * are executed here, as long as the corresponding attribute is still
	 * available.
	 */
	void close();

}
