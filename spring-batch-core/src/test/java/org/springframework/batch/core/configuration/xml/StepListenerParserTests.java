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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Garrette
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepListenerParserTests {

	@Test
	public void testStepListenerParser() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepListenerParserTests-context.xml");
		List<?> list = getListeners("s1", ctx);

		assertEquals(3, list.size());
		boolean a = false;
		boolean b = false;
		boolean c = false;
		for (Object listener : list) {
			if (listener instanceof DummyAnnotationStepExecutionListener) {
				a = true;
			}
			else if (listener instanceof StepExecutionListenerSupport) {
				b = true;
			}
			else if (listener instanceof CompositeStepExecutionListener) {
				c = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
		assertTrue(c);
	}

	@Test
	public void testStepListenerParserNoMerge() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepListenerParserNoMergeTests-context.xml");
		List<?> list = getListeners("s1", ctx);

		assertEquals(2, list.size());
		boolean a = false;
		boolean b = false;
		for (Object listener : list) {
			if (listener instanceof DummyAnnotationStepExecutionListener) {
				a = true;
			}
			else if (listener instanceof StepExecutionListenerSupport) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@SuppressWarnings("unchecked")
	private List<?> getListeners(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Field listenerField = AbstractStep.class.getDeclaredField("stepExecutionListener");
		listenerField.setAccessible(true);
		Object compositeListener = listenerField.get(step);

		Field compositeField = CompositeStepExecutionListener.class.getDeclaredField("list");
		compositeField.setAccessible(true);
		Object composite = compositeField.get(compositeListener);

		Class cls = Class.forName("org.springframework.batch.core.listener.OrderedComposite");
		Field listField = cls.getDeclaredField("list");
		listField.setAccessible(true);
		List<StepExecutionListener> proxiedListeners = (List<StepExecutionListener>) listField.get(composite);
		List<Object> r = new ArrayList<Object>();
		for (Object listener : proxiedListeners) {
			while (listener instanceof Advised) {
				listener = ((Advised) listener).getTargetSource().getTarget();
			}
			r.add(listener);
		}
		return r;
	}

}
