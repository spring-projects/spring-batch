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
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobExecutionContext;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.StepExecutionContext;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.execution.step.simple.AbstractStepConfiguration;
import org.springframework.batch.execution.step.simple.SimpleStepConfiguration;
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
		public ExitStatus process(StepConfiguration configuration, StepExecutionContext stepExecutionContext)
				throws StepInterruptedException, BatchCriticalException {
			list.add("default");
			return ExitStatus.FINISHED;
		}
	};

	StepExecutor configurationStepLifecycle = new StepExecutor() {
		public ExitStatus process(StepConfiguration configuration, StepExecutionContext stepExecutionContext)
				throws StepInterruptedException, BatchCriticalException {
			list.add("special");
			return ExitStatus.FINISHED;
		}
	};

	private JobInstance job;

	private JobExecutionContext jobExecutionContext;

	private StepInstance step1;

	private StepInstance step2;

	private StepExecutionContext stepExecutionContext1;

	private StepExecutionContext stepExecutionContext2;

	private AbstractStepConfiguration stepConfiguration1;

	private AbstractStepConfiguration stepConfiguration2;

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

		stepConfiguration1 = new SimpleStepConfiguration();
		stepConfiguration1.setName("TestStep1");
		stepConfiguration2 = new SimpleStepConfiguration();
		stepConfiguration2.setName("TestStep2");
		List stepConfigurations = new ArrayList();
		stepConfigurations.add(stepConfiguration1);
		stepConfigurations.add(stepConfiguration2);
		jobConfiguration = new JobConfiguration();
		jobConfiguration.setSteps(stepConfigurations);

		jobIdentifer = new SimpleJobIdentifier("TestJob");

		job = jobRepository.findOrCreateJob(jobConfiguration, jobIdentifer);

		jobExecutionContext = new JobExecutionContext(jobIdentifer, job);

		List steps = job.getSteps();
		step1 = (StepInstance) steps.get(0);
		step2 = (StepInstance) steps.get(1);
		stepExecutionContext1 = new StepExecutionContext(jobExecutionContext, step1);
		stepExecutionContext2 = new StepExecutionContext(jobExecutionContext, step2);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRunNormally() throws Exception {

		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		jobExecutor.run(jobConfiguration, jobExecutionContext);
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
		jobExecutor.run(jobConfiguration, jobExecutionContext);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.FINISHED.getExitCode());
	}


	public void testExecutionContextIsSet() throws Exception {

		testRunNormally();
		assertEquals(job, jobExecutionContext.getJob());
		assertEquals(step1, stepExecutionContext1.getStep());
		assertEquals(step2, stepExecutionContext2.getStep());
	}

	public void testRunWithNonDefaultExecutor() throws Exception {

		jobExecutor.setStepExecutorFactory(new StepExecutorFactory() {
			public StepExecutor getExecutor(StepConfiguration configuration) {
				return configuration == stepConfiguration2 ? defaultStepLifecycle : configurationStepLifecycle;
			}
		});
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);

		jobExecutor.run(jobConfiguration, jobExecutionContext);

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
			public ExitStatus process(StepConfiguration configuration, StepExecutionContext stepExecutionContext)
					throws StepInterruptedException, BatchCriticalException {
				throw exception;
			}
		};
		try {
			jobExecutor.run(jobConfiguration, jobExecutionContext);
		}
		catch (BatchCriticalException e) {
			assertEquals(exception, e.getCause());
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitCodeExceptionClassifier.STEP_INTERRUPTED);
	}

	public void testFailed() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		defaultStepLifecycle = new StepExecutor() {
			public ExitStatus process(StepConfiguration configuration, StepExecutionContext stepExecutionContext)
					throws StepInterruptedException, BatchCriticalException {
				throw exception;
			}
		};
		try {
			jobExecutor.run(jobConfiguration, jobExecutionContext);
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, ExitCodeExceptionClassifier.FATAL_EXCEPTION);
	}

	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		stepConfiguration1.setStartLimit(0);

		try{
			jobExecutor.run(jobConfiguration, jobExecutionContext);
			fail();
		}
		catch( Exception ex ){
			//expected
		}
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, String exitCode) {
		assertEquals(job, jobDao.findJobs(jobIdentifer).get(0));
		// because map dao stores in memory, it can be checked directly
		assertEquals(status, job.getStatus());
		JobExecution jobExecution = (JobExecution) jobDao.findJobExecutions(job).get(0);
		assertEquals(job.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if(exitCode != null){
			assertEquals(jobExecution.getExitCode(), exitCode); 
		}
	}
	
	private void checkRepository(BatchStatus status){
		checkRepository(status, null);
	}
}
