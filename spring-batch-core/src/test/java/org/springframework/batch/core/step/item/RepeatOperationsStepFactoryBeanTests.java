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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.EmptyItemWriter;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public class RepeatOperationsStepFactoryBeanTests extends TestCase {

	private SimpleStepFactoryBean<String,String> factory = new SimpleStepFactoryBean<String,String>();

	private List<String> list;

	private JobExecution jobExecution = new JobExecution(new JobInstance(0L, new JobParameters(), "job"));

	protected void setUp() throws Exception {
		factory.setBeanName("RepeatOperationsStep");
		factory.setItemReader(new ListItemReader<String>(new ArrayList<String>()));
		factory.setItemWriter(new EmptyItemWriter<String>());
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
	}

	public void testType() throws Exception {
		assertTrue(Step.class.isAssignableFrom(factory.getObjectType()));
	}

	public void testDefaultValue() throws Exception {
		assertTrue(factory.getObject() instanceof Step);
	}

	public void testStepOperationsWithoutChunkListener() throws Exception {

		factory.setItemReader(new ListItemReader<String>(new ArrayList<String>()));
		factory.setItemWriter(new EmptyItemWriter<String>());
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());

		factory.setStepOperations(new RepeatOperations() {

			public RepeatStatus iterate(RepeatCallback callback) {
				list = new ArrayList<String>();
				list.add("foo");
				return RepeatStatus.FINISHED;
			}
		});

		Step step = (Step) factory.getObject();
		step.execute(new StepExecution(step.getName(), jobExecution));

		assertEquals(1, list.size());
	}

}
