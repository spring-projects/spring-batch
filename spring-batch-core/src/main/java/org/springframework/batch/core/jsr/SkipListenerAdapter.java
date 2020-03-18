/*
 * Copyright 2013-2014 the original author or authors.
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

import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.SkipListener;

import java.util.List;

public class SkipListenerAdapter<T, S> implements SkipListener<T, S> {
	private final SkipReadListener skipReadDelegate;
	private final SkipProcessListener skipProcessDelegate;
	private final SkipWriteListener skipWriteDelegate;

	public SkipListenerAdapter(SkipReadListener skipReadDelegate, SkipProcessListener skipProcessDelegate, SkipWriteListener skipWriteDelegate) {
		this.skipReadDelegate = skipReadDelegate;
		this.skipProcessDelegate = skipProcessDelegate;
		this.skipWriteDelegate = skipWriteDelegate;
	}

	@Override
	public void onSkipInRead(Throwable t) {
		if(skipReadDelegate != null && t instanceof Exception) {
			try {
				skipReadDelegate.onSkipReadItem((Exception) t);
			} catch (Exception e) {
				throw new BatchRuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onSkipInWrite(S item, Throwable t) {
		if(skipWriteDelegate != null && t instanceof Exception) {
			try {
				/*
				 * assuming this SkipListenerAdapter will only be called from JsrFaultTolerantChunkProcessor,
				 * which calls onSkipInWrite() with the whole chunk (List) of items instead of single item 
				 */
				skipWriteDelegate.onSkipWriteItem((List<Object>) item, (Exception) t);
			} catch (Exception e) {
				throw new BatchRuntimeException(e);
			}
		}
	}

	@Override
	public void onSkipInProcess(T item, Throwable t) {
		if(skipProcessDelegate != null && t instanceof Exception) {
			try {
				skipProcessDelegate.onSkipProcessItem(item, (Exception) t);
			} catch (Exception e) {
				throw new BatchRuntimeException(e);
			}
		}
	}
}
