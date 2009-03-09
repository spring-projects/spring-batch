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
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.Step;
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
		System.err.println(completionPolicy);
		assertTrue(completionPolicy instanceof DummyCompletionPolicy);
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

}
