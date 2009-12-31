/*
 * Copyright 2002-2008 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.classify.SubclassClassifier;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.listener.RetryListenerSupport;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0
 */
public class ChunkElementParserTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testSimpleAttributes() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSimpleAttributeParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue("Wrong processor type", chunkProcessor instanceof SimpleChunkProcessor);
	}

	@Test
	public void testSkipPolicyAttribute() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSkipPolicyParserTests-context.xml");
		Map<Class<? extends Throwable>, Boolean> skippable = getExceptionClasses("s1", context);
		assertEquals(2, skippable.size());
		containsClassified(skippable, NullPointerException.class, true);
		containsClassified(skippable, ArithmeticException.class, true);
	}

	@Test
	public void testSkipPolicyElement() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSkipPolicyParserTests-context.xml");
		Map<Class<? extends Throwable>, Boolean> skippable = getExceptionClasses("s2", context);
		assertEquals(1, skippable.size());
		containsClassified(skippable, ArithmeticException.class, true);
	}

	@Test
	public void testProcessorTransactionalAttributes() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementTransactionalAttributeParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		Boolean processorTransactional = (Boolean) ReflectionTestUtils.getField(chunkProcessor,
				"processorTransactional");
		assertFalse("Flag not set", processorTransactional);
	}

	@Test
	public void testProcessorTransactionalNotAllowedOnSimpleProcessor() throws Exception {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/ChunkElementIllegalAttributeParserTests-context.xml");
			fail("Expected BeanCreationException");
		}
		catch (BeanCreationException e) {
			String msg = e.getMessage();
			assertTrue("Wrong message: " +msg, msg.contains("The field 'processor-transactional' is not permitted"));
		}
	}

	@Test
	public void testProcessorNonTransactionalNotAllowedWithTransactionalReader() throws Exception {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/ChunkElementIllegalTransactionalAttributeParserTests-context.xml");
			fail("Expected BeanCreationException");
		}
		catch (BeanCreationException e) {
			String msg = e.getMessage();
			assertTrue("Wrong message: " +msg, msg.contains("The field 'processor-transactional' cannot be false if 'reader-transactional"));
		}

	}

	@Test
	public void testInheritSkippable() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippable = getExceptionClasses("s1", getContext());
		assertEquals(5, skippable.size());
		containsClassified(skippable, NullPointerException.class, true);
		containsClassified(skippable, ArithmeticException.class, true);
		containsClassified(skippable, CannotAcquireLockException.class, false);
		containsClassified(skippable, DeadlockLoserDataAccessException.class, false);
	}

	@Test
	public void testInheritSkippableWithNoMerge() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippable = getExceptionClasses("s2", getContext());
		assertEquals(3, skippable.size());
		containsClassified(skippable, NullPointerException.class, true);
		assertFalse(skippable.containsKey(ArithmeticException.class));
		containsClassified(skippable, CannotAcquireLockException.class, false);
		assertFalse(skippable.containsKey(DeadlockLoserDataAccessException.class));
	}

	@Test
	public void testInheritStreams() throws Exception {
		Collection<ItemStream> streams = getStreams("s1", getContext());
		assertEquals(2, streams.size());
		boolean c = false;
		for (ItemStream o : streams) {
			if (o instanceof CompositeItemStream) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	public void testInheritRetryListeners() throws Exception {
		Collection<RetryListener> retryListeners = getRetryListeners("s1", getContext());
		assertEquals(2, retryListeners.size());
		boolean g = false;
		boolean h = false;
		for (RetryListener o : retryListeners) {
			if (o instanceof RetryListenerSupport) {
				g = true;
			}
			else if (o instanceof DummyRetryListener) {
				h = true;
			}
		}
		assertTrue(g);
		assertTrue(h);
	}

	@Test
	public void testInheritStreamsWithNoMerge() throws Exception {
		Collection<ItemStream> streams = getStreams("s2", getContext());
		assertEquals(1, streams.size());
		boolean c = false;
		for (ItemStream o : streams) {
			if (o instanceof CompositeItemStream) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	public void testInheritRetryListenersWithNoMerge() throws Exception {
		Collection<RetryListener> retryListeners = getRetryListeners("s2", getContext());
		assertEquals(1, retryListeners.size());
		boolean h = false;
		for (RetryListener o : retryListeners) {
			if (o instanceof DummyRetryListener) {
				h = true;
			}
		}
		assertTrue(h);
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionClasses(String stepName, ApplicationContext ctx)
			throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object skipPolicy = ReflectionTestUtils.getField(chunkProvider, "skipPolicy");
		SubclassClassifier<Throwable, SkipPolicy> classifier = (SubclassClassifier<Throwable, SkipPolicy>) ReflectionTestUtils
				.getField(skipPolicy, "classifier");
		Object limitPolicy = classifier.classify(new Exception());
		Object limitClassifier = ReflectionTestUtils.getField(limitPolicy, "skippableExceptionClassifier");
		return (Map<Class<? extends Throwable>, Boolean>) ReflectionTestUtils.getField(limitClassifier, "classified");
	}

	private void containsClassified(Map<Class<? extends Throwable>, Boolean> classified,
			Class<? extends Throwable> cls, boolean include) {
		assertTrue(classified.containsKey(cls));
		assertEquals(include, classified.get(cls));
	}

	@SuppressWarnings("unchecked")
	private Collection<ItemStream> getStreams(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object compositeStream = ReflectionTestUtils.getField(step, "stream");
		return (Collection<ItemStream>) ReflectionTestUtils.getField(compositeStream, "streams");
	}

	@SuppressWarnings("unchecked")
	private Collection<RetryListener> getRetryListeners(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		Object retryTemplate = ReflectionTestUtils.getField(chunkProcessor, "batchRetryTemplate");
		Object regular = ReflectionTestUtils.getField(retryTemplate, "regular");
		RetryListener[] listeners = (RetryListener[]) ReflectionTestUtils.getField(regular, "listeners");
		return Arrays.asList(listeners);
	}

	/**
	 * @return the chunkElementParentAttributeParserTestsContext
	 */
	private ConfigurableApplicationContext getContext() {
		return new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementParentAttributeParserTests-context.xml");
	}
}
