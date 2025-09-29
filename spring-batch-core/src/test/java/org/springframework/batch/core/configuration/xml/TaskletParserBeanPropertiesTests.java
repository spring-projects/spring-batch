/*
 * Copyright 2006-2022 the original author or authors.
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

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.core.test.namespace.config.DummyNamespaceHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig
class TaskletParserBeanPropertiesTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private FlowJob job2;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Autowired
	@Qualifier("job4")
	private Job job4;

	@Autowired
	@Qualifier("tasklet")
	private TestTasklet tasklet;

	@Autowired
	private JobRepository jobRepository;

	@Test
	void testTaskletRef() throws Exception {
		assertNotNull(job1);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job1.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job1.execute(jobExecution);
		assertEquals("bar", tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	void testTaskletInline() throws Exception {
		assertNotNull(job2);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job2.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job2.execute(jobExecution);
		Step step = job2.getStep("step2");
		tasklet = (TestTasklet) ReflectionTestUtils.getField(step, "tasklet");
		assertEquals("foo", tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	void testTasklet3() throws Exception {
		assertNotNull(job3);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job3.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job3.execute(jobExecution);
		assertEquals(FlowJob.class, job3.getClass());
		Step step = ((FlowJob) job3).getStep("step3");
		Field field = ReflectionUtils.findField(TaskletStep.class, "tasklet");
		ReflectionUtils.makeAccessible(field);
		TestTasklet tasklet = (TestTasklet) ReflectionUtils.getField(field, step);
		assertEquals("foobar", tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	void testCustomNestedTasklet() throws Exception {
		assertNotNull(job4);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job4.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job4.execute(jobExecution);
		assertEquals(FlowJob.class, job4.getClass());
		Step step = ((FlowJob) job4).getStep("step4");
		Field field = ReflectionUtils.findField(TaskletStep.class, "tasklet");
		ReflectionUtils.makeAccessible(field);
		TestTasklet tasklet = (TestTasklet) ReflectionUtils.getField(field, step);
		assertEquals(DummyNamespaceHandler.LABEL, tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

}