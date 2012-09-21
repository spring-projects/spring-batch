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
package org.springframework.batch.core.configuration.xml;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.test.namespace.config.DummyNamespaceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;


/**
 * @author Dave Syer
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TaskletParserBeanPropertiesTests {
	
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

	@Autowired
	private MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean;
	
	@Before
	public void setUp() {
		mapJobRepositoryFactoryBean.clear();
	}

	@Test
	public void testTaskletRef() throws Exception {
		assertNotNull(job1);
		JobExecution jobExecution = jobRepository.createJobExecution(job1.getName(), new JobParameters());
		job1.execute(jobExecution);
		assertEquals("bar", tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testTaskletInline() throws Exception {
		assertNotNull(job2);
		JobExecution jobExecution = jobRepository.createJobExecution(job2.getName(), new JobParameters());
		job2.execute(jobExecution);
		Step step = job2.getStep("step2");
		tasklet = (TestTasklet) ReflectionTestUtils.getField(step, "tasklet");
		 assertEquals("foo", tasklet.getName());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testTasklet3() throws Exception {
		assertNotNull(job3);
		JobExecution jobExecution = jobRepository.createJobExecution(job3.getName(), new JobParameters());
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
	public void testCustomNestedTasklet() throws Exception {
		assertNotNull(job4);
		JobExecution jobExecution = jobRepository.createJobExecution(job4.getName(), new JobParameters());
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