/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.UncheckedTransactionException;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link javax.batch.api.chunk.listener.ChunkListener} to
 * a {@link ChunkListener}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class ChunkListenerAdapter implements ChunkListener {

	private final javax.batch.api.chunk.listener.ChunkListener delegate;

	/**
	 * @param delegate to be called within the step chunk lifecycle
	 */
	public ChunkListenerAdapter(javax.batch.api.chunk.listener.ChunkListener delegate) {
		Assert.notNull(delegate, "A ChunkListener is required");
		this.delegate = delegate;
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		try {
			delegate.beforeChunk();
		} catch (Exception e) {
			throw new UncheckedTransactionException(e);
		}
	}

	@Override
	public void afterChunk(ChunkContext context) {
		try {
			delegate.afterChunk();
		} catch (Exception e) {
			throw new UncheckedTransactionException(e);
		}
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		if(context != null) {
			try {
				delegate.onError((Exception) context.getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY));
			} catch (Exception e) {
				throw new UncheckedTransactionException(e);
			}
		} else {
			throw new BatchRuntimeException("Unable to retrieve causing exception due to null ChunkContext");
		}
	}
}
