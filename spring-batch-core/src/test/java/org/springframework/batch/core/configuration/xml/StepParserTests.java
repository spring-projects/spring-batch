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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;

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

	@Test
	public void testParentStep() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");

		// Inline Step
		assertTrue(getListener("s1", ctx) instanceof StepExecutionListenerSupport);

		// Standalone Step
		assertTrue(getListener("s2", ctx) instanceof StepExecutionListenerSupport);

		// Inline With Tasklet Attribute Step
		assertTrue(getListener("s3", ctx) instanceof StepExecutionListenerSupport);

		// Standalone With Tasklet Attribute Step
		assertTrue(getListener("s4", ctx) instanceof StepExecutionListenerSupport);
	}

	@Test
	public void testInheritTransactionAttributes() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");

		// On Inline - No Merge
		validateTransactionAttributesInherited("s1", false, ctx);

		// On Standalone - No Merge
		validateTransactionAttributesInherited("s2", false, ctx);

		// On Inline With Tasklet Ref - No Merge
		validateTransactionAttributesInherited("s3", false, ctx);

		// On Standalone With Tasklet Ref - No Merge
		validateTransactionAttributesInherited("s4", false, ctx);

		// On Inline
		validateTransactionAttributesInherited("s5", true, ctx);

		// On Standalone
		validateTransactionAttributesInherited("s6", true, ctx);

		// On Inline With Tasklet Ref
		validateTransactionAttributesInherited("s7", true, ctx);

		// On Standalone With Tasklet Ref
		validateTransactionAttributesInherited("s8", true, ctx);
	}

	private void validateTransactionAttributesInherited(String stepName, boolean inherited, ApplicationContext ctx) {
		RuleBasedTransactionAttribute txa = getTransactionAttribute(ctx, stepName);
		assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, txa.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_DEFAULT, txa.getIsolationLevel());
		if (inherited) {
			assertEquals(10, txa.getTimeout());
			RollbackRuleAttribute rra = (RollbackRuleAttribute) txa.getRollbackRules().get(0);
			assertEquals("org.springframework.dao.DataIntegrityViolationException", rra.getExceptionName());
		}
		else {
			assertTrue(10 != txa.getTimeout());
			assertTrue(txa.getRollbackRules().isEmpty());
		}
	}

	@SuppressWarnings("unchecked")
	private StepExecutionListener getListener(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Step step = (Step) ctx.getBean(stepName);
		if (step instanceof DelegatingStep) {
			step = (Step) ReflectionTestUtils.getField(step, "delegate");
		}
		assertTrue(step instanceof TaskletStep);
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

	@SuppressWarnings("unchecked")
	private RuleBasedTransactionAttribute getTransactionAttribute(ApplicationContext ctx, String stepName) {
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Step step = (Step) ctx.getBean(stepName);
		if (step instanceof DelegatingStep) {
			step = (Step) ReflectionTestUtils.getField(step, "delegate");
		}
		assertTrue(step instanceof TaskletStep);
		Object transactionAttribute = ReflectionTestUtils.getField(step, "transactionAttribute");
		RuleBasedTransactionAttribute txa = (RuleBasedTransactionAttribute) transactionAttribute;
		return txa;
	}

}
