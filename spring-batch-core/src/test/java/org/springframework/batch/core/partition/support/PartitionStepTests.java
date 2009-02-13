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
package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public class PartitionStepTests {

	private PartitionStep step = new PartitionStep();

	private Step remote = new StepSupport("remote");

	private JobRepository jobRepository;

	@Before
	public void setUp() throws Exception {
		MapJobRepositoryFactoryBean.clear();
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		factory.setTransactionManager(new ResourcelessTransactionManager());
		jobRepository = (JobRepository) factory.getObject();
		step.setJobRepository(jobRepository);
	}

	@Test
	public void testVanillaStepExecution() throws Exception {
		step.setStepExecutionSplitter(new SimpleStepExecutionSplitter(jobRepository, remote));
		step.setPartitionHandler(new PartitionHandler() {
			public Collection<StepExecution> handle(StepExecutionSplitter stepSplitter, StepExecution stepExecution)
					throws Exception {
				Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
				for (StepExecution execution : executions) {
					execution.setStatus(BatchStatus.COMPLETED);
					execution.setExitStatus(ExitStatus.COMPLETED);
				}
				return executions;
			}
		});
		step.afterPropertiesSet();
		JobExecution jobExecution = jobRepository.createJobExecution("vanillaJob", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("foo");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		// one master and two workers
		assertEquals(3, stepExecution.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	@Test
	public void testFailedStepExecution() throws Exception {
		step.setStepExecutionSplitter(new SimpleStepExecutionSplitter(jobRepository, remote));
		step.setPartitionHandler(new PartitionHandler() {
			public Collection<StepExecution> handle(StepExecutionSplitter stepSplitter, StepExecution stepExecution)
					throws Exception {
				Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
				for (StepExecution execution : executions) {
					execution.setStatus(BatchStatus.INCOMPLETE);
					execution.setExitStatus(ExitStatus.FAILED);
				}
				return executions;
			}
		});
		step.afterPropertiesSet();
		JobExecution jobExecution = jobRepository.createJobExecution("vanillaJob", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("foo");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		// one master and two workers
		assertEquals(3, stepExecution.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.INCOMPLETE, stepExecution.getStatus());
	}

}
