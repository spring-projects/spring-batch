/*
 * Copyright 2006-2018 the original author or authors.
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

import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * Listener interface for the lifecycle of a chunk.  A chunk
 * can be thought of as a collection of items that will be
 * committed together.
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public interface ChunkListener extends StepListener {

	static final String ROLLBACK_EXCEPTION_KEY = "sb_rollback_exception";

	/**
	 * Callback before the chunk is executed, but inside the transaction.
	 *
	 * @param context The current {@link ChunkContext}
	 */
	void beforeChunk(ChunkContext context);

	/**
	 * Callback after the chunk is executed, outside the transaction.
	 *
	 * @param context The current {@link ChunkContext}
	 */
	void afterChunk(ChunkContext context);

	/**
	 * Callback after a chunk has been marked for rollback.  It is invoked
	 * after transaction rollback.  While the rollback will have occurred,
	 * transactional resources might still be active and accessible.  Due to
	 * this, data access code within this callback will still "participate" in
	 * the original transaction unless it declares that it runs in its own
	 * transaction.  Hence: <em> Use PROPAGATION_REQUIRES_NEW for any
	 * transactional operation that is called from here.</em>
	 *
	 * @param context the chunk context containing the exception that caused
	 * the underlying rollback.
	 */
	void afterChunkError(ChunkContext context);
}
