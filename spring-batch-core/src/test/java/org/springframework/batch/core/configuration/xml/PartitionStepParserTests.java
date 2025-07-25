/*
 * Copyright 2006-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.PartitionStep;
import org.springframework.batch.core.partition.StepExecutionAggregator;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Josh Long
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
public class PartitionStepParserTests implements ApplicationContextAware {

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
	@Qualifier("job5")
	private Job job5;

	@Autowired
	@Qualifier("nameStoringTasklet")
	private NameStoringTasklet nameStoringTasklet;

	@Autowired
	private JobRepository jobRepository;

	private ApplicationContext applicationContext;

	private final List<String> savedStepNames = new ArrayList<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		nameStoringTasklet.setStepNamesList(savedStepNames);
	}

	@SuppressWarnings("unchecked")
	private <T> T accessPrivateField(Object o, String fieldName) throws ReflectiveOperationException {
		Field field = o.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(o);
	}

	@Test
	void testDefaultHandlerStep() throws Exception {
		assertNotNull(job1);
		JobExecution jobExecution = jobRepository.createJobExecution(job1.getName(), new JobParameters());
		job1.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Collections.sort(savedStepNames);
		assertEquals("[step1:partition0, step1:partition1]", savedStepNames.toString());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(3, stepNames.size());
		assertEquals("[s1, step1:partition0, step1:partition1]", stepNames.toString());
		assertEquals("bar", jobExecution.getExecutionContext().get("foo"));
	}

	@Test
	void testHandlerRefStep() throws Exception {
		assertNotNull(job2);
		JobExecution jobExecution = jobRepository.createJobExecution(job2.getName(), new JobParameters());
		job2.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Collections.sort(savedStepNames);
		assertEquals("[s2:partition0, s2:partition1, s2:partition2, s3]", savedStepNames.toString());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(5, stepNames.size());
		assertEquals("[s2, s2:partition0, s2:partition1, s2:partition2, s3]", stepNames.toString());
	}

	/**
	 * BATCH-1509 we now support the ability define steps inline for partitioned steps.
	 * this demonstrates that the execution proceeds as expected and that the partition
	 * handler has a reference to the inline step definition
	 */
	@Test
	void testNestedPartitionStepStepReference() throws Throwable {
		assertNotNull(job3, "the reference to the job3 configured in the XML file must not be null");
		JobExecution jobExecution = jobRepository.createJobExecution(job3.getName(), new JobParameters());

		job3.execute(jobExecution);

		for (StepExecution se : jobExecution.getStepExecutions()) {
			String stepExecutionName = se.getStepName();
			// the partitioned step
			if (stepExecutionName.equalsIgnoreCase("j3s1")) {
				PartitionStep partitionStep = this.applicationContext.getBean(stepExecutionName, PartitionStep.class);
				// prove that the reference in the {@link
				// TaskExecutorPartitionHandler} is the step configured inline
				TaskExecutorPartitionHandler taskExecutorPartitionHandler = accessPrivateField(partitionStep,
						"partitionHandler");
				TaskletStep taskletStep = accessPrivateField(taskExecutorPartitionHandler, "step");

				assertNotNull(taskletStep, "the taskletStep wasn't configured with a step. "
						+ "We're trusting that the factory ensured " + "a reference was given.");
			}
		}
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Collections.sort(savedStepNames);
		assertEquals(
				"[j3s1:partition0, j3s1:partition1, j3s1:partition2, j3s1:partition3, j3s1:partition4, j3s1:partition5]",
				savedStepNames.toString());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(7, stepNames.size());
		assertEquals(
				"[j3s1, j3s1:partition0, j3s1:partition1, j3s1:partition2, j3s1:partition3, j3s1:partition4, j3s1:partition5]",
				stepNames.toString());
	}

	/**
	 * BATCH-1509 we now support the ability define steps inline for partitioned steps.
	 * this demonstrates that the execution proceeds as expected and that the partition
	 * handler has a reference to the inline step definition
	 */
	@Test
	void testNestedPartitionStep() throws Throwable {
		assertNotNull(job4, "the reference to the job4 configured in the XML file must not be null");
		JobExecution jobExecution = jobRepository.createJobExecution(job4.getName(), new JobParameters());

		job4.execute(jobExecution);

		for (StepExecution se : jobExecution.getStepExecutions()) {
			String stepExecutionName = se.getStepName();
			if (stepExecutionName.equalsIgnoreCase("j4s1")) { // the partitioned
				// step
				PartitionStep partitionStep = this.applicationContext.getBean(stepExecutionName, PartitionStep.class);

				// prove that the reference in the {@link
				// TaskExecutorPartitionHandler} is the step configured inline
				TaskExecutorPartitionHandler taskExecutorPartitionHandler = accessPrivateField(partitionStep,
						"partitionHandler");
				TaskletStep taskletStep = accessPrivateField(taskExecutorPartitionHandler, "step");

				assertNotNull(taskletStep, "the taskletStep wasn't configured with a step. "
						+ "We're trusting that the factory ensured " + "a reference was given.");
			}
		}
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		// Step names not saved by this one (it doesn't have that tasklet)
		assertEquals("[]", savedStepNames.toString());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(7, stepNames.size());
		assertEquals(
				"[j4s1, j4s1:partition0, j4s1:partition1, j4s1:partition2, j4s1:partition3, j4s1:partition4, j4s1:partition5]",
				stepNames.toString());
	}

	@Test
	void testCustomHandlerRefStep() throws Exception {
		assertNotNull(job5);
		JobExecution jobExecution = jobRepository.createJobExecution(job5.getName(), new JobParameters());
		job5.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(1, stepNames.size());
		assertEquals("[j5s1]", stepNames.toString());
	}

	private List<String> getStepNames(JobExecution jobExecution) {
		List<String> list = new ArrayList<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			list.add(stepExecution.getStepName());
		}
		Collections.sort(list);
		return list;
	}

	public static class CustomPartitionHandler implements PartitionHandler {

		@Override
		public Collection<StepExecution> handle(StepExecutionSplitter stepSplitter, StepExecution stepExecution)
				throws Exception {
			return Arrays.asList(stepExecution);
		}

	}

	public static class CustomStepExecutionAggregator implements StepExecutionAggregator {

		@Override
		public void aggregate(StepExecution result, Collection<StepExecution> executions) {
			result.getJobExecution().getExecutionContext().put("foo", "bar");
		}

	}

}
