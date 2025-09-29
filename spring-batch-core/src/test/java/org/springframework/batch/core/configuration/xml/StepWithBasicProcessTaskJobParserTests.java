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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Thomas Risberg
 *
 */
@SpringJUnitConfig
class StepWithBasicProcessTaskJobParserTests {

	@Autowired
	private Job job;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TestReader reader;

	@Autowired
	@Qualifier("listener")
	private TestListener listener;

	@Autowired
	private TestProcessor processor;

	@Autowired
	private TestWriter writer;

	@Autowired
	private StepParserStepFactoryBean<?, ?> factory;

	@SuppressWarnings("unchecked")
	@Test
	void testStepWithTask() throws Exception {
		assertNotNull(job);
		Object ci = ReflectionTestUtils.getField(factory, "commitInterval");
		assertEquals(10, ci, "wrong chunk-size:");
		Object listeners = ReflectionTestUtils.getField(factory, "stepExecutionListeners");
		assertEquals(2, ((Set<StepExecutionListener>) listeners).size(), "wrong number of listeners:");
		Object streams = ReflectionTestUtils.getField(factory, "streams");
		assertEquals(1, ((ItemStream[]) streams).length, "wrong number of streams:");
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertTrue(reader.isExecuted());
		assertTrue(reader.isOpened());
		assertTrue(processor.isExecuted());
		assertTrue(writer.isExecuted());
		assertTrue(listener.isExecuted());
	}

}
