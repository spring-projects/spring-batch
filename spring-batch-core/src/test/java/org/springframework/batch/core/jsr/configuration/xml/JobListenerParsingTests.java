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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.batch.api.listener.JobListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobListenerParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public SpringJobListener springListener;

	@Autowired
	public JsrJobListener jsrListener;

	@Test
	public void test() throws Exception {
		assertNotNull(job);
		assertEquals("job1", job.getName());

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
		assertEquals(1, springListener.countAfterJob);
		assertEquals(1, springListener.countBeforeJob);
		assertEquals(1, jsrListener.countAfterJob);
		assertEquals(1, jsrListener.countBeforeJob);
	}

	public static class SpringJobListener implements JobExecutionListener {

		protected int countBeforeJob = 0;
		protected int countAfterJob = 0;

		@Override
		public void beforeJob(JobExecution jobExecution) {
			countBeforeJob++;
		}

		@Override
		public void afterJob(JobExecution jobExecution) {
			countAfterJob++;
		}
	}

	public static class JsrJobListener implements JobListener {

		protected int countBeforeJob = 0;
		protected int countAfterJob = 0;

		@Override
		public void afterJob() throws Exception {
			countBeforeJob++;
		}

		@Override
		public void beforeJob() throws Exception {
			countAfterJob++;
		}
	}
}
