/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.jsr.item;

import java.io.Serializable;

import javax.batch.api.chunk.ItemReader;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Adapter that wraps an {@link ItemReader} for use by Spring Batch.  All calls are delegated as appropriate
 * to the corresponding method on the delegate.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class ItemReaderAdapter<T> extends CheckpointSupport implements org.springframework.batch.item.ItemReader<T> {

	private static final String CHECKPOINT_KEY = "reader.checkpoint";

	private ItemReader delegate;

	/**
	 * @param reader the {@link ItemReader} implementation to delegate to
	 */
	public ItemReaderAdapter(ItemReader reader) {
		super(CHECKPOINT_KEY);
		Assert.notNull(reader, "An ItemReader implementation is required");
		this.delegate = reader;
		setExecutionContextName(ClassUtils.getShortName(delegate.getClass()));
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	@Override
	public T read() throws Exception {
		return (T) delegate.readItem();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doClose()
	 */
	@Override
	protected void doClose() throws Exception{
		delegate.close();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doCheckpoint()
	 */
	@Override
	protected Serializable doCheckpoint() throws Exception {
		return delegate.checkpointInfo();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doOpen(java.io.Serializable)
	 */
	@Override
	protected void doOpen(Serializable checkpoint) throws Exception {
		delegate.open(checkpoint);
	}
}
