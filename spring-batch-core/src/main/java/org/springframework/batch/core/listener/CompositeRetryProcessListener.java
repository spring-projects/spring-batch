/*
 * Copyright 2013 the original author or authors.
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
import javax.batch.api.chunk.listener.RetryProcessListener;

/**
 * <p>
 * Composite class holding {@link RetryProcessListener}'s.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class CompositeRetryProcessListener implements RetryProcessListener {
	private OrderedComposite<RetryProcessListener> listeners = new OrderedComposite<>();

	/**
	 * <p>
	 * Public setter for the {@link RetryProcessListener}'s.
	 * </p>
	 *
	 * @param listeners the {@link RetryProcessListener}'s to set
	 */
	public void setListeners(List<? extends RetryProcessListener> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * <p>
	 * Register an additional {@link RetryProcessListener}.
	 * </p>
	 *
	 * @param listener the {@link RetryProcessListener} to register
	 */
	public void register(RetryProcessListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onRetryProcessException(Object item, Exception ex) throws Exception {
		for (Iterator<RetryProcessListener> iterator = listeners.reverse(); iterator.hasNext();) {
			RetryProcessListener listener = iterator.next();
			listener.onRetryProcessException(item, ex);
		}
	}
}
