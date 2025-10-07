/*
 * Copyright 2013-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig
// FIXME this test fails when upgrading the batch xsd from 2.2 to 3.0:
@Disabled("https://github.com/spring-projects/spring-batch/issues/1287")
class RestartInPriorStepTests {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@Test
	void test() throws Exception {
		JobExecution run1 = jobOperator.start(job, new JobParameters());

		assertEquals(BatchStatus.STOPPED, run1.getStatus());
		assertEquals(2, run1.getStepExecutions().size());

		JobExecution run2 = jobOperator.start(job, new JobParameters());

		assertEquals(BatchStatus.COMPLETED, run2.getStatus());
		assertEquals(6, run2.getStepExecutions().size());
	}

	public static class DecidingTasklet implements Tasklet {

		@Override
		public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
				throws Exception {
			Map<String, Object> context = chunkContext.getStepContext().getJobExecutionContext();

			if (context.get("restart") != null) {
				contribution.setExitStatus(new ExitStatus("ES3"));
			}
			else {
				chunkContext.getStepContext().setAttribute("restart", true);
				contribution.setExitStatus(new ExitStatus("ES4"));
			}

			return RepeatStatus.FINISHED;
		}

	}

	public static class CompletionDecider implements JobExecutionDecider {

		private int count = 0;

		@Override
		public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
			count++;

			if (count > 2) {
				return new FlowExecutionStatus("END");
			}
			else {
				return new FlowExecutionStatus("CONTINUE");
			}
		}

	}

}
