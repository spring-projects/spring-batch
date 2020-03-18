/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.batch.core.step;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Minella
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RestartLoopTests {

	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Test
	public void test() throws Exception {
		// Run 1
		JobExecution jobExecution1 = jobLauncher.run(job, new JobParameters());

		assertEquals(BatchStatus.STOPPED, jobExecution1.getStatus());

		// Run 2
		JobExecution jobExecution2 = jobLauncher.run(job, new JobParameters());

		assertEquals(BatchStatus.STOPPED, jobExecution2.getStatus());
	}

	public static class DefaultTasklet implements Tasklet {
		@Nullable
		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			return RepeatStatus.FINISHED;
		}
	}
}
