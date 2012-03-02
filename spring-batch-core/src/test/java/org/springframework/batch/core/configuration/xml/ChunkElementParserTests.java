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
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0
 */
public class ChunkElementParserTests {

	@Test
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
	public void testCommitIntervalLateBinding() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementLateBindingParserTests-context.xml");
		Step step = (Step) context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
	}

	@Test
	public void testSkipAndRetryAttributes() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSkipAndRetryAttributeParserTests-context.xml");
		Step step = (Step) context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
	}

	@Test
	public void testIllegalSkipAndRetryAttributes() throws Exception {
		try {
			ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementIllegalSkipAndRetryAttributeParserTests-context.xml");
		Step step = (Step) context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
		fail("Expected BeanCreationException");
		} catch (BeanCreationException e) {
			// expected
		}
	}

	@Test
	public void testRetryPolicyAttribute() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementRetryPolicyParserTests-context.xml");
		Map<Class<? extends Throwable>, Boolean> retryable = getNestedExceptionMap("s1", context,
				"tasklet.chunkProcessor.batchRetryTemplate.regular.retryPolicy.exceptionClassifier",
				"exceptionClassifier");
		assertEquals(2, retryable.size());
		assertTrue(retryable.containsKey(NullPointerException.class));
		assertTrue(retryable.containsKey(ArithmeticException.class));
	}

	@Test
	public void testRetryPolicyElement() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementRetryPolicyParserTests-context.xml");
		SimpleRetryPolicy policy = (SimpleRetryPolicy) getPolicy("s2", context,
				"tasklet.chunkProcessor.batchRetryTemplate.regular.retryPolicy.exceptionClassifier");
		assertEquals(2, policy.getMaxAttempts());
	}

	@Test
	public void testSkipPolicyAttribute() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSkipPolicyParserTests-context.xml");
		SkipPolicy policy = getSkipPolicy("s1", context);
		assertTrue(policy.shouldSkip(new NullPointerException(), 0));
		assertTrue(policy.shouldSkip(new ArithmeticException(), 0));
	}

	@Test
	public void testSkipPolicyElement() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementSkipPolicyParserTests-context.xml");
		SkipPolicy policy = getSkipPolicy("s2", context);
		assertFalse(policy.shouldSkip(new NullPointerException(), 0));
		assertTrue(policy.shouldSkip(new ArithmeticException(), 0));
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
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ChunkElementIllegalAttributeParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull("Step not parsed", step);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof SimpleChunkProcessor<?, ?>);
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
			assertTrue("Wrong message: " + msg, msg
					.contains("The field 'processor-transactional' cannot be false if 'reader-transactional"));
		}

	}

	@Test
	public void testRetryable() throws Exception {
		Map<Class<? extends Throwable>, Boolean> retryable = getRetryableExceptionClasses("s1", getContext());
		System.err.println(retryable);
		assertEquals(3, retryable.size());
		containsClassified(retryable, PessimisticLockingFailureException.class, true);
		containsClassified(retryable, CannotSerializeTransactionException.class, false);
	}

	@Test
	public void testRetryableInherited() throws Exception {
		Map<Class<? extends Throwable>, Boolean> retryable = getRetryableExceptionClasses("s3", getContext());
		System.err.println(retryable);
		assertEquals(2, retryable.size());
		containsClassified(retryable, IOException.class, true);
	}

	@Test
	public void testRetryableInheritedMerge() throws Exception {
		Map<Class<? extends Throwable>, Boolean> retryable = getRetryableExceptionClasses("s4", getContext());
		System.err.println(retryable);
		assertEquals(3, retryable.size());
		containsClassified(retryable, IOException.class, true);
	}

	@Test
	public void testInheritSkippable() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippable = getSkippableExceptionClasses("s1", getContext());
		System.err.println(skippable);
		assertEquals(5, skippable.size());
		containsClassified(skippable, NullPointerException.class, true);
		containsClassified(skippable, ArithmeticException.class, true);
		containsClassified(skippable, CannotAcquireLockException.class, false);
		containsClassified(skippable, DeadlockLoserDataAccessException.class, false);
	}

	@Test
	public void testInheritSkippableWithNoMerge() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippable = getSkippableExceptionClasses("s2", getContext());
		assertEquals(3, skippable.size());
		containsClassified(skippable, IllegalArgumentException.class, true);
		assertFalse(skippable.containsKey(ArithmeticException.class));
		containsClassified(skippable, ConcurrencyFailureException.class, false);
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

	private Map<Class<? extends Throwable>, Boolean> getSkippableExceptionClasses(String stepName,
			ApplicationContext ctx) throws Exception {
		return getNestedExceptionMap(stepName, ctx, "tasklet.chunkProvider.skipPolicy.classifier",
				"skippableExceptionClassifier");
	}

	private SkipPolicy getSkipPolicy(String stepName,
			ApplicationContext ctx) throws Exception {
		return (SkipPolicy) getNestedPathInStep(stepName, ctx, "tasklet.chunkProvider.skipPolicy");
	}

	private Map<Class<? extends Throwable>, Boolean> getRetryableExceptionClasses(String stepName,
			ApplicationContext ctx) throws Exception {
		return getNestedExceptionMap(stepName, ctx,
				"tasklet.chunkProcessor.batchRetryTemplate.regular.retryPolicy.exceptionClassifier",
				"retryableClassifier");
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getNestedExceptionMap(String stepName, ApplicationContext ctx,
			String componentName, String classifierName) throws Exception {

		Object policy = getPolicy(stepName, ctx, componentName);
		Object exceptionClassifier = ReflectionTestUtils.getField(policy, classifierName);

		return (Map<Class<? extends Throwable>, Boolean>) ReflectionTestUtils.getField(exceptionClassifier,
				"classified");

	}

	private Object getPolicy(String stepName, ApplicationContext ctx, String componentName) throws Exception {
		@SuppressWarnings("unchecked")
		SubclassClassifier<Throwable, Object> classifier = (SubclassClassifier<Throwable, Object>) getNestedPathInStep(
				stepName, ctx, componentName);
		Object policy = classifier.classify(new Exception());
		return policy;
	}

	@SuppressWarnings("unchecked")
	private Object getNestedPathInStep(String stepName, ApplicationContext ctx, String path) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		return getNestedPath(step, path);
	}

	/**
	 * @param object the target object
	 * @param path the path to the required field
	 * @return
	 */
	private Object getNestedPath(Object object, String path) {
		while (StringUtils.hasText(path)) {
			int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(path);
			if (index < 0) {
				index = path.length();
			}
			object = ReflectionTestUtils.getField(object, path.substring(0, index));
			if (index < path.length()) {
				path = path.substring(index + 1);
			}
			else {
				path = "";
			}
		}
		return object;
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
