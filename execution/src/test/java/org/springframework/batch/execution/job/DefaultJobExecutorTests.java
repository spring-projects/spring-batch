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
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobExecutionContext;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.StepExecutionContext;
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

	private SimpleJobIdentifier jobRuntimeInformation;

	private DefaultJobExecutor jobLifecycle;

	protected void setUp() throws Exception {
		super.setUp();

		MapJobDao.clear();
		MapStepDao.clear();
		jobDao = new MapJobDao();
		stepDao = new MapStepDao();
		jobRepository = new SimpleJobRepository(jobDao, stepDao);
		jobLifecycle = new DefaultJobExecutor();
		jobLifecycle.setJobRepository(jobRepository);

		jobLifecycle.setStepExecutorResolver(new StepExecutorFactory() {
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

		jobRuntimeInformation = new SimpleJobIdentifier("TestJob");

		job = jobRepository.findOrCreateJob(jobConfiguration, jobRuntimeInformation);

		jobExecutionContext = new JobExecutionContext(jobRuntimeInformation, job);

		List steps = job.getSteps();
		step1 = (StepInstance) steps.get(0);
		step2 = (StepInstance) steps.get(1);
		stepExecutionContext1 = new StepExecutionContext(jobExecutionContext, step1);
		stepExecutionContext2 = new StepExecutionContext(jobExecutionContext, step2);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRunWithDefaultLifecycle() throws Exception {

		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		jobLifecycle.run(jobConfiguration, jobExecutionContext);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED);
	}

	public void testExecutionContextIsSet() throws Exception {

		testRunWithDefaultLifecycle();
		assertEquals(job, jobExecutionContext.getJob());
		assertEquals(step1, stepExecutionContext1.getStep());
		assertEquals(step2, stepExecutionContext2.getStep());
	}

	public void testRunWithNonDefaultExecutor() throws Exception {

		jobLifecycle.setStepExecutorResolver(new StepExecutorFactory() {
			public StepExecutor getExecutor(StepConfiguration configuration) {
				return configuration == stepConfiguration2 ? defaultStepLifecycle : configurationStepLifecycle;
			}
		});
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);

		jobLifecycle.run(jobConfiguration, jobExecutionContext);

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
			jobLifecycle.run(jobConfiguration, jobExecutionContext);
		}
		catch (BatchCriticalException e) {
			assertEquals(exception, e.getCause());
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED);
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
			jobLifecycle.run(jobConfiguration, jobExecutionContext);
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED);
	}

	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		stepConfiguration1.setStartLimit(0);

		try{
			jobLifecycle.run(jobConfiguration, jobExecutionContext);
			fail();
		}
		catch( Exception ex ){
			//expected
		}
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status) {
		assertEquals(job, jobDao.findJobs(jobRuntimeInformation).get(0));
		// because map dao stores in memory, it can be checked directly
		assertEquals(status, job.getStatus());
		JobExecution jobExecution = (JobExecution) jobDao.findJobExecutions(job).get(0);
		assertEquals(job.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
	}
}
