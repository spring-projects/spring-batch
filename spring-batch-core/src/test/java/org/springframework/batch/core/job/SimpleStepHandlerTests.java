/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.job;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepHandlerTests {

	private JobRepository jobRepository;

	private JobExecution jobExecution;

	private SimpleStepHandler stepHandler;

	@Before
	public void setUp() throws Exception {
		MapJobRepositoryFactoryBean jobRepositoryFactoryBean = new MapJobRepositoryFactoryBean();
		jobRepository = jobRepositoryFactoryBean.getJobRepository();
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		stepHandler = new SimpleStepHandler(jobRepository);
		stepHandler.afterPropertiesSet();
	}

	/**
	 * Test method for {@link SimpleStepHandler#afterPropertiesSet()}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testAfterPropertiesSet() throws Exception {
		SimpleStepHandler stepHandler = new SimpleStepHandler();
		stepHandler.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link SimpleStepHandler#handleStep(org.springframework.batch.core.Step, org.springframework.batch.core.JobExecution)}
	 * .
	 */
	@Test
	public void testHandleStep() throws Exception {
		StepExecution stepExecution = stepHandler.handleStep(new StubStep("step"), jobExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	private class StubStep extends StepSupport {

		private StubStep(String name) {
			super(name);
		}

		public void execute(StepExecution stepExecution) throws JobInterruptedException {
			stepExecution.setStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}

	}

}
