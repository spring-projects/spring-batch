/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

/**
 * Listener interface for the processing of an item. Implementations of this interface are
 * notified before and after an item is passed to the {@link ItemProcessor} and in the
 * event of any exceptions thrown by the processor.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public interface ItemProcessListener<T, S> extends StepListener {

	/**
	 * Called before {@link ItemProcessor#process(Object)}.
	 * @param item to be processed.
	 */
	default void beforeProcess(T item) {
	}

	/**
	 * Called after {@link ItemProcessor#process(Object)} returns. If the processor
	 * returns {@code null}, this method is still called, with a {@code null} result,
	 * allowing for notification of "filtered" items.
	 * @param item to be processed
	 * @param result of processing
	 */
	default void afterProcess(T item, @Nullable S result) {
	}

	/**
	 * Called if an exception was thrown from {@link ItemProcessor#process(Object)}.
	 * @param item attempted to be processed
	 * @param e - exception thrown during processing.
	 */
	default void onProcessError(T item, Exception e) {
	}

}
