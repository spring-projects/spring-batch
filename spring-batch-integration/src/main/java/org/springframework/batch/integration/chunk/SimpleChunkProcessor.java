/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class SimpleChunkProcessor<S, T> implements ChunkProcessor<S>, InitializingBean {

	private ItemProcessor<? super S, ? extends T> itemProcessor;

	private ItemWriter<? super T> itemWriter;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemProcessor, "An ItemProcessor must be provided");
		Assert.notNull(itemWriter, "An ItemWriter must be provided");
	}

	/**
	 * @param itemWriter
	 */
	public void setItemWriter(ItemWriter<? super T> itemWriter) {
		this.itemWriter = itemWriter;
	}
	
	/**
	 * Public setter for the {@link ItemProcessor}.
	 * @param itemProcessor the {@link ItemProcessor} to set
	 */
	public void setItemProcessor(ItemProcessor<? super S, ? extends T> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.integration.chunk.ChunkProcessor#process(java.util.Collection,
	 * int)
	 */
	public int process(Collection<? extends S> items, int parentSkipCount) throws Exception {

		List<T> processed = new ArrayList<T>();
		for (S item : items) {
			processed.add(itemProcessor.process(item));
		}
		itemWriter.write(processed);

		return 0;

	}

}
