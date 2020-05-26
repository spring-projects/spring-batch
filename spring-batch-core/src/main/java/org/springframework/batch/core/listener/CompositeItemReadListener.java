/*
 * Copyright 2006-2013 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.ItemReadListener;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
public class CompositeItemReadListener<T> implements ItemReadListener<T> {

	private OrderedComposite<ItemReadListener<? super T>> listeners = new OrderedComposite<>();

	/**
	 * Public setter for the listeners.
	 *
	 * @param itemReadListeners list of {@link ItemReadListener}s to be called when read events occur.
	 */
	public void setListeners(List<? extends ItemReadListener<? super T>> itemReadListeners) {
		this.listeners.setItems(itemReadListeners);
	}

	/**
	 * Register additional listener.
	 *
	 * @param itemReaderListener instance of {@link ItemReadListener} to be registered.
	 */
	public void register(ItemReadListener<? super T> itemReaderListener) {
		listeners.add(itemReaderListener);
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemReadListener#afterRead(java.lang.Object)
	 */
	@Override
	public void afterRead(T item) {
		for (Iterator<ItemReadListener<? super T>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemReadListener<? super T> listener = iterator.next();
			listener.afterRead(item);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemReadListener#beforeRead()
	 */
	@Override
	public void beforeRead() {
		for (Iterator<ItemReadListener<? super T>> iterator = listeners.iterator(); iterator.hasNext();) {
			ItemReadListener<? super T> listener = iterator.next();
			listener.beforeRead();
		}
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemReadListener#onReadError(java.lang.Exception)
	 */
	@Override
	public void onReadError(Exception ex) {
		for (Iterator<ItemReadListener<? super T>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemReadListener<? super T> listener = iterator.next();
			listener.onReadError(ex);
		}
	}
}
