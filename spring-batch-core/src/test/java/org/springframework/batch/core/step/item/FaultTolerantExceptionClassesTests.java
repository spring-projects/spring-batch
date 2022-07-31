/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0.2
 */
@SpringJUnitConfig
public class FaultTolerantExceptionClassesTests implements ApplicationContextAware {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private SkipReaderStub<String> reader;

	@Autowired
	private SkipWriterStub<String> writer;

	@Autowired
	private ExceptionThrowingTaskletStub tasklet;

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setup() {
		reader.clear();
		writer.clear();
		tasklet.clear();
	}

	@Test
	void testNonSkippable() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("nonSkippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testNonSkippableChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("nonSkippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testSkippable() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	void testRegularRuntimeExceptionNotSkipped() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1327:
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// BATCH-1327:
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testFatalOverridesSkippable() throws Exception {
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("skippableFatalStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testDefaultFatalChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("skippableFatalStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1327:
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// BATCH-1327:
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testSkippableChecked() throws Exception {
		writer.setExceptionType(SkippableException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	void testNonSkippableUnchecked() throws Exception {
		writer.setExceptionType(UnexpectedRollbackException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testFatalChecked() throws Exception {
		writer.setExceptionType(FatalSkippableException.class);
		StepExecution stepExecution = launchStep("skippableFatalStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableButNotSkippable() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		// BATCH-1327:
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableSkippable() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableFatal() throws Exception {
		// User wants all exceptions to be retried, but only some are skippable
		// FatalRuntimeException is not skippable because it is fatal, but is a
		// subclass of another skippable
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1333:
		assertEquals("[1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableButNotSkippableChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		// BATCH-1327:
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableSkippableChecked() throws Exception {
		writer.setExceptionType(SkippableException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	void testRetryableFatalChecked() throws Exception {
		writer.setExceptionType(FatalSkippableException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1333:
		assertEquals("[1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	@Test
	void testNoRollbackDefaultRollbackException() throws Exception {
		// Exception is neither no-rollback nor skippable
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("noRollbackDefault");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1318:
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// BATCH-1318:
		assertEquals("[]", writer.getCommitted().toString());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	@Test
	void testNoRollbackDefaultNoRollbackException() throws Exception {
		// Exception is no-rollback and not skippable
		writer.setExceptionType(IllegalStateException.class);
		StepExecution stepExecution = launchStep("noRollbackDefault");
		assertNotNull(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// BATCH-1334:
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		// BATCH-1334:
		assertEquals("[1, 2, 3, 4]", writer.getCommitted().toString());
		// BATCH-1334:
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	@Test
	void testNoRollbackPathology() throws Exception {
		// Exception is neither no-rollback nor skippable and no-rollback is
		// RuntimeException (potentially pathological because other obviously
		// rollback signalling Exceptions also extend RuntimeException)
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("noRollbackPathology");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1335:
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// BATCH-1335:
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testNoRollbackSkippableRollbackException() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackSkippable");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	void testNoRollbackSkippableNoRollbackException() throws Exception {
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackSkippable");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// BATCH-1332:
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		// BATCH-1334:
		// Skipped but also committed (because it was marked as no-rollback)
		assertEquals("[1, 2, 3, 4]", writer.getCommitted().toString());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testNoRollbackFatalRollbackException() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackFatal");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	void testNoRollbackFatalNoRollbackException() throws Exception {
		// User has asked for no rollback on a fatal exception. What should the
		// outcome be? As per BATCH-1333 it is interpreted as not skippable, but
		// retryable if requested. Here it was not requested to be retried, but
		// it was marked as no-rollback. As per BATCH-1334 this has to be ignored
		// so that the failed item can be isolated.
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackFatal");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// BATCH-1331:
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		// BATCH-1331:
		assertEquals("[1, 2, 3, 4]", writer.getCommitted().toString());
	}

	@Test
	@DirtiesContext
	void testNoRollbackTaskletRollbackException() throws Exception {
		tasklet.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackTasklet");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[]", tasklet.getCommitted().toString());
	}

	@Test
	@DirtiesContext
	void testNoRollbackTaskletNoRollbackException() throws Exception {
		tasklet.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("noRollbackTasklet");
		// assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// BATCH-1298:
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 1, 1, 1]", tasklet.getCommitted().toString());
	}

	private StepExecution launchStep(String stepName) throws Exception {
		SimpleJob job = new SimpleJob();
		job.setName("job");
		job.setJobRepository(jobRepository);

		List<Step> stepsToExecute = new ArrayList<>();
		stepsToExecute.add((Step) applicationContext.getBean(stepName));
		job.setSteps(stepsToExecute);

		JobExecution jobExecution = jobLauncher.run(job,
				new JobParametersBuilder().addString("uuid", UUID.randomUUID().toString()).toJobParameters());
		return jobExecution.getStepExecutions().iterator().next();
	}

}
