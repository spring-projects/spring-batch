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

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class CompositeItemWriteListener implements ItemWriteListener {

	private OrderedComposite listeners = new OrderedComposite();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param itemWriteListeners
	 */
	public void setListeners(ItemWriteListener[] itemWriteListeners) {
		this.listeners.setItems(itemWriteListeners);
	}

	/**
	 * Register additional listener.
	 * 
	 * @param itemReaderListener
	 */
	public void register(ItemWriteListener itemReaderListener) {
		listeners.add(itemReaderListener);
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemWriteListener#afterWrite(java.lang.Object)
	 */
	public void afterWrite(Object item) {
		for (Iterator<Object> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.afterWrite(item);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemWriteListener#beforeWrite(java.lang.Object)
	 */
	public void beforeWrite(Object item) {
		for (Iterator<Object> iterator = listeners.iterator(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.beforeWrite(item);
		}
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemWriteListener#onWriteError(java.lang.Exception,
	 * java.lang.Object)
	 */
	public void onWriteError(Exception ex, Object item) {
		for (Iterator<Object> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemWriteListener listener = (ItemWriteListener) iterator.next();
			listener.onWriteError(ex, item);
		}
	}
}
