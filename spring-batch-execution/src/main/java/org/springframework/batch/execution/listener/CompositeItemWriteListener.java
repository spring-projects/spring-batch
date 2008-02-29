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
import org.springframework.batch.core.domain.ItemReadListener;
import org.springframework.batch.core.domain.ItemWriteListener;

/**
 * @author Lucas Ward
 * 
 */
public class CompositeItemWriteListener implements ItemWriteListener {

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
	 * @param itemReaderListener
	 */
	public void register(ItemWriteListener itemReaderListener) {
		if (!listeners.contains(itemReaderListener)) {
			listeners.add(itemReaderListener);
		}
	}

	public void afterWrite() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.afterWrite();
		}
	}

	public void beforeWrite(Object item) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.beforeWrite(item);
		}
	}

	public void onWriteError(Exception ex, Object item) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.onWriteError(ex, item);
		}
	}
}
