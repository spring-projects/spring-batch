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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeChunkListener<I, O> implements ChunkListener<I, O> {

	private final OrderedComposite<ChunkListener> listeners = new OrderedComposite<>();

	/**
	 * Default constructor
	 */
	public CompositeChunkListener() {

	}

	/**
	 * Convenience constructor for setting the {@link ChunkListener}s.
	 * @param listeners list of {@link ChunkListener}.
	 */
	public CompositeChunkListener(List<? extends ChunkListener> listeners) {
		setListeners(listeners);
	}

	/**
	 * Convenience constructor for setting the {@link ChunkListener}s.
	 * @param listeners array of {@link ChunkListener}.
	 */
	public CompositeChunkListener(ChunkListener... listeners) {
		this(Arrays.asList(listeners));
	}

	/**
	 * Public setter for the listeners.
	 * @param listeners list of {@link ChunkListener}.
	 */
	public void setListeners(List<? extends ChunkListener> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * Register additional listener.
	 * @param chunkListener instance of {@link ChunkListener}.
	 */
	public void register(ChunkListener chunkListener) {
		listeners.add(chunkListener);
	}

	/**
	 * Call the registered listeners in reverse order.
	 *
	 * @see ChunkListener#afterChunk(ChunkContext context)
	 * @deprecated since 6.0, use {@link #afterChunk(Chunk)} instead. Scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	@Override
	public void afterChunk(ChunkContext context) {
		for (Iterator<ChunkListener> iterator = listeners.reverse(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.afterChunk(context);
		}
	}

	/**
	 * Call the registered listeners in reverse order.
	 *
	 * @see ChunkListener#afterChunk(Chunk)
	 */
	@Override
	public void afterChunk(Chunk chunk) {
		for (Iterator<ChunkListener> iterator = listeners.reverse(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.afterChunk(chunk);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritizing those that
	 * implement {@link Ordered}.
	 *
	 * @see ChunkListener#beforeChunk(ChunkContext context)
	 * @deprecated since 6.0, use {@link #beforeChunk(Chunk)} instead. Scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	@Override
	public void beforeChunk(ChunkContext context) {
		for (Iterator<ChunkListener> iterator = listeners.iterator(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.beforeChunk(context);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritizing those that
	 * implement {@link Ordered}.
	 *
	 * @see ChunkListener#beforeChunk(Chunk chunk)
	 */
	@Override
	public void beforeChunk(Chunk chunk) {
		for (Iterator<ChunkListener> iterator = listeners.iterator(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.beforeChunk(chunk);
		}
	}

	/**
	 * Call the registered listeners in reverse order.
	 *
	 * @see ChunkListener#afterChunkError(ChunkContext context)
	 * @deprecated since 6.0, use {@link #onChunkError(Exception,Chunk)} instead.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	@Override
	public void afterChunkError(ChunkContext context) {
		for (Iterator<ChunkListener> iterator = listeners.reverse(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.afterChunkError(context);
		}
	}

	/**
	 * Call the registered listeners in reverse order.
	 *
	 * @see ChunkListener#onChunkError(Exception, Chunk)
	 */
	@Override
	public void onChunkError(Exception exception, Chunk chunk) {
		for (Iterator<ChunkListener> iterator = listeners.reverse(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.onChunkError(exception, chunk);
		}
	}

}
