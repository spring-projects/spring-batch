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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.PartitionStep;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.PassThroughItemProcessor;
import org.springframework.batch.infrastructure.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.retry.RetryListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
class StepParserStepFactoryBeanTests {

	@Test
	void testNothingSet() {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		assertThrows(IllegalArgumentException.class, fb::getObject);
	}

	@Test
	void testOnlyTaskletSet() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setName("step");
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setJobRepository(new JobRepositorySupport());
		fb.setTasklet(new DummyTasklet());
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof DummyTasklet);
	}

	@Test
	void testOnlyTaskletTaskExecutor() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setName("step");
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setJobRepository(new JobRepositorySupport());
		fb.setTasklet(new DummyTasklet());
		fb.setTaskExecutor(new SimpleAsyncTaskExecutor());
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object stepOperations = ReflectionTestUtils.getField(step, "stepOperations");
		assertTrue(stepOperations instanceof TaskExecutorRepeatTemplate);
	}

	@Test
	void testSkipLimitSet() {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setName("step");
		fb.setSkipLimit(5);
		assertThrows(IllegalArgumentException.class, fb::getObject);
	}

	@Test
	void testTaskletStepAll() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTasklet(new DummyTasklet());
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepExecutionListener[] { new StepExecutionListener() {
		} });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof DummyTasklet);
	}

	@Test
	void testTaskletStepMissingIsolation() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setJobRepository(new JobRepositorySupport());
		fb.setTasklet(new DummyTasklet());
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setPropagation(Propagation.REQUIRED);
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof DummyTasklet);
	}

	@Test
	void testSimpleStepAll() {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setCommitInterval(5);
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemWriter(new DummyItemWriter());
		fb.setHasChunkElement(true);

		assertThrows(IllegalStateException.class, fb::getObject);
	}

	@Test
	void testFaultTolerantStepAll() {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setCommitInterval(5);
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemWriter(new DummyItemWriter());
		fb.setCacheCapacity(5);
		fb.setIsReaderTransactionalQueue(true);
		fb.setRetryLimit(5);
		fb.setSkipLimit(100);
		fb.setRetryListeners(new RetryListener() {
		});
		fb.setSkippableExceptionClasses(new HashMap<>());
		fb.setRetryableExceptionClasses(new HashMap<>());
		fb.setHasChunkElement(true);

		assertThrows(IllegalArgumentException.class, fb::getObject);
	}

	@Test
	void testSimpleStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setHasChunkElement(true);
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemProcessor(new PassThroughItemProcessor<>());
		fb.setItemWriter(new DummyItemWriter());

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
	}

	@Test
	void testFaultTolerantStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setHasChunkElement(true);
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemProcessor(new PassThroughItemProcessor<>());
		fb.setItemWriter(new DummyItemWriter());
		fb.setCacheCapacity(5);
		fb.setIsReaderTransactionalQueue(true);
		fb.setRetryLimit(5);
		fb.setSkipLimit(100);
		fb.setRetryListeners(new RetryListener() {
		});
		@SuppressWarnings("unchecked")
		Map<Class<? extends Throwable>, Boolean> exceptionMap = getExceptionMap(Exception.class);
		fb.setSkippableExceptionClasses(exceptionMap);
		fb.setRetryableExceptionClasses(exceptionMap);

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object throttleLimit = ReflectionTestUtils.getField(ReflectionTestUtils.getField(step, "stepOperations"),
				"throttleLimit");
		assertEquals(4, throttleLimit);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
		assertFalse((Boolean) ReflectionTestUtils.getField(tasklet, "buffering"));
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object repeatOperations = ReflectionTestUtils.getField(chunkProvider, "repeatOperations");
		Object completionPolicy = ReflectionTestUtils.getField(repeatOperations, "completionPolicy");
		assertTrue(completionPolicy instanceof DummyCompletionPolicy);
	}

	@Test
	void testPartitionStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setTaskExecutor(new SyncTaskExecutor());

		SimplePartitioner partitioner = new SimplePartitioner();
		fb.setPartitioner(partitioner);
		fb.setStep(new StepSupport("foo"));

		Object step = fb.getObject();
		assertTrue(step instanceof PartitionStep);
		Object handler = ReflectionTestUtils.getField(step, "partitionHandler");
		assertTrue(handler instanceof TaskExecutorPartitionHandler);
	}

	@Test
	void testPartitionStepWithProxyHandler() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setTaskExecutor(new SyncTaskExecutor());

		SimplePartitioner partitioner = new SimplePartitioner();
		fb.setPartitioner(partitioner);
		TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
		partitionHandler.setStep(new StepSupport("foo"));
		ProxyFactory factory = new ProxyFactory(partitionHandler);
		fb.setPartitionHandler((PartitionHandler) factory.getProxy());

		Object step = fb.getObject();
		assertTrue(step instanceof PartitionStep);
		Object handler = ReflectionTestUtils.getField(step, "partitionHandler");
		assertTrue(handler instanceof Advised);
	}

	@Test
	void testFlowStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListener() {
		} });
		fb.setTaskExecutor(new SyncTaskExecutor());

		fb.setFlow(new SimpleFlow("foo"));

		Object step = fb.getObject();
		assertTrue(step instanceof FlowStep);
		Object handler = ReflectionTestUtils.getField(step, "flow");
		assertTrue(handler instanceof SimpleFlow);
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}

}
