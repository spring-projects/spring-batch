/*
 * Copyright 2006-2013 the original author or authors.
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

package org.springframework.batch.core.explore.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;

/**
 * Test {@link SimpleJobExplorer}.
 *
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 *
 */
public class SimpleJobExplorerTests {

	private SimpleJobExplorer jobExplorer;

	private JobExecutionDao jobExecutionDao;

	private JobInstanceDao jobInstanceDao;

	private StepExecutionDao stepExecutionDao;

	private JobInstance jobInstance = new JobInstance(111L, "job");

	private ExecutionContextDao ecDao;

	private JobExecution jobExecution = new JobExecution(jobInstance, 1234L, new JobParameters(), null);

	@Before
	public void setUp() throws Exception {

		jobExecutionDao = mock(JobExecutionDao.class);
		jobInstanceDao = mock(JobInstanceDao.class);
		stepExecutionDao = mock(StepExecutionDao.class);
		ecDao = mock(ExecutionContextDao.class);

		jobExplorer = new SimpleJobExplorer(jobInstanceDao, jobExecutionDao,
				stepExecutionDao, ecDao);

	}

	@Test
	public void testGetJobExecution() throws Exception {
		when(jobExecutionDao.getJobExecution(123L)).thenReturn(jobExecution);
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(
				jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		jobExplorer.getJobExecution(123L);
	}

	@Test
	public void testMissingGetJobExecution() throws Exception {
		when(jobExecutionDao.getJobExecution(123L)).thenReturn(null);
		assertNull(jobExplorer.getJobExecution(123L));
	}

	@Test
	public void testGetStepExecution() throws Exception {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(jobExecution);
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(jobInstance);
		StepExecution stepExecution = jobExecution.createStepExecution("foo");
		when(stepExecutionDao.getStepExecution(jobExecution, 123L))
		.thenReturn(stepExecution);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		stepExecution = jobExplorer.getStepExecution(jobExecution.getId(), 123L);

		assertEquals(jobInstance,
				stepExecution.getJobExecution().getJobInstance());

		verify(jobInstanceDao).getJobInstance(jobExecution);
	}

	@Test
	public void testGetStepExecutionMissing() throws Exception {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(jobExecution);
		when(stepExecutionDao.getStepExecution(jobExecution, 123L))
		.thenReturn(null);
		assertNull(jobExplorer.getStepExecution(jobExecution.getId(), 123L));
	}

	@Test
	public void testGetStepExecutionMissingJobExecution() throws Exception {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(null);
		assertNull(jobExplorer.getStepExecution(jobExecution.getId(), 123L));
	}

	@Test
	public void testFindRunningJobExecutions() throws Exception {
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		when(jobExecutionDao.findRunningJobExecutions("job")).thenReturn(
				Collections.singleton(jobExecution));
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(
				jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		when(ecDao.getExecutionContext(jobExecution)).thenReturn(null);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		jobExplorer.findRunningJobExecutions("job");
	}

	@Test
	public void testFindJobExecutions() throws Exception {
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(
				Collections.singletonList(jobExecution));
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(
				jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		when(ecDao.getExecutionContext(jobExecution)).thenReturn(null);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		jobExplorer.getJobExecutions(jobInstance);
	}

	@Test
	public void testGetJobInstance() throws Exception {
		jobInstanceDao.getJobInstance(111L);
		jobExplorer.getJobInstance(111L);
	}

	@Test
	public void testGetLastJobInstances() throws Exception {
		jobInstanceDao.getJobInstances("foo", 0, 1);
		jobExplorer.getJobInstances("foo", 0, 1);
	}

	@Test
	public void testGetJobNames() throws Exception {
		jobInstanceDao.getJobNames();
		jobExplorer.getJobNames();
	}

	@Test
	public void testGetJobInstanceCount() throws Exception {
		when(jobInstanceDao.getJobInstanceCount("myJob")).thenReturn(4);

		assertEquals(4, jobExplorer.getJobInstanceCount("myJob"));
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetJobInstanceCountException() throws Exception {
		when(jobInstanceDao.getJobInstanceCount("throwException")).thenThrow(new NoSuchJobException("expected"));

		jobExplorer.getJobInstanceCount("throwException");
	}
}
