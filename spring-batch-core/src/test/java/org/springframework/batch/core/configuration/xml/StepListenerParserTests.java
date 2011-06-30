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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepListenerParserTests {
	
	@Autowired
	@Qualifier("s1")
	private Step step1;

	@Autowired
	@Qualifier("s2")
	private Step step2;

	@Autowired
	@Qualifier("s3")
	private Step step3;

	@Test
	public void testInheritListeners() throws Exception {

		List<?> list = getListeners(step1);

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
	public void testInheritListenersNoMerge() throws Exception {

		List<?> list = getListeners(step2);

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

	@Test
	public void testInheritListenersNoMergeFaultTolerant() throws Exception {

		List<?> list = getListeners(step3);

		assertEquals(2, list.size());
		boolean a = false;
		boolean b = false;
		for (Object listener : list) {
			if (listener instanceof DummyAnnotationStepExecutionListener) {
				a = true;
			}
			else if (listener instanceof ItemListenerSupport) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@SuppressWarnings("unchecked")
	private List<?> getListeners(Step step) throws Exception {
		assertTrue(step instanceof TaskletStep);

		Object compositeListener = ReflectionTestUtils.getField(step, "stepExecutionListener");
		Object composite = ReflectionTestUtils.getField(compositeListener, "list");
		List<StepListener> proxiedListeners = (List<StepListener>) ReflectionTestUtils.getField(
				composite, "list");
		List<Object> r = new ArrayList<Object>();
		for (Object listener : proxiedListeners) {
			while (listener instanceof Advised) {
				listener = ((Advised) listener).getTargetSource().getTarget();
			}
			r.add(listener);
		}
		compositeListener = ReflectionTestUtils.getField(step, "chunkListener");
		composite = ReflectionTestUtils.getField(compositeListener, "listeners");
		proxiedListeners = (List<StepListener>) ReflectionTestUtils.getField(composite, "list");
		for (Object listener : proxiedListeners) {
			while (listener instanceof Advised) {
				listener = ((Advised) listener).getTargetSource().getTarget();
			}
			r.add(listener);
		}
		try {
			compositeListener = ReflectionTestUtils.getField(
					ReflectionTestUtils.getField(ReflectionTestUtils.getField(
							ReflectionTestUtils.getField(step, "tasklet"), "chunkProvider"), "listener"),
					"itemReadListener");
			composite = ReflectionTestUtils.getField(compositeListener, "listeners");
			proxiedListeners = (List<StepListener>) ReflectionTestUtils.getField(composite, "list");
			for (Object listener : proxiedListeners) {
				while (listener instanceof Advised) {
					listener = ((Advised) listener).getTargetSource().getTarget();
				}
				r.add(listener);
			}
		}
		catch (IllegalArgumentException e) {
			// ignore (not a chunk oriented step)
		}
		return r;
	}

}
