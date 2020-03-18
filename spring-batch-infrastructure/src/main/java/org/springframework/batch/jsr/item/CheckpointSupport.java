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

import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;

/**
 * Provides support for JSR-352 checkpointing.  Checkpoint objects are copied prior
 * to being added to the {@link ExecutionContext} for persistence by the framework.
 * If the checkpoint object cannot be copied and further changes occur to the same
 * instance, side effects may occur.  In cases like this, it is recommended that a
 * copy of the object being acted upon in the reader/writer is returned via the
 * {@link ItemReader#checkpointInfo()} or {@link ItemWriter#checkpointInfo()} calls.
 *
 * @author Michael Minella
 * @since 3.0
 */
public abstract class CheckpointSupport extends ItemStreamSupport{

	private final Log logger = LogFactory.getLog(this.getClass());

	private final String checkpointKey;

	/**
	 * @param checkpointKey key to store the checkpoint object with in the {@link ExecutionContext}
	 */
	public CheckpointSupport(String checkpointKey) {
		Assert.hasText(checkpointKey, "checkpointKey is required");
		this.checkpointKey = checkpointKey;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStreamSupport#open(org.springframework.batch.item.ExecutionContext)
	 */
	@Override
	public void open(ExecutionContext executionContext)
			throws ItemStreamException {
		try {
			String executionContextKey = getExecutionContextKey(checkpointKey);
			Serializable checkpoint = (Serializable) executionContext.get(executionContextKey);
			doOpen(checkpoint);
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	/**
	 * Used to open a batch artifact with previously saved checkpoint information.
	 *
	 * @param checkpoint previously saved checkpoint object
	 * @throws Exception thrown by the implementation
	 */
	protected abstract void doOpen(Serializable checkpoint) throws Exception;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStreamSupport#update(org.springframework.batch.item.ExecutionContext)
	 */
	@Override
	public void update(ExecutionContext executionContext)
			throws ItemStreamException {
		try {
			executionContext.put(getExecutionContextKey(checkpointKey), deepCopy(doCheckpoint()));
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	/**
	 * Used to provide a {@link Serializable} representing the current state of the
	 * batch artifact.
	 *
	 * @return the current state of the batch artifact
	 * @throws Exception thrown by the implementation
	 */
	protected abstract Serializable doCheckpoint() throws Exception;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStreamSupport#close()
	 */
	@Override
	public void close() throws ItemStreamException {
		try {
			doClose();
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	/**
	 * Used to close the underlying batch artifact
	 *
	 * @throws Exception thrown by the underlying implementation
	 */
	protected abstract void doClose() throws Exception;

	private Object deepCopy(Serializable orig) {
		Object obj = orig;

		try {
			obj = SerializationUtils.deserialize(SerializationUtils.serialize(orig));
		} catch (Exception e) {
			logger.warn("Unable to copy checkpoint object.  Updating the instance passed may cause side effects");
		}

		return obj;
	}

}
