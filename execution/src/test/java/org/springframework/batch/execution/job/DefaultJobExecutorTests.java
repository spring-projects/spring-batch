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

package org.springframework.batch.execution.job;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.execution.step.SimpleStepConfiguration;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Tests for DefaultJobLifecycle. MapJobDao and MapStepDao are used instead of a
 * mock repository to test that status is being stored correctly.
 * 
 * @author Lucas Ward
 */
public class DefaultJobExecutorTests extends TestCase {

	private JobRepository jobRepository;

	private JobDao jobDao;

	private StepDao stepDao;

	private List list = new ArrayList();

	StepExecutor defaultStepLifecycle = new StepExecutor() {
		public ExitStatus process(StepConfiguration configuration, StepExecution stepExecution)
				throws StepInterruptedException, BatchCriticalException {
			list.add("default");
			return ExitStatus.FINISHED;
		}
	};

	StepExecutor configurationStepLifecycle = new StepExecutor() {
		public ExitStatus process(StepConfiguration configuration, StepExecution stepExecution)
				throws StepInterruptedException, BatchCriticalException {
			list.add("special");
			return ExitStatus.FINISHED;
		}
	};

	private JobInstance job;

	private JobExecution jobExecution;

	private StepInstance step1;

	private StepInstance step2;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private StepConfigurationSupport stepConfiguration1;

	private StepConfigurationSupport stepConfiguration2;

	private JobConfiguration jobConfiguration;

	private SimpleJobIdentifier jobIdentifer;

	private DefaultJobExecutor jobExecutor;

	protected void setUp() throws Exception {
		super.setUp();

		MapJobDao.clear();
		MapStepDao.clear();
		jobDao = new MapJobDao();
		stepDao = new MapStepDao();
		jobRepository = new SimpleJobRepository(jobDao, stepDao);
		jobExecutor = new DefaultJobExecutor();
		jobExecutor.setJobRepository(jobRepository);

		jobExecutor.setStepExecutorFactory(new StepExecutorFactory() {
			public StepExecutor getExecutor(StepConfiguration configuration) {
				return defaultStepLifecycle;
			}
		});

		stepConfiguration1 = new SimpleStepConfiguration("TestStep1");
		stepConfiguration2 = new SimpleStepConfiguration("TestStep2");
		List stepConfigurations = new ArrayList();
		stepConfigurations.add(stepConfiguration1);
		stepConfigurations.add(stepConfiguration2);
		jobConfiguration = new JobConfiguration();
		jobConfiguration.setSteps(stepConfigurations);

		jobIdentifer = new SimpleJobIdentifier("TestJob");

		job = jobRepository.findOrCreateJob(jobConfiguration, jobIdentifer);

		jobExecution = new JobExecution(job);

		List steps = job.getSteps();
		step1 = (StepInstance) steps.get(0);
		step2 = (StepInstance) steps.get(1);
		stepExecution1 = new StepExecution(step1, jobExecution);
		stepExecution2 = new StepExecution(step2, jobExecution);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRunNormally() throws Exception {

		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		jobExecutor.run(jobConfiguration, jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED);
	}
	
	public void testRunWithDefaultStepExecutor() throws Exception {

		jobExecutor = new DefaultJobExecutor();
		jobExecutor.setJobRepository(jobRepository);
		// do not set StepExecutorFactory...
		stepConfiguration1.setStartLimit(5);
		stepConfiguration1.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				list.add("1");
				return ExitStatus.FINISHED;
			}
		});
		stepConfiguration2.setStartLimit(5);
		stepConfiguration2.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				list.add("2");
				return ExitStatus.FINISHED;
			}
		});
		jobExecutor.run(jobConfiguration, jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.FINISHED);
	}


	public void testExecutionContextIsSet() throws Exception {

		testRunNormally();
		assertEquals(job, jobExecution.getJob());
		assertEquals(step1, stepExecution1.getStep());
		assertEquals(step2, stepExecution2.getStep());
	}

	public void testRunWithNonDefaultExecutor() throws Exception {

		jobExecutor.setStepExecutorFactory(new StepExecutorFactory() {
			public StepExecutor getExecutor(StepConfiguration configuration) {
				return configuration == stepConfiguration2 ? defaultStepLifecycle : configurationStepLifecycle;
			}
		});
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);

		jobExecutor.run(jobConfiguration, jobExecution);

		assertEquals(2, list.size());
		assertEquals("special", list.get(0));
		assertEquals("default", list.get(1));
		checkRepository(BatchStatus.COMPLETED);
	}

	public void testInterrupted() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		final StepInterruptedException exception = new StepInterruptedException("Interrupt!");
		defaultStepLifecycle = new StepExecutor() {
			public ExitStatus process(StepConfiguration configuration, StepExecution stepExecution)
					throws StepInterruptedException, BatchCriticalException {
				throw exception;
			}
		};
		try {
			jobExecutor.run(jobConfiguration, jobExecution);
		}
		catch (BatchCriticalException e) {
			assertEquals(exception, e.getCause());
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, new ExitStatus(false, ExitCodeExceptionClassifier.STEP_INTERRUPTED));
	}

	public void testFailed() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		defaultStepLifecycle = new StepExecutor() {
			public ExitStatus process(StepConfiguration configuration, StepExecution stepExecution)
					throws StepInterruptedException, BatchCriticalException {
				throw exception;
			}
		};
		try {
			jobExecutor.run(jobConfiguration, jobExecution);
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, new ExitStatus(false, ExitCodeExceptionClassifier.FATAL_EXCEPTION));
	}

	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		stepConfiguration1.setStartLimit(0);

		try{
			jobExecutor.run(jobConfiguration, jobExecution);
			fail();
		}
		catch( Exception ex ){
			//expected
		}
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		assertEquals(job, jobDao.findJobs(jobIdentifer).get(0));
		// because map dao stores in memory, it can be checked directly
		assertEquals(status, job.getStatus());
		JobExecution jobExecution = (JobExecution) jobDao.findJobExecutions(job).get(0);
		assertEquals(job.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if(exitStatus != null){
			assertEquals(jobExecution.getExitStatus().getExitCode(), exitStatus.getExitCode()); 
		}
	}
	
	private void checkRepository(BatchStatus status){
		checkRepository(status, null);
	}
}
