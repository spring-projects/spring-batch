/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class MapJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

	@Test
	public void testWildcardPrefix() {
		MapJobInstanceDao mapJobInstanceDao = new MapJobInstanceDao();
		mapJobInstanceDao.createJobInstance("testJob", new JobParameters());
		mapJobInstanceDao.createJobInstance("Jobtest", new JobParameters());
		List<JobInstance> jobInstances = mapJobInstanceDao.findJobInstancesByName("*Job", 0, 2);
		assertTrue("Invalid matching job instances found, expected 1, got: " + jobInstances.size(), jobInstances.size() == 1);
	}

	@Test
	public void testWildcardSuffix() {
		MapJobInstanceDao mapJobInstanceDao = new MapJobInstanceDao();
		mapJobInstanceDao.createJobInstance("testJob", new JobParameters());
		mapJobInstanceDao.createJobInstance("Jobtest", new JobParameters());
		List<JobInstance> jobInstances = mapJobInstanceDao.findJobInstancesByName("Job*", 0, 2);
		assertTrue("No matching job instances found, expected 1, got: " + jobInstances.size(), jobInstances.size() == 1);
	}

	@Test
	public void testWildcardRange() {
		MapJobInstanceDao mapJobInstanceDao = new MapJobInstanceDao();
		mapJobInstanceDao.createJobInstance("testJob", new JobParameters());
		mapJobInstanceDao.createJobInstance("Jobtest", new JobParameters());
		List<JobInstance> jobInstances = mapJobInstanceDao.findJobInstancesByName("*Job*", 0, 2);
		assertTrue("No matching job instances found, expected 2, got: " + jobInstances.size(), jobInstances.size() == 2);
	}
}
