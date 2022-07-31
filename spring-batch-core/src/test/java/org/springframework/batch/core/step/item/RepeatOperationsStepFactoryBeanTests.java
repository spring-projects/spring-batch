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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.EmptyItemWriter;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 *
 */
class RepeatOperationsStepFactoryBeanTests {

	private final SimpleStepFactoryBean<String, String> factory = new SimpleStepFactoryBean<>();

	private List<String> list;

	private final JobExecution jobExecution = new JobExecution(new JobInstance(0L, "job"), new JobParameters());

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
		assertTrue(factory.getObject() instanceof Step);
	}

	@Test
	void testStepOperationsWithoutChunkListener() throws Exception {

		factory.setItemReader(new ListItemReader<>(new ArrayList<>()));
		factory.setItemWriter(new EmptyItemWriter<>());
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());

		factory.setStepOperations(new RepeatOperations() {

			@Override
			public RepeatStatus iterate(RepeatCallback callback) {
				list = new ArrayList<>();
				list.add("foo");
				return RepeatStatus.FINISHED;
			}
		});

		Step step = factory.getObject();
		step.execute(new StepExecution(step.getName(), jobExecution));

		assertEquals(1, list.size());
	}

}
