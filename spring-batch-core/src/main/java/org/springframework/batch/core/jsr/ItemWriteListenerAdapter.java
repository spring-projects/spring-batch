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
package org.springframework.batch.core.jsr;

import java.util.List;

import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link javax.batch.api.chunk.listener.ItemWriteListener} to
 * a {@link ItemWriteListener}.
 * 
 * @author Michael Minella
 *
 * @param <S> type to be written by the associated {@link ItemWriter}
 * @since 3.0
 */
public class ItemWriteListenerAdapter<S> implements ItemWriteListener<S> {

	private javax.batch.api.chunk.listener.ItemWriteListener delegate;

	public ItemWriteListenerAdapter(javax.batch.api.chunk.listener.ItemWriteListener delegate) {
		Assert.notNull(delegate, "An ItemWriteListener is required");
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeWrite(List<? extends S> items) {
		try {
			delegate.beforeWrite((List<Object>) items);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void afterWrite(List<? extends S> items) {
		try {
			delegate.afterWrite((List<Object>) items);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onWriteError(Exception exception, List<? extends S> items) {
		try {
			delegate.onWriteError((List<Object>) items, exception);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
