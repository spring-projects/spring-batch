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

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class JobParserTests {

	private static ConfigurableApplicationContext jobParserParentAttributeTestsCtx;

	@BeforeClass
	public static void loadAppCtx() {
		jobParserParentAttributeTestsCtx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/JobParserParentAttributeTests-context.xml");
	}

	@Test
	public void testInheritListeners() throws Exception {
		List<?> job1Listeners = getListeners("job1", jobParserParentAttributeTestsCtx);
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
		List<?> job2Listeners = getListeners("job2", jobParserParentAttributeTestsCtx);
		assertEquals(1, job2Listeners.size());
		boolean c = false;
		for (Object l : job2Listeners) {
			if (l instanceof JobExecutionListenerSupport) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	public void testStandaloneListener() throws Exception {
		List<?> jobListeners = getListeners("job3", jobParserParentAttributeTestsCtx);
		assertEquals(2, jobListeners.size());
		boolean a = false;
		boolean b = false;
		for (Object l : jobListeners) {
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

	@SuppressWarnings("unchecked")
	private List<?> getListeners(String jobName, ApplicationContext ctx) throws Exception {
		assertTrue(ctx.containsBean(jobName));
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

	@Test
	public void testJobRepositoryDefaults() throws Exception {
		ApplicationContext ctx = jobParserParentAttributeTestsCtx;

		assertTrue(getJobRepository("defaultRepoJob", ctx) instanceof SimpleJobRepository);

		assertTrue(getJobRepository("specifiedRepoJob", ctx) instanceof DummyJobRepository);

		assertTrue(getJobRepository("inheritSpecifiedRepoJob", ctx) instanceof DummyJobRepository);

		assertTrue(getJobRepository("overrideInheritedRepoJob", ctx) instanceof SimpleJobRepository);
	}

	private JobRepository getJobRepository(String jobName, ApplicationContext ctx) throws Exception {
		assertTrue(ctx.containsBean(jobName));
		Job job = (Job) ctx.getBean(jobName);
		assertTrue(job instanceof AbstractJob);
		Object jobRepository = ReflectionTestUtils.getField(job, "jobRepository");
		while (jobRepository instanceof Advised) {
			jobRepository = ((Advised) jobRepository).getTargetSource().getTarget();
		}
		assertTrue(jobRepository instanceof JobRepository);
		return (JobRepository) jobRepository;
	}

	@Test
	public void testUnreachableStep() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepTests-context.xml");
			fail("Error expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("The element [s2] is unreachable"));
		}
	}

	@Test
	public void testUnreachableStepInFlow() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepInFlowTests-context.xml");
			fail("Error expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("The element [s4] is unreachable"));
		}
	}

	@Test
	public void testNextOutOfScope() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserNextOutOfScopeTests-context.xml");
			fail("Error expected");
		}
		catch (BeanCreationException e) {
			assertTrue(e.getMessage().contains("Missing state for [StateTransition: [state=s2, pattern=*, next=s3]]"));
		}
	}

	@Test
	public void testListenerClearingJob() throws Exception {
		assertEquals(0, getListeners("listenerClearingJob", jobParserParentAttributeTestsCtx).size());
	}

}
