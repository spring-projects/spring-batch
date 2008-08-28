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

import org.springframework.batch.core.SkipListener;
import org.springframework.core.Ordered;

/**
 * @author Dave Syer
 * 
 */
public class CompositeSkipListener<T,S> implements SkipListener<T,S> {

	private OrderedComposite<SkipListener<? super T,? super S>> listeners = new OrderedComposite<SkipListener<? super T,? super S>>();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param listeners
	 */
	public void setListeners(List<? extends SkipListener<? super T,? super S>> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * Register additional listener.
	 * 
	 * @param listener
	 */
	public void register(SkipListener<? super T,? super S> listener) {
		listeners.add(listener);
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.SkipListener#onSkipInRead(java.lang.Throwable)
	 */
	public void onSkipInRead(Throwable t) {
		for (Iterator<SkipListener<? super T,? super S>> iterator = listeners.iterator(); iterator.hasNext();) {
			SkipListener<? super T,? super S> listener = iterator.next();
			listener.onSkipInRead(t);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.SkipListener#onSkipInWrite(java.lang.Object,
	 * java.lang.Throwable)
	 */
	public void onSkipInWrite(S item, Throwable t) {
		for (Iterator<SkipListener<? super T,? super S>> iterator = listeners.iterator(); iterator.hasNext();) {
			SkipListener<? super T,? super S> listener = iterator.next();
			listener.onSkipInWrite(item, t);
		}
	}

	/**
	 * Call the registered listeners in order, respecting and prioritising those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.SkipListener#onSkipInWrite(java.lang.Object,
	 * java.lang.Throwable)
	 */
	public void onSkipInProcess(T item, Throwable t) {
		for (Iterator<SkipListener<? super T,? super S>> iterator = listeners.iterator(); iterator.hasNext();) {
			SkipListener<? super T,? super S> listener = iterator.next();
			listener.onSkipInProcess(item, t);
		}
	}

}
