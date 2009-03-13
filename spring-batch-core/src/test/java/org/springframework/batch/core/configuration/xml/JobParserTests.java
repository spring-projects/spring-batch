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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class JobParserTests {

	@Test
	public void testInheritListeners() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/JobParserParentAttributeTests-context.xml");
		List<?> job1Listeners = getListeners("job1", ctx);
		assertEquals(2, job1Listeners.size());
		boolean a = false;
		boolean b = false;
		for (Object l : job1Listeners) {
			if (l instanceof DummyAnnotationJobExecutionListener) {
				a = true;
			}
			else if (l instanceof JobExecutionListenerSupport) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@Test
	public void testInheritListeners_NoMerge() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/JobParserParentAttributeTests-context.xml");
		List<?> job2Listeners = getListeners("job2", ctx);
		assertEquals(1, job2Listeners.size());
		boolean c = false;
		for (Object l : job2Listeners) {
			if (l instanceof JobExecutionListenerSupport) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@SuppressWarnings("unchecked")
	private List<?> getListeners(String jobName, ApplicationContext ctx) throws Exception {
		Map<String, Object> beans = ctx.getBeansOfType(Job.class);
		assertTrue(beans.containsKey(jobName));
		Job job = (Job) ctx.getBean(jobName);

		assertTrue(job instanceof AbstractJob);
		Object compositeListener = ReflectionTestUtils.getField(job, "listener");
		Object composite = ReflectionTestUtils.getField(compositeListener, "listeners");
		List<JobExecutionListener> list = (List<JobExecutionListener>) ReflectionTestUtils.getField(composite, "list");

		List<Object> listeners = new ArrayList<Object>();
		for (Object listener : list) {
			while (listener instanceof Advised) {
				listener = ((Advised) listener).getTargetSource().getTarget();
			}
			listeners.add(listener);
		}
		return listeners;
	}
}
