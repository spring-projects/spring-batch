/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FlowStepParserTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Autowired
	@Qualifier("job4")
	private Job job4;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean;

	@Before
	public void setUp() {
		mapJobRepositoryFactoryBean.clear();
	}

	@Test
	public void testFlowStep() throws Exception {
		assertNotNull(job1);
		JobExecution jobExecution = jobRepository.createJobExecution(job1.getName(), new JobParameters());
		job1.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(5, stepNames.size());
		assertEquals("[s1, job1.flow, s2, s3, s4]", stepNames.toString());
	}

	@Test
	public void testFlowExternalStep() throws Exception {
		assertNotNull(job2);
		JobExecution jobExecution = jobRepository.createJobExecution(job2.getName(), new JobParameters());
		job2.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(5, stepNames.size());
		assertEquals("[job2.s1, job2.flow, s2, s3, job2.s4]", stepNames.toString());
	}

	@Test
	public void testRepeatedFlow() throws Exception {
		assertNotNull(job3);
		JobExecution jobExecution = jobRepository.createJobExecution(job3.getName(), new JobParameters());
		job3.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(6, stepNames.size());
		assertEquals("[job3.flow, s2, s3, job3.flow, s2, s3]", stepNames.toString());
	}

	@Test
	// TODO: BATCH-1745
	public void testRestartedFlow() throws Exception {
		assertNotNull(job4);
		JobExecution jobExecution = jobRepository.createJobExecution(job4.getName(), new JobParameters());
		job4.execute(jobExecution);
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(3, stepNames.size());
		assertEquals("[job4.flow, s2, s3]", stepNames.toString());
		jobExecution = jobRepository.createJobExecution(job4.getName(), new JobParameters());
		job4.execute(jobExecution);
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		stepNames = getStepNames(jobExecution);
		assertEquals(1, stepNames.size());
		// The flow executes again, but all the steps were already complete
		assertEquals("[job4.flow]", stepNames.toString());
	}

	private List<String> getStepNames(JobExecution jobExecution) {
		List<String> list = new ArrayList<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			list.add(stepExecution.getStepName());
		}
		return list;
	}

	public static class Decider implements JobExecutionDecider {

		int count = 0;

		@Override
		public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
			if (count++ < 2) {
				return new FlowExecutionStatus("OK");
			}
			return new FlowExecutionStatus("END");
		}

	}
}
