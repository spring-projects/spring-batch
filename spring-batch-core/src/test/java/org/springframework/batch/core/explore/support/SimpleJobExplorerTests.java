/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.explore.support.SimpleJobExplorer;

/**
 * Test {@link SimpleJobExplorer}.
 *
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
class SimpleJobExplorerTests {

	private SimpleJobExplorer jobExplorer;

	private JobExecutionDao jobExecutionDao;

	private JobInstanceDao jobInstanceDao;

	private StepExecutionDao stepExecutionDao;

	private final JobInstance jobInstance = new JobInstance(111L, "job");

	private ExecutionContextDao ecDao;

	private final JobExecution jobExecution = new JobExecution(jobInstance, 1234L, new JobParameters());

	@BeforeEach
	void setUp() {

		jobExecutionDao = mock();
		jobInstanceDao = mock();
		stepExecutionDao = mock();
		ecDao = mock();

		jobExplorer = new SimpleJobExplorer(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);

	}

	@Test
	void testGetJobExecution() {
		when(jobExecutionDao.getJobExecution(123L)).thenReturn(jobExecution);
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		jobExplorer.getJobExecution(123L);
	}

	@Test
	void testGetLastJobExecution() {
		when(jobExecutionDao.getLastJobExecution(jobInstance)).thenReturn(jobExecution);
		JobExecution lastJobExecution = jobExplorer.getLastJobExecution(jobInstance);
		assertEquals(jobExecution, lastJobExecution);
	}

	@Test
	void testMissingGetJobExecution() {
		when(jobExecutionDao.getJobExecution(123L)).thenReturn(null);
		assertNull(jobExplorer.getJobExecution(123L));
	}

	@Test
	void testGetStepExecution() {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(jobExecution);
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(jobInstance);
		StepExecution stepExecution = jobExecution.createStepExecution("foo");
		when(stepExecutionDao.getStepExecution(jobExecution, 123L)).thenReturn(stepExecution);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		stepExecution = jobExplorer.getStepExecution(jobExecution.getId(), 123L);

		assertEquals(jobInstance, stepExecution.getJobExecution().getJobInstance());

		verify(jobInstanceDao).getJobInstance(jobExecution);
	}

	@Test
	void testGetStepExecutionMissing() {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(jobExecution);
		when(stepExecutionDao.getStepExecution(jobExecution, 123L)).thenReturn(null);
		assertNull(jobExplorer.getStepExecution(jobExecution.getId(), 123L));
	}

	@Test
	void testGetStepExecutionMissingJobExecution() {
		when(jobExecutionDao.getJobExecution(jobExecution.getId())).thenReturn(null);
		assertNull(jobExplorer.getStepExecution(jobExecution.getId(), 123L));
	}

	@Test
	void testFindRunningJobExecutions() {
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		when(jobExecutionDao.findRunningJobExecutions("job")).thenReturn(Collections.singleton(jobExecution));
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		when(ecDao.getExecutionContext(jobExecution)).thenReturn(null);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		jobExplorer.findRunningJobExecutions("job");
	}

	@Test
	void testFindJobExecutions() {
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Collections.singletonList(jobExecution));
		when(jobInstanceDao.getJobInstance(jobExecution)).thenReturn(jobInstance);
		stepExecutionDao.addStepExecutions(jobExecution);
		when(ecDao.getExecutionContext(jobExecution)).thenReturn(null);
		when(ecDao.getExecutionContext(stepExecution)).thenReturn(null);
		jobExplorer.getJobExecutions(jobInstance);
	}

	@Test
	void testGetJobInstance() {
		jobInstanceDao.getJobInstance(111L);
		jobExplorer.getJobInstance(111L);
	}

	@Test
	public void testGetJobInstanceWithNameAndParameters() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();

		// when
		when(jobInstanceDao.getJobInstance(jobName, jobParameters)).thenReturn(this.jobInstance);
		JobInstance jobInstance = jobExplorer.getJobInstance(jobName, jobParameters);

		// then
		verify(jobInstanceDao).getJobInstance(jobName, jobParameters);
		assertEquals(this.jobInstance, jobInstance);
	}

	@Test
	void testGetLastJobInstances() {
		jobInstanceDao.getJobInstances("foo", 0, 1);
		jobExplorer.getJobInstances("foo", 0, 1);
	}

	@Test
	void testGetLastJobInstance() {
		when(jobInstanceDao.getLastJobInstance("foo")).thenReturn(jobInstance);
		JobInstance lastJobInstance = jobExplorer.getLastJobInstance("foo");
		assertEquals(jobInstance, lastJobInstance);
	}

	@Test
	void testGetJobNames() {
		jobInstanceDao.getJobNames();
		jobExplorer.getJobNames();
	}

	@Test
	void testGetJobInstanceCount() throws Exception {
		when(jobInstanceDao.getJobInstanceCount("myJob")).thenReturn(4L);

		assertEquals(4, jobExplorer.getJobInstanceCount("myJob"));
	}

	@Test
	void testGetJobInstanceCountException() throws Exception {
		when(jobInstanceDao.getJobInstanceCount("throwException")).thenThrow(new NoSuchJobException("expected"));
		assertThrows(NoSuchJobException.class, () -> jobExplorer.getJobInstanceCount("throwException"));
	}

}
