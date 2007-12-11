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

package org.springframework.batch.execution.step.simple;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
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
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

public class StepExecutorInterruptionTests extends TestCase {

	private JobRepository jobRepository;

	private JobDao jobDao = new MapJobDao();

	private StepDao stepDao = new MapStepDao();

	private JobInstance job;

	private StepConfigurationSupport stepConfiguration;

	private SimpleStepExecutor executor;

	public void setUp() throws Exception {

		jobRepository = new SimpleJobRepository(jobDao, stepDao);

		JobConfiguration jobConfiguration = new JobConfiguration();
		stepConfiguration = new SimpleStepConfiguration();
		jobConfiguration.addStep(stepConfiguration);
		JobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		jobConfiguration.setBeanName("testJob");
		job = jobRepository.findOrCreateJob(jobConfiguration, runtimeInformation).getJob();
		executor = new SimpleStepExecutor();
	}

	public void testInterruptChunk() throws Exception {

		executor.setRepository(jobRepository);

		List steps = job.getSteps();
		final StepInstance step = (StepInstance) steps.get(0);
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(null, new Long(0)));
		final StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
		stepConfiguration.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				// do something non-trivial (and not Thread.sleep())
				double foo = 1;
				for (int i = 2; i < 250; i++) {
					foo = foo * i;
				}
				// always return true, so processing always continues
				return new ExitStatus(foo != 1);
			}
		});

		Thread processingThread = new Thread() {
			public void run() {
				try {
					executor.process(stepConfiguration, stepExecution);
				}
				catch (StepInterruptedException e) {
					// do nothing...
				}
			}
		};

		processingThread.start();

		Thread.sleep(500);

		processingThread.interrupt();

		int count = 0;
		while (processingThread.isAlive() && count < 1000) {
			Thread.sleep(20);
			count++;
		}

		assertFalse(processingThread.isAlive());
		assertEquals(BatchStatus.STOPPED, step.getStatus());
	}

	public void testInterruptStep() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		// N.B, If we don't set the completion policy it might run forever
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		executor.setChunkOperations(template);
		testInterruptChunk();
	}

}
