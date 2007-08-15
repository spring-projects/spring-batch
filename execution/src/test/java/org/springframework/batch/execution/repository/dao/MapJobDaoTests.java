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

package org.springframework.batch.execution.repository.dao;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

public class MapJobDaoTests extends TestCase {

	MapJobDao dao = new MapJobDao();
	
	protected void setUp() throws Exception {
		MapJobDao.clear();
	}
	
	public void testCreateAndRetrieveSingle() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		List result = dao.findJobs(new SimpleJobIdentifier("foo"));
		assertTrue(result.contains(job));
	}
	
	public void testCreateAndRetrieveMultiple() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		job = dao.createJob(new SimpleJobIdentifier("bar"));
		List result = dao.findJobs(new SimpleJobIdentifier("bar"));
		assertEquals(1, result.size());
		assertTrue(result.contains(job));		
	}
	
	public void testNoExecutionsForNewJob() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		assertEquals(0, dao.getJobExecutionCount(job.getId()));
	}

	public void testSaveExecutionUpdatesId() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		JobExecution execution = new JobExecution(job.getId());
		assertNull(execution.getId());
		dao.save(execution);
		assertNotNull(execution.getId());
	}
	public void testCorrectExecutionCountForExistingJob() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		dao.save(new JobExecution(job.getId()));
		assertEquals(1, dao.getJobExecutionCount(job.getId()));
	}

	public void testMultipleExecutionsPerExisting() throws Exception {
		JobInstance job = dao.createJob(new SimpleJobIdentifier("foo"));
		dao.save(new JobExecution(job.getId()));
		Thread.sleep(50L); // Hack, hack, hackety, hack - job executions are not unique if created too close together!
		dao.save(new JobExecution(job.getId()));
		assertEquals(2, dao.getJobExecutionCount(job.getId()));
	}
}
