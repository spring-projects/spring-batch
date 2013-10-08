/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.partition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Collection;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.BatchStatus;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.partition.JsrStepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepSupport;

public class JsrPartitionHandlerTests {

	private JsrPartitionHandler handler;
	private JobRepository repository = new JobRepositorySupport();
	private StepExecution stepExecution = new StepExecution("step", new JobExecution(1L));
	private int count;
	private BatchPropertyContext propertyContext;
	private JsrStepExecutionSplitter stepSplitter;

	@Before
	public void setUp() throws Exception {
		stepSplitter = new JsrStepExecutionSplitter("step1", repository);
		Analyzer.collectorData = "";
		Analyzer.status = "";
		count = 0;
		handler = new JsrPartitionHandler();
		handler.setStep(new StepSupport() {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				count++;
				stepExecution.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);
				stepExecution.setExitStatus(new ExitStatus("done"));
			}
		});
		propertyContext = new BatchPropertyContext();
		handler.setPropertyContext(propertyContext);
		repository = new MapJobRepositoryFactoryBean().getJobRepository();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		handler = new JsrPartitionHandler();

		try {
			handler.afterPropertiesSet();
			fail("PropertyContext was not checked for");
		} catch(IllegalArgumentException iae) {
			assertEquals("A BatchPropertyContext is required", iae.getMessage());
		}

		handler.setPropertyContext(new BatchPropertyContext());

		try {
			handler.afterPropertiesSet();
			fail("Threads or mapper was not checked for");
		} catch(IllegalArgumentException iae) {
			assertEquals("Either a mapper implementation or the number of partitions/threads is required", iae.getMessage());
		}

		handler.setThreads(3);
		handler.afterPropertiesSet();
	}

	@Test
	public void testHardcodedNumberOfPartitions() throws Exception {
		handler.setThreads(3);
		handler.setPartitions(3);
		handler.afterPropertiesSet();

		Collection<StepExecution> executions = handler.handle(stepSplitter, stepExecution);

		assertEquals(3, executions.size());
		assertEquals(3, count);
	}

	@Test
	public void testMapperProvidesPartitions() throws Exception {
		handler.setPartitionMapper(new PartitionMapper() {

			@Override
			public PartitionPlan mapPartitions() throws Exception {
				PartitionPlan plan = new PartitionPlanImpl();
				plan.setPartitions(3);
				plan.setThreads(0);
				return plan;
			}
		});

		handler.afterPropertiesSet();

		Collection<StepExecution> executions = handler.handle(new JsrStepExecutionSplitter("step1", repository), stepExecution);

		assertEquals(3, executions.size());
		assertEquals(3, count);
	}

	@Test
	public void testMapperProvidesPartitionsAndThreads() throws Exception {
		handler.setPartitionMapper(new PartitionMapper() {

			@Override
			public PartitionPlan mapPartitions() throws Exception {
				PartitionPlan plan = new PartitionPlanImpl();
				plan.setPartitions(3);
				plan.setThreads(1);
				return plan;
			}
		});

		handler.afterPropertiesSet();

		Collection<StepExecution> executions = handler.handle(new JsrStepExecutionSplitter("step1", repository), stepExecution);

		assertEquals(3, executions.size());
		assertEquals(3, count);
	}

	@Test
	public void testMapperWithProperties() throws Exception {
		handler.setPartitionMapper(new PartitionMapper() {

			@Override
			public PartitionPlan mapPartitions() throws Exception {
				PartitionPlan plan = new PartitionPlanImpl();
				Properties [] props = new Properties[2];
				props[0] = new Properties();
				props[0].put("key1", "value1");
				props[1] = new Properties();
				props[1].put("key1", "value2");
				plan.setPartitionProperties(props);
				plan.setPartitions(3);
				plan.setThreads(1);
				return plan;
			}
		});

		handler.afterPropertiesSet();

		Collection<StepExecution> executions = handler.handle(new JsrStepExecutionSplitter("step1", repository), stepExecution);

		assertEquals(3, executions.size());
		assertEquals(3, count);
		assertEquals("value1", propertyContext.getStepProperties("step1:partition0").get("key1"));
		assertEquals("value2", propertyContext.getStepProperties("step1:partition1").get("key1"));
	}

	@Test
	public void testAnalyzer() throws Exception {
		Queue<Serializable> queue = new ConcurrentLinkedQueue<Serializable>();
		queue.add("foo");
		queue.add("bar");

		handler.setPartitionDataQueue(queue);
		handler.setThreads(2);
		handler.setPartitions(2);
		handler.setPartitionAnalyzer(new Analyzer());
		handler.afterPropertiesSet();

		Collection<StepExecution> executions = handler.handle(new JsrStepExecutionSplitter("step1", repository), stepExecution);

		assertEquals(2, executions.size());
		assertEquals(2, count);
		assertEquals("foobar", Analyzer.collectorData);
		assertEquals("COMPLETEDdone", Analyzer.status);
	}

	public static class Analyzer implements PartitionAnalyzer {

		public static String collectorData;
		public static String status;

		@Override
		public void analyzeCollectorData(Serializable data) throws Exception {
			collectorData = collectorData + data;
		}

		@Override
		public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
				throws Exception {
			status = batchStatus + exitStatus;
		}
	}
}
