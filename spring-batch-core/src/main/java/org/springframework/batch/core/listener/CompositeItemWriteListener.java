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

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class CompositeItemWriteListener<S> implements ItemWriteListener<S> {

	private OrderedComposite<ItemWriteListener<? super S>> listeners = new OrderedComposite<ItemWriteListener<? super S>>();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param itemWriteListeners
	 */
	public void setListeners(List<? extends ItemWriteListener<? super S>> itemWriteListeners) {
		this.listeners.setItems(itemWriteListeners);
	}

	/**
	 * Register additional listener.
	 * 
	 * @param itemWriteListener
	 */
	public void register(ItemWriteListener<? super S> itemWriteListener) {
		listeners.add(itemWriteListener);
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see ItemWriteListener#afterWrite(java.util.List)
	 */
	public void afterWrite(List<? extends S> items) {
		for (Iterator<ItemWriteListener<? super S>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemWriteListener<? super S> listener = iterator.next();
			listener.afterWrite(items);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see ItemWriteListener#beforeWrite(List)
	 */
	public void beforeWrite(List<? extends S> items) {
		for (Iterator<ItemWriteListener<? super S>> iterator = listeners.iterator(); iterator.hasNext();) {
			ItemWriteListener<? super S> listener = iterator.next();
			listener.beforeWrite(items);
		}
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see ItemWriteListener#onWriteError(Exception, List)
	 */
	public void onWriteError(Exception ex, List<? extends S> items) {
		for (Iterator<ItemWriteListener<? super S>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemWriteListener<? super S> listener = iterator.next();
			listener.onWriteError(ex, items);
		}
	}
}
