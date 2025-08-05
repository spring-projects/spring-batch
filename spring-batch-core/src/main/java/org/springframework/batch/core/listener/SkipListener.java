/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.listener;

import org.springframework.batch.core.step.Step;

/**
 * Interface for listener to skipped items. Callbacks are called by {@link Step}
 * implementations at the appropriate time in the step lifecycle. Implementers of this
 * interface should not assume that any method is called immediately after an error has
 * been encountered. Because there may be errors later on in processing the chunk, this
 * listener is not called until just before committing.
 *
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 *
 */
public interface SkipListener<T, S> extends StepListener {

	/**
	 * Callback for a failure on read that is legal and, consequently, is not going to be
	 * re-thrown. In case a transaction is rolled back and items are re-read, this
	 * callback occurs repeatedly for the same cause. This happens only if read items are
	 * not buffered.
	 * @param t cause of the failure
	 */
	default void onSkipInRead(Throwable t) {
	}

	/**
	 * This item failed on write with the given exception, and a skip was called for.
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	default void onSkipInWrite(S item, Throwable t) {
	}

	/**
	 * This item failed on processing with the given exception, and a skip was called for.
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	default void onSkipInProcess(T item, Throwable t) {
	}

}
