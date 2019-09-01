/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.core.configuration.xml.AbstractTestComponent;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_STEP;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_WRITE;

/**
 * @author Lucas Ward
 *
 */
public class StepListenerFactoryBeanTests {

	private StepListenerFactoryBean factoryBean;

	private JobExecution jobExecution = new JobExecution(11L);

	private StepExecution stepExecution = new StepExecution("testStep", jobExecution);

	@Before
	public void setUp() {
		factoryBean = new StepListenerFactoryBean();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStepAndChunk() throws Exception {
		TestListener testListener = new TestListener();
		factoryBean.setDelegate(testListener);
		//		Map<String, String> metaDataMap = new HashMap<String, String>();
		//		metaDataMap.put(AFTER_STEP.getPropertyName(), "destroy");
		//		metaDataMap.put(AFTER_CHUNK.getPropertyName(), "afterChunk");
		//		factoryBean.setMetaDataMap(metaDataMap);
		String readItem = "item";
		Integer writeItem = 2;
		List<Integer> writeItems = Arrays.asList(writeItem);
		StepListener listener = (StepListener) factoryBean.getObject();
		((StepExecutionListener) listener).beforeStep(stepExecution);
		((StepExecutionListener) listener).afterStep(stepExecution);
		((ChunkListener) listener).beforeChunk(null);
		((ChunkListener) listener).afterChunk(null);
		((ChunkListener) listener).afterChunkError(new ChunkContext(null));
		((ItemReadListener<String>) listener).beforeRead();
		((ItemReadListener<String>) listener).afterRead(readItem);
		((ItemReadListener<String>) listener).onReadError(new Exception());
		((ItemProcessListener<String, Integer>) listener).beforeProcess(readItem);
		((ItemProcessListener<String, Integer>) listener).afterProcess(readItem, writeItem);
		((ItemProcessListener<String, Integer>) listener).onProcessError(readItem, new Exception());
		((ItemWriteListener<Integer>) listener).beforeWrite(writeItems);
		((ItemWriteListener<Integer>) listener).afterWrite(writeItems);
		((ItemWriteListener<Integer>) listener).onWriteError(new Exception(), writeItems);
		((SkipListener<String, Integer>) listener).onSkipInRead(new Throwable());
		((SkipListener<String, Integer>) listener).onSkipInProcess(readItem, new Throwable());
		((SkipListener<String, Integer>) listener).onSkipInWrite(writeItem, new Throwable());
		assertTrue(testListener.beforeStepCalled);
		assertTrue(testListener.beforeChunkCalled);
		assertTrue(testListener.afterChunkCalled);
		assertTrue(testListener.afterChunkErrorCalled);
		assertTrue(testListener.beforeReadCalled);
		assertTrue(testListener.afterReadCalled);
		assertTrue(testListener.onReadErrorCalled);
		assertTrue(testListener.beforeProcessCalled);
		assertTrue(testListener.afterProcessCalled);
		assertTrue(testListener.onProcessErrorCalled);
		assertTrue(testListener.beforeWriteCalled);
		assertTrue(testListener.afterWriteCalled);
		assertTrue(testListener.onWriteErrorCalled);
		assertTrue(testListener.onSkipInReadCalled);
		assertTrue(testListener.onSkipInProcessCalled);
		assertTrue(testListener.onSkipInWriteCalled);
	}

	@Test
	public void testAllThreeTypes() throws Exception {
		// Test to make sure if someone has annotated a method, implemented the
		// interface, and given a string
		// method name, that all three will be called
		ThreeStepExecutionListener delegate = new ThreeStepExecutionListener();
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_STEP.getPropertyName(), "destroy");
		factoryBean.setMetaDataMap(metaDataMap);
		StepListener listener = (StepListener) factoryBean.getObject();
		((StepExecutionListener) listener).afterStep(stepExecution);
		assertEquals(3, delegate.callcount);
	}

	@Test
	public void testAnnotatingInterfaceResultsInOneCall() throws Exception {
		MultipleAfterStep delegate = new MultipleAfterStep();
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_STEP.getPropertyName(), "afterStep");
		factoryBean.setMetaDataMap(metaDataMap);
		StepListener listener = (StepListener) factoryBean.getObject();
		((StepExecutionListener) listener).afterStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	public void testVanillaInterface() throws Exception {
		MultipleAfterStep delegate = new MultipleAfterStep();
		factoryBean.setDelegate(delegate);
		Object listener = factoryBean.getObject();
		assertTrue(listener instanceof StepExecutionListener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	public void testVanillaInterfaceWithProxy() throws Exception {
		MultipleAfterStep delegate = new MultipleAfterStep();
		ProxyFactory factory = new ProxyFactory(delegate);
		factoryBean.setDelegate(factory.getProxy());
		Object listener = factoryBean.getObject();
		assertTrue(listener instanceof StepExecutionListener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	public void testFactoryMethod() throws Exception {
		MultipleAfterStep delegate = new MultipleAfterStep();
		Object listener = StepListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof StepExecutionListener);
		assertFalse(listener instanceof ChunkListener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	public void testAnnotationsWithOrdered() throws Exception {
		Object delegate = new Ordered() {
			@BeforeStep
			public void foo(StepExecution execution) {
			}

			@Override
			public int getOrder() {
				return 3;
			}
		};
		StepListener listener = StepListenerFactoryBean.getListener(delegate);
		assertTrue("Listener is not of correct type", listener instanceof Ordered);
		assertEquals(3, ((Ordered) listener).getOrder());
	}

	@Test
	public void testProxiedAnnotationsFactoryMethod() throws Exception {
		Object delegate = new InitializingBean() {
			@BeforeStep
			public void foo(StepExecution execution) {
			}

			@Override
			public void afterPropertiesSet() throws Exception {
			}
		};
		ProxyFactory factory = new ProxyFactory(delegate);
		assertTrue("Listener is not of correct type",
				StepListenerFactoryBean.getListener(factory.getProxy()) instanceof StepExecutionListener);
	}

	@Test
	public void testInterfaceIsListener() throws Exception {
		assertTrue(StepListenerFactoryBean.isListener(new ThreeStepExecutionListener()));
	}

	@Test
	public void testAnnotationsIsListener() throws Exception {
		assertTrue(StepListenerFactoryBean.isListener(new Object() {
			@BeforeStep
			public void foo(StepExecution execution) {
			}
		}));
	}

	@Test
	public void testProxyWithNoTarget() throws Exception {
		ProxyFactory factory = new ProxyFactory();
		factory.addInterface(DataSource.class);
		factory.addAdvice(new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				return null;
			}
		});
		Object proxy = factory.getProxy();
		assertFalse(StepListenerFactoryBean.isListener(proxy));
	}

	@Test
	public void testProxiedAnnotationsIsListener() throws Exception {
		Object delegate = new InitializingBean() {
			@BeforeStep
			public void foo(StepExecution execution) {
			}

			@Override
			public void afterPropertiesSet() throws Exception {
			}
		};
		ProxyFactory factory = new ProxyFactory(delegate);
		Object proxy = factory.getProxy();
		assertTrue(StepListenerFactoryBean.isListener(proxy));
		((StepExecutionListener) StepListenerFactoryBean.getListener(proxy)).beforeStep(null);
	}

	@Test
	public void testMixedIsListener() throws Exception {
		assertTrue(StepListenerFactoryBean.isListener(new MultipleAfterStep()));
	}

	@Test
	public void testNonListener() throws Exception {
		Object delegate = new Object();
		factoryBean.setDelegate(delegate);
		assertTrue(factoryBean.getObject() instanceof StepListener);
	}

	@Test
	public void testEmptySignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod() {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Arrays.asList("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	public void testRightSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod(List<String> items) {
				executed = true;
				assertEquals("foo", items.get(0));
				assertEquals("bar", items.get(1));
			}
		};
		factoryBean.setDelegate(delegate);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Arrays.asList("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod(Integer item) {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		factoryBean.getObject();
	}

	@Test
	public void testEmptySignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod() {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_WRITE.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Arrays.asList("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	public void testRightSignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod(List<String> items) {
				executed = true;
				assertEquals("foo", items.get(0));
				assertEquals("bar", items.get(1));
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_WRITE.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Arrays.asList("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongSignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod(Integer item) {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_WRITE.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		factoryBean.getObject();
	}

	private class MultipleAfterStep implements StepExecutionListener {

		int callcount = 0;

		@Nullable
		@Override
		@AfterStep
		public ExitStatus afterStep(StepExecution stepExecution) {
			Assert.notNull(stepExecution, "A stepExecution is required");
			callcount++;
			return null;
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			callcount++;
		}

	}

	@SuppressWarnings("unused")
	private class ThreeStepExecutionListener implements StepExecutionListener {

		int callcount = 0;

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			Assert.notNull(stepExecution, "A stepExecution is required");
			callcount++;
			return null;
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			callcount++;
		}

		public void destroy() {
			callcount++;
		}

		@AfterStep
		public void after() {
			callcount++;
		}

	}

	@SuppressWarnings("unused")
	private class TestListener implements SkipListener<String, Integer> {

		boolean beforeStepCalled = false;

		boolean afterStepCalled = false;

		boolean beforeChunkCalled = false;

		boolean afterChunkCalled = false;

		boolean afterChunkErrorCalled = false;

		boolean beforeReadCalled = false;

		boolean afterReadCalled = false;

		boolean onReadErrorCalled = false;

		boolean beforeProcessCalled = false;

		boolean afterProcessCalled = false;

		boolean onProcessErrorCalled = false;

		boolean beforeWriteCalled = false;

		boolean afterWriteCalled = false;

		boolean onWriteErrorCalled = false;

		boolean onSkipInReadCalled = false;

		boolean onSkipInProcessCalled = false;

		boolean onSkipInWriteCalled = false;

		@BeforeStep
		public void initStep() {
			beforeStepCalled = true;
		}

		@AfterStep
		public void destroy() {
			afterStepCalled = true;
		}

		@BeforeChunk
		public void before() {
			beforeChunkCalled = true;
		}

		@AfterChunk
		public void afterChunk() {
			afterChunkCalled = true;
		}

		@AfterChunkError
		public void afterChunkError(ChunkContext context) {
			afterChunkErrorCalled = true;
		}

		@BeforeRead
		public void beforeReadMethod() {
			beforeReadCalled = true;
		}

		@AfterRead
		public void afterReadMethod(Object item) {
			Assert.notNull(item, "An item is required");
			afterReadCalled = true;
		}

		@OnReadError
		public void onErrorInRead() {
			onReadErrorCalled = true;
		}

		@BeforeProcess
		public void beforeProcess() {
			beforeProcessCalled = true;
		}

		@AfterProcess
		public void afterProcess(String item, Integer result) {
			afterProcessCalled = true;
		}

		@OnProcessError
		public void processError() {
			onProcessErrorCalled = true;
		}

		@BeforeWrite
		public void beforeWrite() {
			beforeWriteCalled = true;
		}

		@AfterWrite
		public void afterWrite() {
			afterWriteCalled = true;
		}

		@OnWriteError
		public void writeError() {
			onWriteErrorCalled = true;
		}

		@Override
		public void onSkipInProcess(String item, Throwable t) {
			onSkipInProcessCalled = true;
		}

		@Override
		public void onSkipInRead(Throwable t) {
			onSkipInReadCalled = true;
		}

		@Override
		public void onSkipInWrite(Integer item, Throwable t) {
			onSkipInWriteCalled = true;
		}

	}
}
