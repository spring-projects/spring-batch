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
package org.springframework.batch.jsr.item;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Adapter that wraps an {@link ItemWriter} for use by Spring Batch.  All calls are delegated as appropriate
 * to the corresponding method on the delegate.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class ItemWriterAdapter<T> extends CheckpointSupport implements org.springframework.batch.item.ItemWriter<T> {

	private static final String CHECKPOINT_KEY = "writer.checkpoint";

	private ItemWriter delegate;

	/**
	 * @param writer a {@link ItemWriter} to delegate calls to
	 */
	public ItemWriterAdapter(ItemWriter writer) {
		super(CHECKPOINT_KEY);
		Assert.notNull(writer, "An ItemWriter implementation is required");
		this.delegate = writer;
		super.setExecutionContextName(ClassUtils.getShortName(delegate.getClass()));
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void write(List<? extends T> items) throws Exception {
		delegate.writeItems((List<Object>) items);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doOpen(java.io.Serializable)
	 */
	@Override
	protected void doOpen(Serializable checkpoint) throws Exception {
		delegate.open(checkpoint);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doCheckpoint()
	 */
	@Override
	protected Serializable doCheckpoint() throws Exception {
		Serializable checkpointInfo = delegate.checkpointInfo();
		return checkpointInfo;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.jsr.item.CheckpointSupport#doClose()
	 */
	@Override
	protected void doClose() throws Exception{
		delegate.close();
	}
}
