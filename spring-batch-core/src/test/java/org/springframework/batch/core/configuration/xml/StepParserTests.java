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

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

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
		assertEquals(25, ReflectionTestUtils.getField(completionPolicy, "chunkSize"));
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

	private CompletionPolicy getCompletionPolicy(Step s1) throws NoSuchFieldException, IllegalAccessException {
		Object tasklet = ReflectionTestUtils.getField(s1, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object repeatOperations = ReflectionTestUtils.getField(chunkProvider, "repeatOperations");
		return (CompletionPolicy) ReflectionTestUtils.getField(repeatOperations, "completionPolicy");
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
		} catch (BeanDefinitionParsingException e) {
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
	public void testParentOnInlineStep() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		assertTrue(s1 instanceof TaskletStep);
		assertTrue(getListener((TaskletStep) s1) instanceof StepExecutionListenerSupport);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testParentOnStandaloneStep() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s2"));
		Step s2 = (Step) ctx.getBean("s2");
		assertTrue(s2 instanceof DelegatingStep);
		assertTrue(getListener((DelegatingStep) s2) instanceof StepExecutionListenerSupport);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testParentOnInlineWithTaskletAttributeStep() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s3"));
		Step s3 = (Step) ctx.getBean("s3");
		assertTrue(s3 instanceof TaskletStep);
		assertTrue(getListener((TaskletStep) s3) instanceof StepExecutionListenerSupport);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testParentOnStandaloneWithTaskletAttributeStep() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s4"));
		Step s4 = (Step) ctx.getBean("s4");
		assertTrue(s4 instanceof DelegatingStep);
		assertTrue(getListener((DelegatingStep) s4) instanceof StepExecutionListenerSupport);
	}

	private StepExecutionListener getListener(DelegatingStep step) throws Exception {
		assertTrue(step instanceof DelegatingStep);
		Object delegate = ReflectionTestUtils.getField(step, "delegate");
		assertTrue(delegate instanceof TaskletStep);
		return getListener((TaskletStep) delegate);
	}

	@SuppressWarnings("unchecked")
	private StepExecutionListener getListener(TaskletStep step) throws Exception {
		Object compositeListener = ReflectionTestUtils.getField(step, "stepExecutionListener");
		Object composite = ReflectionTestUtils.getField(compositeListener, "list");
		List<StepExecutionListener> list = (List<StepExecutionListener>) ReflectionTestUtils
				.getField(composite, "list");

		assertEquals(1, list.size());
		StepExecutionListener listener = list.get(0);
		if (listener instanceof Advised) {
			listener = (StepExecutionListener) ((Advised) listener).getTargetSource().getTarget();
		}
		return listener;
	}
}
