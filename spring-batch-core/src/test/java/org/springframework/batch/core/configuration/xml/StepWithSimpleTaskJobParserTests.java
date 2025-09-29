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
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Thomas Risberg
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
class StepWithSimpleTaskJobParserTests {

	@Autowired
	private Job job;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	@Qualifier("listener")
	private TestListener listener;

	@Test
	void testJob() throws Exception {
		assertNotNull(job);
		assertTrue(job instanceof FlowJob);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		TestTasklet t1 = assertTasklet(job, "step1", "t1");
		TestTasklet t2 = assertTasklet(job, "step2", "t2");
		TestTasklet t3 = assertTasklet(job, "step3", "t3");
		TestTasklet t4 = assertTasklet(job, "step4", "t4");

		job.execute(jobExecution);

		assertTrue(t1.isExecuted());
		assertTrue(t2.isExecuted());
		assertTrue(t3.isExecuted());
		assertTrue(t4.isExecuted());

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(4, jobExecution.getStepExecutions().size());
		assertTrue(listener.isExecuted());
	}

	private TestTasklet assertTasklet(Job job, String stepName, String taskletName) {
		Step step = ((FlowJob) job).getStep(stepName);
		assertTrue(step instanceof TaskletStep, "Wrong type for step name=" + stepName + ": " + step);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof TestTasklet);
		TestTasklet testTasklet = (TestTasklet) tasklet;
		assertEquals(taskletName, testTasklet.getName());
		assertFalse(testTasklet.isExecuted());
		return testTasklet;
	}

}
