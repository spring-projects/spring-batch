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
package org.springframework.batch.core.listener;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeItemProcessListener<T, S> implements ItemProcessListener<T, S> {

	private OrderedComposite<ItemProcessListener<? super T, ? super S>> listeners = new OrderedComposite<>();

	/**
	 * Public setter for the listeners.
	 *
	 * @param itemProcessorListeners list of {@link ItemProcessListener}s to be called when process events occur.
	 */
	public void setListeners(List<? extends ItemProcessListener<? super T, ? super S>> itemProcessorListeners) {
		this.listeners.setItems(itemProcessorListeners);
	}

	/**
	 * Register additional listener.
	 *
	 * @param itemProcessorListener instance  of {@link ItemProcessListener} to be registered.
	 */
	public void register(ItemProcessListener<? super T, ? super S> itemProcessorListener) {
		listeners.add(itemProcessorListener);
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemProcessListener#afterProcess(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void afterProcess(T item, @Nullable S result) {
		for (Iterator<ItemProcessListener<? super T, ? super S>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemProcessListener<? super T, ? super S> listener = iterator.next();
			listener.afterProcess(item, result);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemProcessListener#beforeProcess(java.lang.Object)
	 */
	@Override
	public void beforeProcess(T item) {
		for (Iterator<ItemProcessListener<? super T, ? super S>> iterator = listeners.iterator(); iterator.hasNext();) {
			ItemProcessListener<? super T, ? super S> listener = iterator.next();
			listener.beforeProcess(item);
		}
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritising those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.ItemProcessListener#onProcessError(java.lang.Object,
	 * java.lang.Exception)
	 */
	@Override
	public void onProcessError(T item, Exception e) {
		for (Iterator<ItemProcessListener<? super T, ? super S>> iterator = listeners.reverse(); iterator.hasNext();) {
			ItemProcessListener<? super T, ? super S> listener = iterator.next();
			listener.onProcessError(item, e);
		}
	}

}
