/*
 * Copyright 2009-2023 the original author or authors.
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
package org.springframework.batch.integration.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemStreamException;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class ExampleItemReader implements ItemReader<String>, ItemStream {

	private final Log logger = LogFactory.getLog(getClass());

	private final String[] input = { "Hello", "world!", "Go", "on", "punk", "make", "my", "day!" };

	private int index = 0;

	public static volatile boolean fail = false;

	/**
	 * Reads next record from input
	 */
	@Override
	public @Nullable String read() throws Exception {
		if (index >= input.length) {
			return null;
		}
		logger.info(String.format("Processing input index=%s, item=%s, in (%s)", index, input[index], this));
		if (fail && index == 4) {
			synchronized (ExampleItemReader.class) {
				if (fail) {
					// Only fail once per flag setting...
					fail = false;
					logger.info(
							String.format("Throwing exception index=%s, item=%s, in (%s)", index, input[index], this));
					index++;
					throw new RuntimeException("Planned failure");
				}
			}
		}
		return input[index++];
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		index = (int) executionContext.getLong("POSITION", 0);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putLong("POSITION", index);
	}

}
