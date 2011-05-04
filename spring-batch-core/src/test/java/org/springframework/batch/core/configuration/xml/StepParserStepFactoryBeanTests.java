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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.retry.listener.RetryListenerSupport;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class StepParserStepFactoryBeanTests {

	@Test(expected = IllegalStateException.class)
	public void testNothingSet() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.getObject();
	}

	@Test
	public void testOnlyTaskletSet() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setTasklet(new DummyTasklet());
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof DummyTasklet);
	}

	@Test
	public void testOnlyTaskletTaskExecutor() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setTasklet(new DummyTasklet());
		fb.setTaskExecutor(new SimpleAsyncTaskExecutor());
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object stepOperations = ReflectionTestUtils.getField(step, "stepOperations");
		assertTrue(stepOperations instanceof TaskExecutorRepeatTemplate);
	}

	@Test(expected = IllegalStateException.class)
	public void testSkipLimitSet() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setSkipLimit(5);
		fb.getObject();
	}

	@Test
	public void testTaskletStepAll() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTasklet(new DummyTasklet());
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepExecutionListenerSupport[] { new StepExecutionListenerSupport() });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof DummyTasklet);
	}

	@Test
	public void testTaskletStepMissingIsolation() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
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

	@Test(expected = IllegalStateException.class)
	public void testSimpleStepAll() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setCommitInterval(5);
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemWriter(new DummyItemWriter());
		fb.setStreams(new ItemStream[] { new FlatFileItemReader<Object>() });

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
	}

	@Test(expected = IllegalStateException.class)
	public void testFaultTolerantStepAll() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setCommitInterval(5);
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemWriter(new DummyItemWriter());
		fb.setStreams(new ItemStream[] { new FlatFileItemReader<Object>() });
		fb.setCacheCapacity(5);
		fb.setIsReaderTransactionalQueue(true);
		fb.setRetryLimit(5);
		fb.setSkipLimit(100);
		fb.setRetryListeners(new RetryListenerSupport());
		fb.setSkippableExceptionClasses(new HashMap<Class<? extends Throwable>, Boolean>());
		fb.setRetryableExceptionClasses(new HashMap<Class<? extends Throwable>, Boolean>());

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
	}

	@Test
	public void testSimpleStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setHasChunkElement(true);
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
		fb.setIsolation(Isolation.DEFAULT);
		fb.setTransactionTimeout(-1);
		fb.setPropagation(Propagation.REQUIRED);
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemProcessor(new PassThroughItemProcessor<Object>());
		fb.setItemWriter(new DummyItemWriter());
		fb.setStreams(new ItemStream[] { new FlatFileItemReader<Object>() });

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
	}

	@Test
	public void testFaultTolerantStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setHasChunkElement(true);
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setTransactionManager(new ResourcelessTransactionManager());
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
		fb.setChunkCompletionPolicy(new DummyCompletionPolicy());
		fb.setTaskExecutor(new SyncTaskExecutor());
		fb.setItemReader(new DummyItemReader());
		fb.setItemProcessor(new PassThroughItemProcessor<Object>());
		fb.setItemWriter(new DummyItemWriter());
		fb.setStreams(new ItemStream[] { new FlatFileItemReader<Object>() });
		fb.setCacheCapacity(5);
		fb.setIsReaderTransactionalQueue(true);
		fb.setRetryLimit(5);
		fb.setSkipLimit(100);
		fb.setThrottleLimit(10);
		fb.setRetryListeners(new RetryListenerSupport());
		@SuppressWarnings("unchecked")
		Map<Class<? extends Throwable>, Boolean> exceptionMap = getExceptionMap(Exception.class);
		fb.setSkippableExceptionClasses(exceptionMap);
		fb.setRetryableExceptionClasses(exceptionMap);

		Object step = fb.getObject();
		assertTrue(step instanceof TaskletStep);
		Object throttleLimit = ReflectionTestUtils.getField(ReflectionTestUtils.getField(step, "stepOperations"), "throttleLimit");
		assertEquals(new Integer(10), throttleLimit);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof ChunkOrientedTasklet<?>);
	}

	@Test
	public void testPartitionStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
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
	public void testPartitionStepWithProxyHandler() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
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
	public void testFlowStep() throws Exception {
		StepParserStepFactoryBean<Object, Object> fb = new StepParserStepFactoryBean<Object, Object>();
		fb.setBeanName("step1");
		fb.setAllowStartIfComplete(true);
		fb.setJobRepository(new JobRepositorySupport());
		fb.setStartLimit(5);
		fb.setListeners(new StepListener[] { new StepExecutionListenerSupport() });
		fb.setTaskExecutor(new SyncTaskExecutor());

		fb.setFlow(new SimpleFlow("foo"));

		Object step = fb.getObject();
		assertTrue(step instanceof FlowStep);
		Object handler = ReflectionTestUtils.getField(step, "flow");
		assertTrue(handler instanceof SimpleFlow);
	}

	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}

}
