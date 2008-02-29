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
package org.springframework.batch.execution.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.ChunkListener;

/**
 * @author Lucas Ward
 * 
 */
public class CompositeChunkListener implements ChunkListener {

	private List listeners = new ArrayList();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param listeners
	 */
	public void setListeners(ChunkListener[] listeners) {
		this.listeners = Arrays.asList(listeners);
	}

	/**
	 * Register additional listener.
	 * 
	 * @param stepListener
	 */
	public void register(ChunkListener chunkListener) {
		if (!listeners.contains(chunkListener)) {
			listeners.add(chunkListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#afterChunk()
	 */
	public void afterChunk() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ChunkListener listener = (ChunkListener) iterator.next();
			listener.afterChunk();
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#beforeChunk()
	 */
	public void beforeChunk() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ChunkListener listener = (ChunkListener) iterator.next();
			listener.beforeChunk();
		}
	}
}
