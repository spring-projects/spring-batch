/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
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
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_STEP;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_WRITE;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
class StepListenerFactoryBeanTests {

	private final StepListenerFactoryBean factoryBean = new StepListenerFactoryBean();

	private final JobExecution jobExecution = new JobExecution(11L);

	private final StepExecution stepExecution = new StepExecution("testStep", jobExecution);

	@Test
	@SuppressWarnings("unchecked")
	void testStepAndChunk() {
		TestListener testListener = new TestListener();
		factoryBean.setDelegate(testListener);
		// Map<String, String> metaDataMap = new HashMap<String, String>();
		// metaDataMap.put(AFTER_STEP.getPropertyName(), "destroy");
		// metaDataMap.put(AFTER_CHUNK.getPropertyName(), "afterChunk");
		// factoryBean.setMetaDataMap(metaDataMap);
		String readItem = "item";
		Integer writeItem = 2;
		Chunk<Integer> writeItems = Chunk.of(writeItem);
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
	void testAllThreeTypes() {
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
	void testAnnotatingInterfaceResultsInOneCall() {
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
	void testVanillaInterface() {
		MultipleAfterStep delegate = new MultipleAfterStep();
		factoryBean.setDelegate(delegate);
		Object listener = factoryBean.getObject();
		assertInstanceOf(StepExecutionListener.class, listener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	void testVanillaInterfaceWithProxy() {
		MultipleAfterStep delegate = new MultipleAfterStep();
		ProxyFactory factory = new ProxyFactory(delegate);
		factoryBean.setDelegate(factory.getProxy());
		Object listener = factoryBean.getObject();
		assertInstanceOf(StepExecutionListener.class, listener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	void testFactoryMethod() {
		MultipleAfterStep delegate = new MultipleAfterStep();
		Object listener = StepListenerFactoryBean.getListener(delegate);
		assertInstanceOf(StepExecutionListener.class, listener);
		assertFalse(listener instanceof ChunkListener);
		((StepExecutionListener) listener).beforeStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}

	@Test
	void testAnnotationsWithOrdered() {
		Object delegate = new Ordered() {
			@BeforeStep
			public void foo(@SuppressWarnings("unused") StepExecution execution) {
			}

			@Override
			public int getOrder() {
				return 3;
			}
		};
		StepListener listener = StepListenerFactoryBean.getListener(delegate);
		assertInstanceOf(Ordered.class, listener, "Listener is not of correct type");
		assertEquals(3, ((Ordered) listener).getOrder());
	}

	@Test
	void testProxiedAnnotationsFactoryMethod() {
		Object delegate = new InitializingBean() {
			@BeforeStep
			public void foo(@SuppressWarnings("unused") StepExecution execution) {
			}

			@Override
			public void afterPropertiesSet() {
			}
		};
		ProxyFactory factory = new ProxyFactory(delegate);
		assertInstanceOf(StepExecutionListener.class, StepListenerFactoryBean.getListener(factory.getProxy()),
				"Listener is not of correct type");
	}

	@Test
	void testInterfaceIsListener() {
		assertTrue(StepListenerFactoryBean.isListener(new ThreeStepExecutionListener()));
	}

	@Test
	void testAnnotationsIsListener() {
		assertTrue(StepListenerFactoryBean.isListener(new Object() {
			@BeforeStep
			public void foo(@SuppressWarnings("unused") StepExecution execution) {
			}
		}));
	}

	@Test
	void testProxyWithNoTarget() {
		ProxyFactory factory = new ProxyFactory();
		factory.addInterface(DataSource.class);
		factory.addAdvice((MethodInterceptor) invocation -> null);
		Object proxy = factory.getProxy();
		assertFalse(StepListenerFactoryBean.isListener(proxy));
	}

	@Test
	void testProxiedAnnotationsIsListener() {
		Object delegate = new InitializingBean() {
			@BeforeStep
			public void foo(@SuppressWarnings("unused") StepExecution execution) {
			}

			@Override
			public void afterPropertiesSet() {
			}
		};
		ProxyFactory factory = new ProxyFactory(delegate);
		Object proxy = factory.getProxy();
		assertTrue(StepListenerFactoryBean.isListener(proxy));
		((StepExecutionListener) StepListenerFactoryBean.getListener(proxy)).beforeStep(null);
	}

	@Test
	void testMixedIsListener() {
		assertTrue(StepListenerFactoryBean.isListener(new MultipleAfterStep()));
	}

	@Test
	void testNonListener() {
		Object delegate = new Object();
		factoryBean.setDelegate(delegate);
		assertInstanceOf(StepListener.class, factoryBean.getObject());
	}

	@Test
	void testEmptySignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod() {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Chunk.of("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	void testRightSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod(Chunk<String> chunk) {
				executed = true;
				assertEquals("foo", chunk.getItems().get(0));
				assertEquals("bar", chunk.getItems().get(1));
			}
		};
		factoryBean.setDelegate(delegate);
		@SuppressWarnings("unchecked")
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Chunk.of("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	void testWrongSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@AfterWrite
			public void aMethod(@SuppressWarnings("unused") Integer item) {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		assertThrows(IllegalArgumentException.class, factoryBean::getObject);
	}

	@Test
	void testEmptySignatureNamedMethod() {
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
		listener.afterWrite(Chunk.of("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	void testRightSignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod(Chunk<String> chunk) {
				executed = true;
				assertEquals("foo", chunk.getItems().get(0));
				assertEquals("bar", chunk.getItems().get(1));
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<>();
		metaDataMap.put(AFTER_WRITE.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		ItemWriteListener<String> listener = (ItemWriteListener<String>) factoryBean.getObject();
		listener.afterWrite(Chunk.of("foo", "bar"));
		assertTrue(delegate.isExecuted());
	}

	@Test
	void testWrongSignatureNamedMethod() {
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
		assertThrows(IllegalArgumentException.class, factoryBean::getObject);
	}

	private static class MultipleAfterStep implements StepExecutionListener {

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
	private static class ThreeStepExecutionListener implements StepExecutionListener {

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
	private static class TestListener implements SkipListener<String, Integer> {

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
