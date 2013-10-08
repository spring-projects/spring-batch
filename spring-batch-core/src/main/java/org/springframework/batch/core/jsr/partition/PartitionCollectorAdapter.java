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
package org.springframework.batch.core.jsr.partition;

import java.io.Serializable;
import java.util.Queue;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionCollector;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Adapter class used to wrap a {@link PartitionCollector} so that it can be consumed
 * as a {@link ChunkListener}.  A thread safe {@link Queue} is required along with the
 * {@link PartitionCollector}.  The {@link Queue} is where the result of the call to
 * the PartitionCollector will be placed.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class PartitionCollectorAdapter implements ChunkListener, InitializingBean {

	private PartitionCollector collector;
	private Queue<Serializable> partitionQueue;

	/**
	 * @param queue destination for results of each {@link PartitionCollector#collectPartitionData()} call.
	 */
	public void setPartitionQueue(Queue<Serializable> queue) {
		this.partitionQueue = queue;
	}

	/**
	 * @param collector Provides partition specific information back to the {@link PartitionAnalyzer} as needed.
	 */
	public void setPartitionCollector(PartitionCollector collector) {
		this.collector = collector;
	}

	@Override
	public void beforeChunk(ChunkContext context) {
	}

	@Override
	public void afterChunk(ChunkContext context) {
		try {
			partitionQueue.add(collector.collectPartitionData());
		} catch (Exception e) {
			throw new PartitionException(e);
		}
	}

	@Override
	public void afterChunkError(ChunkContext context) {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(collector, "A PartitionCollector instance is required");
		Assert.notNull(partitionQueue, "A thread safe Queue instance is required");
	}
}
