/*
 * Copyright 2006-present the original author or authors.
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
 * listener is not called until just before the outcome of the item, skip or step failure,
 * is decided.
 * <p>
 * {@link #onSkipInRead} and {@link #onSkipInProcess} are called inside the chunk's
 * ongoing transaction, before the rest of the chunk has been processed and written:
 * whether that transaction ends up committed or rolled back depends on the outcome of the
 * remaining items. {@link #onSkipInWrite} is called during a chunk scan, inside the
 * single-item transaction used to re-attempt the faulty item, which <strong>is always
 * rolled back</strong> right after this callback returns. <em>As a result, you should use
 * {@code PROPAGATION_REQUIRES_NEW} for any transactional operation that is called from
 * one of these methods</em>, consistent with {@link ChunkListener#onChunkError} and
 * {@link ItemWriteListener#onWriteError}.
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
	 * not buffered. Called inside the chunk's ongoing transaction, which may or may not
	 * be rolled back depending on the outcome of the rest of the chunk.
	 * @param t cause of the failure
	 */
	default void onSkipInRead(Throwable t) {
	}

	/**
	 * This item failed on write with the given exception, and a skip was called for.
	 * Called during a chunk scan, inside the single-item transaction used to re-attempt
	 * this item, which is rolled back right after this callback returns.
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	default void onSkipInWrite(S item, Throwable t) {
	}

	/**
	 * This item failed on processing with the given exception, and a skip was called for.
	 * Called inside the chunk's ongoing transaction, which may or may not be rolled back
	 * depending on the outcome of the rest of the chunk.
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	default void onSkipInProcess(T item, Throwable t) {
	}

}
