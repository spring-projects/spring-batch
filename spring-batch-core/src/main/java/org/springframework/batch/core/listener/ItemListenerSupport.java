/*
 * Copyright 2006-2013 the original author or authors.
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

import java.util.List;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;

/**
 * Basic no-op implementation of the {@link ItemReadListener},
 * {@link ItemProcessListener}, and {@link ItemWriteListener} interfaces. All
 * are implemented, since it is very common that all may need to be implemented
 * at once.
 *
 * @author Lucas Ward
 *
 */
public class ItemListenerSupport<I, O> implements ItemReadListener<I>, ItemProcessListener<I, O>, ItemWriteListener<O> {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemReadListener#afterRead(java.lang.Object)
	 */
	@Override
	public void afterRead(I item) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemReadListener#beforeRead()
	 */
	@Override
	public void beforeRead() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemReadListener#onReadError(java.lang.Exception)
	 */
	@Override
	public void onReadError(Exception ex) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.ItemProcessListener#afterProcess(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public void afterProcess(I item, O result) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.ItemProcessListener#beforeProcess(java.lang.Object)
	 */
	@Override
	public void beforeProcess(I item) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.ItemProcessListener#onProcessError(java.lang.Object,
	 *      java.lang.Exception)
	 */
	@Override
	public void onProcessError(I item, Exception e) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemWriteListener#afterWrite()
	 */
	@Override
	public void afterWrite(List<? extends O> item) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemWriteListener#beforeWrite(java.lang.Object)
	 */
	@Override
	public void beforeWrite(List<? extends O> item) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.ItemWriteListener#onWriteError(java.lang.Exception,
	 *      java.lang.Object)
	 */
	@Override
	public void onWriteError(Exception ex, List<? extends O> item) {
	}
}
