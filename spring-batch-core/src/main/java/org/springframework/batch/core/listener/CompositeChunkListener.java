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
package org.springframework.batch.core.listener;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.ChunkListener;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * 
 */
public class CompositeChunkListener implements ChunkListener {

	private OrderedComposite<ChunkListener> listeners = new OrderedComposite<ChunkListener>();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param listeners
	 */
	public void setListeners(List<? extends ChunkListener> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * Register additional listener.
	 * 
	 * @param chunkListener
	 */
	public void register(ChunkListener chunkListener) {
		listeners.add(chunkListener);
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * 
	 * @see org.springframework.batch.core.ChunkListener#afterChunk()
	 */
	public void afterChunk() {
		for (Iterator<ChunkListener> iterator = listeners.iterator(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.afterChunk();
		}
	}

	/**
	 * Call the registered listeners in reverse order.
	 * 
	 * @see org.springframework.batch.core.ChunkListener#beforeChunk()
	 */
	public void beforeChunk() {
		for (Iterator<ChunkListener> iterator = listeners.reverse(); iterator.hasNext();) {
			ChunkListener listener = iterator.next();
			listener.beforeChunk();
		}
	}
}
