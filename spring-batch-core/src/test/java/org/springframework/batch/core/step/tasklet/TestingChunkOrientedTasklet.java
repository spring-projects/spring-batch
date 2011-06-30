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
package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * Simplest possible implementation of {@link Tasklet} with no skipping or
 * recovering or processing. Just delegates all calls to the provided
 * {@link ItemReader} and {@link ItemWriter}.
 * 
 * @author Dave Syer
 */
public class TestingChunkOrientedTasklet<T> extends ChunkOrientedTasklet<T> {

	/**
	 * 
	 */
	private static final RepeatTemplate repeatTemplate = new RepeatTemplate();

	static {
		// It's only for testing, and we don't want any infinite loops...
		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(6));
	}

	/**
	 * Creates a {@link PassThroughItemProcessor} and uses it to create an
	 * instance of {@link Tasklet}.
	 */
	public TestingChunkOrientedTasklet(ItemReader<T> itemReader, ItemWriter<T> itemWriter) {
		this(itemReader, itemWriter, repeatTemplate);
	}

	/**
	 * Creates a {@link PassThroughItemProcessor} and uses it to create an
	 * instance of {@link Tasklet}.
	 */
	public TestingChunkOrientedTasklet(ItemReader<T> itemReader, ItemProcessor<T, T> itemProcessor, ItemWriter<T> itemWriter,
			RepeatOperations repeatOperations) {
		super(new SimpleChunkProvider<T>(itemReader, repeatOperations), new SimpleChunkProcessor<T, T>(
				itemProcessor, itemWriter));
	}

	/**
	 * Creates a {@link PassThroughItemProcessor} and uses it to create an
	 * instance of {@link Tasklet}.
	 */
	public TestingChunkOrientedTasklet(ItemReader<T> itemReader, ItemWriter<T> itemWriter,
			RepeatOperations repeatOperations) {
		this(itemReader, new PassThroughItemProcessor<T>(), itemWriter, repeatOperations);
	}

}
