/*
 * Copyright 2006-2021 the original author or authors.
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

package org.springframework.batch.sample.common;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;

/**
 * Simple module implementation that will always return true to indicate that processing
 * should continue. This is useful for testing graceful shutdown of jobs.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public class InfiniteLoopWriter implements StepExecutionListener, ItemWriter<Object> {

	private static final Log LOG = LogFactory.getLog(InfiniteLoopWriter.class);

	private StepExecution stepExecution;

	private int count = 0;

	/**
	 * @see org.springframework.batch.core.StepExecutionListener#beforeStep(StepExecution)
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	public InfiniteLoopWriter() {
		super();
	}

	@Override
	public void write(List<? extends Object> items) throws Exception {
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Job interrupted.", e);
		}

		stepExecution.setWriteCount(++count);
		if (LOG.isInfoEnabled()) {
			LOG.info("Executing infinite loop, at count=" + count);
		}
	}

}
