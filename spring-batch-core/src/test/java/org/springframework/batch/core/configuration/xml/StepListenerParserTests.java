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

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
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

	@SuppressWarnings("unchecked")
	@Test
	public void testStepListenerParser() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepListenerParserTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		assertTrue(s1 instanceof TaskletStep);

		Field listenerField = AbstractStep.class.getDeclaredField("stepExecutionListener");
		listenerField.setAccessible(true);
		Object compositeListener = listenerField.get(s1);

		Field compositeField = CompositeStepExecutionListener.class.getDeclaredField("list");
		compositeField.setAccessible(true);
		Object composite = compositeField.get(compositeListener);

		Class cls = Class.forName("org.springframework.batch.core.listener.OrderedComposite");
		Field listField = cls.getDeclaredField("list");
		listField.setAccessible(true);
		List<StepExecutionListener> list = (List<StepExecutionListener>) listField.get(composite);

//		assertEquals(3, list.size());
		boolean a = false;
		boolean b = false;
		boolean c = false;
		for (StepExecutionListener listener : list) {
			if (listener instanceof Advised) {
				listener = (StepExecutionListener) ((Advised) listener).getTargetSource().getTarget();
			}
			if (listener instanceof TestListener) {
				a = true;
			}
			if (listener instanceof StepExecutionListenerSupport) {
				b = true;
			}
			if (listener instanceof CompositeStepExecutionListener) {
				c = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
//		assertTrue(c);
	}

}
