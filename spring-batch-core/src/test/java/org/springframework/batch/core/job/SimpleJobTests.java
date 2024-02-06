/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core.job;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.observability.BatchJobObservation;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for DefaultJobLifecycle.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Jinwoo Bae
 */
class SimpleJobTests {

	private JobRepository jobRepository;

	private JobExplorer jobExplorer;

	private final List<Serializable> list = new ArrayList<>();

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private StubStep step1;

	private StubStep step2;

	private final JobParameters jobParameters = new JobParameters();

	private SimpleJob job;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.generateUniqueName(true)
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(embeddedDatabase);
		repositoryFactoryBean.setTransactionManager(transactionManager);
		repositoryFactoryBean.afterPropertiesSet();
		this.jobRepository = repositoryFactoryBean.getObject();
		JobExplorerFactoryBean explorerFactoryBean = new JobExplorerFactoryBean();
		explorerFactoryBean.setDataSource(embeddedDatabase);
		explorerFactoryBean.setTransactionManager(transactionManager);
		explorerFactoryBean.afterPropertiesSet();
		this.jobExplorer = explorerFactoryBean.getObject();
		job = new SimpleJob();
		job.setJobRepository(jobRepository);

		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig()
			.observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));
		job.setObservationRegistry(observationRegistry);

		step1 = new StubStep("TestStep1", jobRepository);
		step1.setCallback(() -> list.add("default"));
		step2 = new StubStep("TestStep2", jobRepository);
		step2.setCallback(() -> list.add("default"));

		List<Step> steps = new ArrayList<>();
		steps.add(step1);
		steps.add(step2);
		job.setName("testJob");
		job.setSteps(steps);

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobInstance = jobExecution.getJobInstance();

		stepExecution1 = new StepExecution(step1.getName(), jobExecution);
		stepExecution2 = new StepExecution(step2.getName(), jobExecution);

	}

	/**
	 * Test method for {@link SimpleJob#setSteps(java.util.List)}.
	 */
	@Test
	void testSetSteps() {
		job.setSteps(Collections.singletonList((Step) new StepSupport("step")));
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	/**
	 * Test method for {@link SimpleJob#setSteps(java.util.List)}.
	 */
	@Test
	void testGetSteps() {
		assertEquals(2, job.getStepNames().size());
	}

	/**
	 * Test method for {@link SimpleJob#addStep(org.springframework.batch.core.Step)}.
	 */
	@Test
	void testAddStep() {
		job.setSteps(Collections.<Step>emptyList());
		job.addStep(new StepSupport("step"));
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	// Test to ensure the exit status returned by the last step is returned
	@Test
	void testExitStatusReturned() {

		final ExitStatus customStatus = new ExitStatus("test");

		Step testStep = new Step() {

			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.setExitStatus(customStatus);
			}

			@Override
			public String getName() {
				return "test";
			}

			@Override
			public int getStartLimit() {
				return 1;
			}

			@Override
			public boolean isAllowStartIfComplete() {
				return false;
			}
		};
		List<Step> steps = new ArrayList<>();
		steps.add(testStep);
		job.setSteps(steps);
		job.execute(jobExecution);
		assertEquals(customStatus, jobExecution.getExitStatus());
	}

	@Test
	void testRunNormally() {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED);
		assertNotNull(jobExecution.getEndTime());
		assertNotNull(jobExecution.getStartTime());

		assertEquals(1, step1.passedInJobContext.size());
		assertFalse(step2.passedInJobContext.isEmpty());

		// Observability
		MeterRegistryAssert.assertThat(Metrics.globalRegistry)
			.hasTimerWithNameAndTags(BatchJobObservation.BATCH_JOB_OBSERVATION.getName(),
					Tags.of(Tag.of("error", "none"), Tag.of("spring.batch.job.name", "testJob"),
							Tag.of("spring.batch.job.status", "COMPLETED")));
	}

	@AfterEach
	void cleanup() {
		Metrics.globalRegistry.clear();
	}

	@Test
	void testRunNormallyWithListener() {
		job.setJobExecutionListeners(new JobExecutionListener[] { new JobExecutionListener() {
			@Override
			public void beforeJob(JobExecution jobExecution) {
				list.add("before");
			}

			@Override
			public void afterJob(JobExecution jobExecution) {
				list.add("after");
			}
		} });
		job.execute(jobExecution);
		assertEquals(4, list.size());
	}

	@Test
	void testRunWithSimpleStepExecutor() {

		job.setJobRepository(jobRepository);
		// do not set StepExecutorFactory...
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.COMPLETED);

	}

	@Test
	void testExecutionContextIsSet() {
		testRunNormally();
		assertEquals(jobInstance, jobExecution.getJobInstance());
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals(step1.getName(), stepExecution1.getStepName());
		assertEquals(step2.getName(), stepExecution2.getStepName());
	}

	@Test
	void testInterrupted() {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final JobInterruptedException exception = new JobInterruptedException("Interrupt!");
		step1.setProcessException(exception);
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getStepExecutions().iterator().next().getFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
	}

	@Test
	void testInterruptedAfterUnknownStatus() {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final JobInterruptedException exception = new JobInterruptedException("Interrupt!", BatchStatus.UNKNOWN);
		step1.setProcessException(exception);
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getStepExecutions().iterator().next().getFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.UNKNOWN, ExitStatus.STOPPED);
	}

	@Test
	void testFailed() {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(0, list.size());
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	void testFailedWithListener() {
		job.setJobExecutionListeners(new JobExecutionListener[] { new JobExecutionListener() {
			@Override
			public void afterJob(JobExecution jobExecution) {
				list.add("afterJob");
			}
		} });
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(1, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	void testFailedWithError() {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final Error exception = new Error("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	void testStepShouldNotStart() {
		// Start policy will return false, keeping the step from being started.
		step1.setStartLimit(0);

		job.execute(jobExecution);

		assertEquals(1, jobExecution.getFailureExceptions().size());
		Throwable ex = jobExecution.getFailureExceptions().get(0);
		assertTrue(ex.getMessage().contains("start limit exceeded"), "Wrong message in exception: " + ex.getMessage());
	}

	@Test
	void testStepAlreadyComplete() throws Exception {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		jobRepository.add(stepExecution1);
		jobExecution.setEndTime(LocalDateTime.now());
		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobRepository.update(jobExecution);
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		job.execute(jobExecution);
		assertEquals(0, jobExecution.getFailureExceptions().size());
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals(stepExecution2.getStepName(), jobExecution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	void testStepAlreadyCompleteInSameExecution() throws Exception {
		List<Step> steps = new ArrayList<>();
		steps.add(step1);
		steps.add(step2);
		// Two steps with the same name should both be executed, since
		// the user might actually want it to happen twice. On a restart
		// it would be executed twice again, even if it failed on the
		// second execution. This seems reasonable.
		steps.add(step2);
		job.setSteps(steps);
		job.execute(jobExecution);
		assertEquals(0, jobExecution.getFailureExceptions().size());
		assertEquals(3, jobExecution.getStepExecutions().size());
		assertEquals(stepExecution1.getStepName(), jobExecution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	void testNoSteps() {
		job.setSteps(new ArrayList<>());

		job.execute(jobExecution);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertTrue(exitStatus.getExitDescription().contains("no steps configured"),
				"Wrong message in execution: " + exitStatus);
	}

	@Test
	void testRestart() throws Exception {
		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		job.execute(jobExecution);
		Throwable e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		job.execute(jobExecution);
		e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);
		assertTrue(step1.passedInStepContext.isEmpty());
		assertFalse(step2.passedInStepContext.isEmpty());
	}

	@Test
	void testInterruptWithListener() {
		step1.setProcessException(new JobInterruptedException("job interrupted!"));

		JobExecutionListener listener = mock();
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);

		job.setJobExecutionListeners(new JobExecutionListener[] { listener });

		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());

	}

	/**
	 * Execution context should be restored on restart.
	 */
	@Test
	void testRestartAndExecutionContextRestored() throws Exception {

		job.setRestartable(true);

		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		Throwable e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);

		assertEquals(1, step1.passedInJobContext.size());
		assertFalse(step2.passedInJobContext.isEmpty());

		assertFalse(jobExecution.getExecutionContext().isEmpty());

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);
		assertFalse(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());
	}

	@Test
	void testGetStepExists() {
		step1 = new StubStep("step1", jobRepository);
		step2 = new StubStep("step2", jobRepository);
		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));
		Step step = job.getStep("step2");
		assertNotNull(step);
		assertEquals("step2", step.getName());
	}

	@Test
	void testGetStepNotExists() {
		step1 = new StubStep("step1", jobRepository);
		step2 = new StubStep("step2", jobRepository);
		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));
		Step step = job.getStep("foo");
		assertNull(step);
	}

	@Test
	void testGetMultipleJobParameters() throws Exception {
		StubStep failStep = new StubStep("failStep", jobRepository);

		failStep.setCallback(() -> {
			throw new RuntimeException("An error occurred.");
		});

		job.setName("parametersTestJob");
		job.setSteps(Arrays.asList(new Step[] { failStep }));

		JobParameters firstJobParameters = new JobParametersBuilder().addString("JobExecutionParameter", "first", false)
			.toJobParameters();
		JobExecution jobexecution = jobRepository.createJobExecution(job.getName(), firstJobParameters);
		job.execute(jobexecution);

		List<JobExecution> jobExecutionList = jobExplorer.getJobExecutions(jobexecution.getJobInstance());

		assertEquals(jobExecutionList.size(), 1);
		assertEquals(jobExecutionList.get(0).getJobParameters().getString("JobExecutionParameter"), "first");

		JobParameters secondJobParameters = new JobParametersBuilder()
			.addString("JobExecutionParameter", "second", false)
			.toJobParameters();
		jobexecution = jobRepository.createJobExecution(job.getName(), secondJobParameters);
		job.execute(jobexecution);

		jobExecutionList = jobExplorer.getJobExecutions(jobexecution.getJobInstance());

		assertEquals(jobExecutionList.size(), 2);
		assertEquals(jobExecutionList.get(0).getJobParameters().getString("JobExecutionParameter"), "second");
		assertEquals(jobExecutionList.get(1).getJobParameters().getString("JobExecutionParameter"), "first");

	}

	@Test
	public void testMultipleStepsWithSameName(){
		job.setName("MultipleStepsWithSameName");
		String sharedName="stepName";
		final List<String> executionsCallbacks=new ArrayList<>();
		StubStep sharedNameStep1=new StubStep(sharedName, jobRepository);
		sharedNameStep1.setCallback(()->executionsCallbacks.add("step1"));
		job.addStep(sharedNameStep1);
		StubStep sharedNameStep2=new StubStep(sharedName, jobRepository);
		sharedNameStep2.setCallback(()->executionsCallbacks.add("step2"));
		job.addStep(sharedNameStep2);
		StubStep sharedNameStep3=new StubStep(sharedName, jobRepository);
		sharedNameStep3.setCallback(()->executionsCallbacks.add("step3"));
		job.addStep(sharedNameStep3);
		job.execute(jobExecution);
		assertEquals(List.of("step1", "step2", "step3"), executionsCallbacks);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		assertEquals(jobInstance,
				this.jobRepository.getLastJobExecution(job.getName(), jobParameters).getJobInstance());
		JobExecution jobExecution = this.jobExplorer.getJobExecutions(jobInstance).get(0);
		assertEquals(jobInstance.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if (exitStatus != null) {
			assertEquals(exitStatus.getExitCode(), jobExecution.getExitStatus().getExitCode());
		}
	}

	private void checkRepository(BatchStatus status) {
		checkRepository(status, null);
	}

	private static class StubStep extends StepSupport {

		private Runnable runnable;

		private Throwable exception;

		private final JobRepository jobRepository;

		private ExecutionContext passedInStepContext;

		private ExecutionContext passedInJobContext;

		/**
		 * @param string the step name
		 */
		public StubStep(String string, JobRepository jobRepository) {
			super(string);
			this.jobRepository = jobRepository;
		}

		public void setProcessException(Throwable exception) {
			this.exception = exception;
		}

		public void setCallback(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, UnexpectedJobExecutionException {

			passedInJobContext = new ExecutionContext(stepExecution.getJobExecution().getExecutionContext());
			passedInStepContext = new ExecutionContext(stepExecution.getExecutionContext());
			stepExecution.getExecutionContext().putString("stepKey", "stepValue");
			stepExecution.getJobExecution().getExecutionContext().putString("jobKey", "jobValue");
			jobRepository.update(stepExecution);
			jobRepository.updateExecutionContext(stepExecution);

			if (exception instanceof JobInterruptedException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(((JobInterruptedException) exception).getStatus());
				stepExecution.addFailureException(exception);
				throw (JobInterruptedException) exception;
			}
			if (exception instanceof RuntimeException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (exception instanceof Error) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (exception instanceof JobInterruptedException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (runnable != null) {
				runnable.run();
			}
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			stepExecution.setStatus(BatchStatus.COMPLETED);
			jobRepository.update(stepExecution);
			jobRepository.updateExecutionContext(stepExecution);

		}

	}

}
