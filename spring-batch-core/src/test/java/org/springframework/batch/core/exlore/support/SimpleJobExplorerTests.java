/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.core.exlore.support;

import static org.easymock.EasyMock.createMock;

import java.util.Collections;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;

/**
 * Test {@link SimpleJobExplorer}. 
 * 
 * @author Dave Syer
 * 
 */
public class SimpleJobExplorerTests extends TestCase {

	SimpleJobExplorer jobExplorer;

	JobExecutionDao jobExecutionDao;
	
	JobInstanceDao jobInstanceDao;

	JobInstance jobInstance = new JobInstance(111L, new JobParameters(), "job");
	
	JobExecution jobExecution = new JobExecution(jobInstance, 123L);

	public void setUp() throws Exception {

		jobExecutionDao = createMock(JobExecutionDao.class);
		jobInstanceDao = createMock(JobInstanceDao.class);

		jobExplorer = new SimpleJobExplorer(jobInstanceDao, jobExecutionDao);

	}

	@Test
	public void testGetJobExecution() throws Exception {
		jobExecutionDao.getJobExecution(123L);
		EasyMock.expectLastCall().andReturn(jobExecution);
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		jobExplorer.getJobExecution(123L);
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testFindRunningJobExecutions() throws Exception {
		jobExecutionDao.findRunningJobExecutions("job");
		EasyMock.expectLastCall().andReturn(Collections.singleton(jobExecution));
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		jobExplorer.findRunningJobExecutions("job");
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testFindJobExecutions() throws Exception {
		jobExecutionDao.findJobExecutions(jobInstance);
		EasyMock.expectLastCall().andReturn(Collections.singletonList(jobExecution));
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		jobExplorer.findJobExecutions(jobInstance);
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testGetJobInstance() throws Exception {
		jobInstanceDao.getJobInstance(111L);
		EasyMock.expectLastCall().andReturn(jobInstance);
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		jobExplorer.getJobInstance(111L);
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testGetLastJobInstances() throws Exception {
		jobInstanceDao.getLastJobInstances("foo", 1);
		EasyMock.expectLastCall().andReturn(Collections.singletonList(jobInstance));
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		jobExplorer.getLastJobInstances("foo", 1);
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testIsJobInstanceFalse() throws Exception {
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		EasyMock.expectLastCall().andReturn(null);
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		assertFalse(jobExplorer.isJobInstanceExists("foo", new JobParameters()));
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

	@Test
	public void testIsJobInstanceTrue() throws Exception {
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		EasyMock.expectLastCall().andReturn(jobInstance);
		EasyMock.replay(jobExecutionDao, jobInstanceDao);
		assertTrue(jobExplorer.isJobInstanceExists("foo", new JobParameters()));
		EasyMock.verify(jobExecutionDao, jobInstanceDao);
	}

}
