/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.core.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemStreamReader} with hard-coded input data.
 */
public class ExampleItemReader extends AbstractItemStreamItemReader<String> {

	private Log logger = LogFactory.getLog(getClass());

	private String[] input = { "Hello", "world!", "Go", "on", "punk", "make", "my", "day!" };

	private int index = 0;

	private int min = 0;

	private int max = Integer.MAX_VALUE;

	public static volatile boolean fail = false;

        public ExampleItemReader() {
                this.setExecutionContextName(ClassUtils.getShortName(this.getClass()));
        }
        
	/**
	 * @param min the min to set
	 */
	public void setMin(int min) {
		this.min = min;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(int max) {
		this.max = max;
	}

	/**
	 * Reads next record from input
	 */
	@Nullable
	@Override
	public String read() throws Exception {
		if (index >= input.length || index >= max) {
			return null;
		}
		logger.info(String.format("Processing input index=%s, item=%s, in (%s)", index, input[index], this));
		if (fail && index == 4) {
			synchronized (ExampleItemReader.class) {
				if (fail) {
					// Only fail once per flag setting...
					fail = false;
					logger.info(String.format("Throwing exception index=%s, item=%s, in (%s)", index, input[index],
							this));
					index++;
					throw new RuntimeException("Planned failure");
				}
			}
		}
		return input[index++];
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
                super.open(executionContext);
		index = (int) executionContext.getLong(getExecutionContextKey("POSITION"), min);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
                super.update(executionContext);
		executionContext.putLong(getExecutionContextKey("POSITION"), index);
	}

}
