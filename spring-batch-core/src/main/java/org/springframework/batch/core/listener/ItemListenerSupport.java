/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;

/**
 * Basic no-op implementation of both the {@link ItemWriteListener} and
 * {@link ItemReadListener} interfaces.  Both are implemented, since it is 
 * very common that both may need to be implemented at once.
 * 
 * @author Lucas Ward
 *
 */
public class ItemListenerSupport<S> implements ItemWriteListener<S>, ItemReadListener {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#afterWrite()
	 */
	public void afterWrite(List<? extends S> item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#beforeWrite(java.lang.Object)
	 */
	public void beforeWrite(List<? extends S> item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#onWriteError(java.lang.Exception, java.lang.Object)
	 */
	public void onWriteError(Exception ex, List<? extends S> item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#afterRead(java.lang.Object)
	 */
	public void afterRead(Object item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#beforeRead()
	 */
	public void beforeRead() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#onReadError(java.lang.Exception)
	 */
	public void onReadError(Exception ex) {
	}

}
