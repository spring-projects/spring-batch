/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.samples.skip;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Error is encountered during writing - transaction is rolled back and the error item is
 * skipped on second attempt to process the chunk.
 *
 * @author Robert Kasanicky
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 */
class SkipSampleFunctionalTests {

	/*
	 * When a skippable exception is thrown during reading, the item is skipped from the
	 * chunk and is not passed to the chunk processor (So it will not be processed nor
	 * written).
	 */
	@Test
	void testSkippableExceptionDuringRead() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(SkippableExceptionDuringReadSample.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	/*
	 * When a skippable exception is thrown during processing, items will re-processed one
	 * by one and the faulty item will be skipped from the chunk (it will not be passed to
	 * the writer).
	 */
	@Test
	void testSkippableExceptionDuringProcess() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				SkippableExceptionDuringProcessSample.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	/*
	 * When a skippable exception is thrown during writing, the item writer (which
	 * receives a chunk of items) does not know which item caused the issue. Hence, it
	 * will "scan" the chunk item by item and only the faulty item will be skipped
	 * (technically, the commit-interval will be re-set to 1 and each item will
	 * re-processed/re-written in its own transaction).
	 */
	@Test
	void testSkippableExceptionDuringWrite() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(SkippableExceptionDuringWriteSample.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

}
