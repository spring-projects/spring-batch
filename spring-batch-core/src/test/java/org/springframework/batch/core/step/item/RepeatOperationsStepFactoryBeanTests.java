/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.EmptyItemWriter;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author jojoldu
 *
 */
class RepeatOperationsStepFactoryBeanTests {

	private final SimpleStepFactoryBean<String, String> factory = new SimpleStepFactoryBean<>();

	private List<String> list;

	private final JobExecution jobExecution = new JobExecution(1L, new JobInstance(0L, "job"), new JobParameters());

	@BeforeEach
	void setUp() {
		factory.setBeanName("RepeatOperationsStep");
		factory.setItemReader(new ListItemReader<>(new ArrayList<>()));
		factory.setItemWriter(new EmptyItemWriter<>());
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
	}

	@Test
	void testType() {
		assertTrue(Step.class.isAssignableFrom(factory.getObjectType()));
	}

	@Test
	@SuppressWarnings("cast")
	void testDefaultValue() throws Exception {
		assertInstanceOf(Step.class, factory.getObject());
	}

	@Test
	void testStepOperationsWithoutChunkListener() throws Exception {

		factory.setItemReader(new ListItemReader<>(new ArrayList<>()));
		factory.setItemWriter(new EmptyItemWriter<>());
		JobRepositorySupport jobRepository = new JobRepositorySupport();
		factory.setJobRepository(jobRepository);
		factory.setTransactionManager(new ResourcelessTransactionManager());

		factory.setStepOperations(callback -> {
			list = new ArrayList<>();
			list.add("foo");
			return RepeatStatus.FINISHED;
		});

		Step step = factory.getObject();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(1, list.size());
	}

}
