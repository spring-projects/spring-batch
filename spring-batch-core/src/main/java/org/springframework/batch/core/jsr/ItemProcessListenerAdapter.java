/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.util.Assert;

/**
 * Wrapper class for {@link javax.batch.api.chunk.listener.ItemProcessListener}
 *
 * @author Michael Minella
 *
 * @param <T> input type
 * @param <S> output type
 * @since 3.0
 */
public class ItemProcessListenerAdapter<T,S> implements ItemProcessListener<T, S> {

	private javax.batch.api.chunk.listener.ItemProcessListener delegate;

	/**
	 * @param delegate to be called within the batch lifecycle
	 */
	public ItemProcessListenerAdapter(javax.batch.api.chunk.listener.ItemProcessListener delegate) {
		Assert.notNull(delegate, "An ItemProcessListener is requred");
		this.delegate = delegate;
	}

	@Override
	public void beforeProcess(T item) {
		try {
			delegate.beforeProcess(item);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void afterProcess(T item, S result) {
		try {
			delegate.afterProcess(item, result);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void onProcessError(T item, Exception e) {
		try {
			delegate.onProcessError(item, e);
		} catch (Exception e1) {
			throw new BatchRuntimeException(e1);
		}
	}
}
