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

package org.springframework.batch.sample.tasklet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple module implementation that will always return true to indicate that
 * processing should continue. This is useful for testing graceful shutdown of
 * jobs.
 * 
 * @author Lucas Ward
 * 
 */
public class InfiniteLoopTasklet extends StepListenerSupport implements Tasklet {

	private StepExecution stepExecution;
	private int count = 0;
	private static final Log logger = LogFactory.getLog(InfiniteLoopTasklet.class);

	/**
	 * 
	 */
	public InfiniteLoopTasklet() {
		super();
	}

	public ExitStatus execute() throws Exception {
		while(!stepExecution.isTerminateOnly()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Job interrupted.");
			}
			count++;
			logger.info("Executing infinite loop, at count="+count);
		}
		return ExitStatus.FAILED;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.listener.StepListenerSupport#beforeStep(org.springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}
