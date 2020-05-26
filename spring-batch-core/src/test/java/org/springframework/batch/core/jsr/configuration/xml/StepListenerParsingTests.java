/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;

import javax.batch.api.listener.StepListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepListenerParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public SpringStepListener springStepListener;

	@Autowired
	public JsrStepListener jsrStepListener;

	@Test
	public void test() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
		assertEquals(2, springStepListener.countBeforeStep);
		assertEquals(2, springStepListener.countAfterStep);
		assertEquals(2, jsrStepListener.countBeforeStep);
		assertEquals(2, jsrStepListener.countAfterStep);
	}

	public static class SpringStepListener implements StepExecutionListener {
		protected int countBeforeStep = 0;
		protected int countAfterStep = 0;

		@Override
		public void beforeStep(StepExecution stepExecution) {
			countBeforeStep++;
		}

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			countAfterStep++;
			return null;
		}
	}

	public static class JsrStepListener implements StepListener {
		protected int countBeforeStep = 0;
		protected int countAfterStep = 0;

		@Override
		public void beforeStep() throws Exception {
			countBeforeStep++;
		}

		@Override
		public void afterStep() throws Exception {
			countAfterStep++;
		}
	}
}
