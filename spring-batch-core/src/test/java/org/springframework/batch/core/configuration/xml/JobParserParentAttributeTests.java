/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@SpringJUnitConfig
class JobParserParentAttributeTests {

	@Autowired
	@Qualifier("listenerClearingJob")
	private Job listenerClearingJob;

	@Autowired
	@Qualifier("defaultRepoJob")
	private Job defaultRepoJob;

	@Autowired
	@Qualifier("specifiedRepoJob")
	private Job specifiedRepoJob;

	@Autowired
	@Qualifier("inheritSpecifiedRepoJob")
	private Job inheritSpecifiedRepoJob;

	@Autowired
	@Qualifier("overrideInheritedRepoJob")
	private Job overrideInheritedRepoJob;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Test
	void testInheritListeners() throws Exception {
		List<?> job1Listeners = getListeners(job1);
		assertEquals(2, job1Listeners.size());
		boolean a = false;
		boolean b = false;
		for (Object l : job1Listeners) {
			if (l instanceof DummyAnnotationJobExecutionListener) {
				a = true;
			}
			else if (l instanceof DummyJobExecutionListener) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@Test
	void testInheritListeners_NoMerge() throws Exception {
		List<?> job2Listeners = getListeners(job2);
		assertEquals(1, job2Listeners.size());
		boolean c = false;
		for (Object l : job2Listeners) {
			if (l instanceof DummyJobExecutionListener) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	void testStandaloneListener() throws Exception {
		List<?> jobListeners = getListeners(job3);
		assertEquals(2, jobListeners.size());
		boolean a = false;
		boolean b = false;
		for (Object l : jobListeners) {
			if (l instanceof DummyAnnotationJobExecutionListener) {
				a = true;
			}
			else if (l instanceof DummyJobExecutionListener) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@Test
	void testJobRepositoryDefaults() throws Exception {
		assertTrue(getJobRepository(defaultRepoJob) instanceof SimpleJobRepository);
		assertTrue(getJobRepository(specifiedRepoJob) instanceof DummyJobRepository);
		assertTrue(getJobRepository(inheritSpecifiedRepoJob) instanceof DummyJobRepository);
		assertTrue(getJobRepository(overrideInheritedRepoJob) instanceof SimpleJobRepository);
	}

	@Test
	void testListenerClearingJob() throws Exception {
		assertEquals(0, getListeners(listenerClearingJob).size());
	}

	private JobRepository getJobRepository(Job job) throws Exception {
		assertTrue(job instanceof AbstractJob);
		Object jobRepository = ReflectionTestUtils.getField(job, "jobRepository");
		while (jobRepository instanceof Advised) {
			jobRepository = ((Advised) jobRepository).getTargetSource().getTarget();
		}
		assertTrue(jobRepository instanceof JobRepository);
		return (JobRepository) jobRepository;
	}

	@SuppressWarnings("unchecked")
	private List<?> getListeners(Job job) throws Exception {

		assertTrue(job instanceof AbstractJob);
		Object compositeListener = ReflectionTestUtils.getField(job, "listener");
		Object composite = ReflectionTestUtils.getField(compositeListener, "listeners");
		List<JobExecutionListener> list = (List<JobExecutionListener>) ReflectionTestUtils.getField(composite, "list");

		List<Object> listeners = new ArrayList<>();
		for (Object listener : list) {
			while (listener instanceof Advised) {
				listener = ((Advised) listener).getTargetSource().getTarget();
			}
			listeners.add(listener);
		}
		return listeners;

	}

}
