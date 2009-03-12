/*
 * Copyright 2006-2009 the original author or authors.
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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProvider;
import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Thomas Risberg
 */
public class StepParserTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testTaskletStepAttributes() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserTaskletAttributesTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(FaultTolerantStepFactoryBean.class);
		String factoryName = (String) beans.keySet().toArray()[0];
		FaultTolerantStepFactoryBean<Object, Object> factory = (FaultTolerantStepFactoryBean<Object, Object>) beans
				.get(factoryName);
		TaskletStep bean = (TaskletStep) factory.getObject();
		assertEquals("wrong start-limit:", 25, bean.getStartLimit());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepParserBeanName() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserBeanNameTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		assertEquals("wrong name", "s1", s1.getName());
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testStepParserCommitIntervalCompletionPolicy() throws Exception {
		new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCommitIntervalCompletionPolicyTests-context.xml");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepParserCommitInterval() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCommitIntervalTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		CompletionPolicy completionPolicy = getCompletionPolicy(s1);
		assertTrue(completionPolicy instanceof SimpleCompletionPolicy);
		Field chunkSizeField = SimpleCompletionPolicy.class.getDeclaredField("chunkSize");
		chunkSizeField.setAccessible(true);
		assertEquals(25, chunkSizeField.get(completionPolicy));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepParserCompletionPolicy() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCompletionPolicyTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		CompletionPolicy completionPolicy = getCompletionPolicy(s1);
		assertTrue(completionPolicy instanceof DummyCompletionPolicy);
	}

	@SuppressWarnings("unchecked")
	private CompletionPolicy getCompletionPolicy(Step s1) throws NoSuchFieldException, IllegalAccessException {
		Field taskletField = TaskletStep.class.getDeclaredField("tasklet");
		taskletField.setAccessible(true);
		Tasklet tasklet = (Tasklet) taskletField.get(s1);
		Field chunkProviderField = ChunkOrientedTasklet.class.getDeclaredField("chunkProvider");
		chunkProviderField.setAccessible(true);
		ChunkProvider chunkProvider = (ChunkProvider) chunkProviderField.get(tasklet);
		Field repeatOperationsField = SimpleChunkProvider.class.getDeclaredField("repeatOperations");
		repeatOperationsField.setAccessible(true);
		RepeatOperations repeatOperations = (RepeatOperations) repeatOperationsField.get(chunkProvider);
		Field completionPolicyField = RepeatTemplate.class.getDeclaredField("completionPolicy");
		completionPolicyField.setAccessible(true);
		return (CompletionPolicy) completionPolicyField.get(repeatOperations);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testStepParserNoCommitIntervalOrCompletionPolicy() throws Exception {
		new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserNoCommitIntervalOrCompletionPolicyTests-context.xml");
	}

	@Test
	public void testTaskletStepWithBadStepListener() throws Exception {
		loadContextWithBadListener("org/springframework/batch/core/configuration/xml/StepParserBadStepListenerTests-context.xml");
	}

	@Test
	public void testTaskletStepWithBadRetryListener() throws Exception {
		loadContextWithBadListener("org/springframework/batch/core/configuration/xml/StepParserBadRetryListenerTests-context.xml");
	}

	private void loadContextWithBadListener(String contextLocation) {
		try {
			new ClassPathXmlApplicationContext(contextLocation);
			fail("Context should not load!");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("'ref' and 'class'"));
		}
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testStepParserParentAndRef() throws Exception {
		new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAndRefTests-context.xml");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepParserParentAttribute() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		assertTrue(beans.containsKey("s2"));
		Step s2 = (Step) ctx.getBean("s2");
		assertTrue(beans.containsKey("s3"));
		Step s3 = (Step) ctx.getBean("s3");
		assertTrue(beans.containsKey("s4"));
		Step s4 = (Step) ctx.getBean("s4");

		assertTrue(s1 instanceof TaskletStep);
		assertTrue(getListener((TaskletStep) s1) instanceof StepExecutionListenerSupport);

		assertTrue(s2 instanceof DelegatingStep);
		assertTrue(getListener((DelegatingStep) s2) instanceof StepExecutionListenerSupport);

		assertTrue(s3 instanceof TaskletStep);
		assertTrue(getListener((TaskletStep) s3) instanceof StepExecutionListenerSupport);

		assertTrue(s4 instanceof DelegatingStep);
		assertTrue(getListener((DelegatingStep) s4) instanceof StepExecutionListenerSupport);
	}

	private StepExecutionListener getListener(DelegatingStep step) throws Exception {
		assertTrue(step instanceof DelegatingStep);
		Field delegateField = DelegatingStep.class.getDeclaredField("delegate");
		delegateField.setAccessible(true);
		Object delegate = delegateField.get(step);
		assertTrue(delegate instanceof TaskletStep);
		return getListener((TaskletStep) delegate);
	}

	@SuppressWarnings("unchecked")
	private StepExecutionListener getListener(TaskletStep step) throws Exception {
		Field listenerField = AbstractStep.class.getDeclaredField("stepExecutionListener");
		listenerField.setAccessible(true);
		Object compositeListener = listenerField.get(step);

		Field compositeField = CompositeStepExecutionListener.class.getDeclaredField("list");
		compositeField.setAccessible(true);
		Object composite = compositeField.get(compositeListener);

		Class cls = Class.forName("org.springframework.batch.core.listener.OrderedComposite");
		Field listField = cls.getDeclaredField("list");
		listField.setAccessible(true);
		List<StepExecutionListener> list = (List<StepExecutionListener>) listField.get(composite);

		assertEquals(1, list.size());
		StepExecutionListener listener = list.get(0);
		if (listener instanceof Advised) {
			listener = (StepExecutionListener) ((Advised) listener).getTargetSource().getTarget();
		}
		return listener;
	}
}
