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

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * Listener interface for the lifecycle of a chunk. A chunk can be thought of as a
 * collection of items that are committed together.
 * <p>
 * {@link ChunkListener} shouldn't throw exceptions and expect continued processing, they
 * must be handled in the implementation or the step will terminate.
 * <p>
 * <strong>Note: This listener is not called in concurrent steps.</strong>
 * </p>
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @author Injae Kim
 */
public interface ChunkListener<I, O> extends StepListener {

	/**
	 * The key for retrieving the rollback exception.
	 * @deprecated since 6.0 with no replacement. Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	String ROLLBACK_EXCEPTION_KEY = "sb_rollback_exception";

	/**
	 * Callback before the chunk is executed, but inside the transaction.
	 * @param context The current {@link ChunkContext}
	 * @deprecated since 6.0, use {@link #beforeChunk(Chunk)} instead. Scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default void beforeChunk(ChunkContext context) {
	}

	/**
	 * Callback after the chunk is executed, outside the transaction.
	 * @param context The current {@link ChunkContext}
	 * @deprecated since 6.0, use {@link #afterChunk(Chunk)} instead. Scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default void afterChunk(ChunkContext context) {
	}

	/**
	 * Callback after a chunk has been marked for rollback. It is invoked after
	 * transaction rollback. While the rollback will have occurred, transactional
	 * resources might still be active and accessible. Due to this, data access code
	 * within this callback still "participates" in the original transaction unless it
	 * declares that it runs in its own transaction. <em>As a result, you should use
	 * {@code PROPAGATION_REQUIRES_NEW} for any transactional operation that is called
	 * from here.</em>
	 * @param context the chunk context containing the exception that caused the
	 * underlying rollback.
	 * @deprecated since 6.0, use {@link #onChunkError(Exception,Chunk)} instead.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default void afterChunkError(ChunkContext context) {
	}

	/**
	 * Callback after a chunk is read but before it is processed, inside the transaction.
	 * <strong>This method is not called in concurrent steps.</strong>
	 * <p>
	 * When a chunk is scanned after a write failure, each item is re-attempted in its own
	 * transaction and this method is called once per scanned item, with a chunk holding
	 * that single input item.
	 * @since 6.0
	 */
	default void beforeChunk(Chunk<I> chunk) {
	}

	/**
	 * Callback after the chunk is written, inside the transaction. <strong>This method is
	 * not called in concurrent steps.</strong>
	 * <p>
	 * When a chunk is scanned after a write failure, this method is called once per
	 * scanned item that was successfully written, and is not called for a scanned item
	 * that was skipped.
	 * @since 6.0
	 */
	default void afterChunk(Chunk<O> chunk) {
	}

	/**
	 * Callback if an exception occurs while processing or writing a chunk, inside the
	 * transaction, which is about to be rolled back. <em>As a result, you should use
	 * {@code PROPAGATION_REQUIRES_NEW} for any transactional operation that is called
	 * here</em>. <strong>This method is not called in concurrent steps.</strong>
	 * <p>
	 * When a chunk is scanned after a write failure, this method is called once for the
	 * whole chunk that failed to be written, and then once per scanned item that turns
	 * out to be faulty, with a chunk holding that single item.
	 * <p>
	 * The given chunk is never empty. A failure that occurs before any item was
	 * processed, such as a read failure, has no processed chunk to report and is
	 * signalled by {@link ItemReadListener#onReadError(Exception)} or
	 * {@link ItemProcessListener#onProcessError} instead.
	 * @param exception the exception that caused the underlying rollback.
	 * @param chunk the processed chunk, never empty
	 * @since 6.0
	 */
	default void onChunkError(Exception exception, Chunk<O> chunk) {
	}

}
