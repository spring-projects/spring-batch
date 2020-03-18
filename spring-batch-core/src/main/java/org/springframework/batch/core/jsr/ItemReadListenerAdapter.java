/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link javax.batch.api.chunk.listener.ItemReadListener} to
 * a {@link ItemReadListener}.
 * 
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 * @param <T> type to be returned via a read on the associated {@link ItemReader}
 * @since 3.0
 */
public class ItemReadListenerAdapter<T> implements ItemReadListener<T> {

	private javax.batch.api.chunk.listener.ItemReadListener delegate;

	public ItemReadListenerAdapter(javax.batch.api.chunk.listener.ItemReadListener delegate) {
		Assert.notNull(delegate, "An ItemReadListener is required");
		this.delegate = delegate;
	}

	@Override
	public void beforeRead() {
		try {
			delegate.beforeRead();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void afterRead(T item) {
		try {
			delegate.afterRead(item);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void onReadError(Exception ex) {
		try {
			delegate.onReadError(ex);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
