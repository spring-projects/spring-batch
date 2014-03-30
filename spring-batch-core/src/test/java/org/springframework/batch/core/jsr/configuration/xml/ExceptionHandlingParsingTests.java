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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ExceptionHandlingParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Test
	@Ignore
	public void testSkippable() throws Exception {
		JobExecution execution1 = jobLauncher.run(job, new JobParametersBuilder().addLong("run", 1L).toJobParameters());
		assertEquals(BatchStatus.FAILED, execution1.getStatus());
		assertEquals(1, execution1.getStepExecutions().size());
		assertEquals(1, execution1.getStepExecutions().iterator().next().getSkipCount());
		assertTrue(execution1.getAllFailureExceptions().get(0).getMessage().contains("But don't skip me"));

		JobExecution execution2 = jobLauncher.run(job, new JobParametersBuilder().addLong("run", 2L).toJobParameters());
		assertEquals(BatchStatus.FAILED, execution2.getStatus());
		assertEquals(2, execution2.getStepExecutions().size());
		assertTrue(execution2.getAllFailureExceptions().get(0).getMessage().contains("But don't retry me"));

		JobExecution execution3 = jobLauncher.run(job, new JobParametersBuilder().addLong("run", 3L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution3.getStatus());
		assertEquals(3, execution3.getStepExecutions().size());

		List<StepExecution> stepExecutions = new ArrayList<StepExecution>(execution3.getStepExecutions());
		assertEquals(0, stepExecutions.get(2).getRollbackCount());

		JobExecution execution4 = jobLauncher.run(job, new JobParametersBuilder().addLong("run", 4L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution4.getStatus());
		assertEquals(3, execution4.getStepExecutions().size());
	}

	public static class ProblemProcessor implements ItemProcessor<String, String> {

		private long runId = 0;
		private boolean hasRetried = false;

		public void setRunId(long id) {
			this.runId = id;
		}

		@Override
		public String process(String item) throws Exception {
			throwException(item);
			return item;
		}

		private void throwException(String item) throws Exception {
			if(runId == 1) {
				if(item.equals("One")) {
					throw new Exception("skip me");
				} else if(item.equals("Two")){
					throw new RuntimeException("But don't skip me");
				}
			} else if(runId == 2) {
				if(item.equals("Three") && !hasRetried) {
					hasRetried = true;
					throw new Exception("retry me");
				} else if(item.equals("Four")){
					throw new RuntimeException("But don't retry me");
				}
			} else if(runId == 3) {
				if(item.equals("Five")) {
					throw new Exception("Don't rollback on my account");
				}
			}
		}
	}
}
