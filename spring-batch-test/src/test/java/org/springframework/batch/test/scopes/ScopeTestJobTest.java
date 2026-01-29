/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.test.scopes;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class will specifically test the capabilities of {@link JobRepositoryTestUtils} to
 * test {@link SimpleJob}s.
 *
 * @author Dan Garrette
 * @since 2.0
 */
@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/scopeTests.xml", "/job-runner-context.xml" })
class ScopeTestJobTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testJob(@Autowired Job job) throws Exception {
		this.jobLauncherTestUtils.setJob(job);
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchJob().getStatus());

	}

}
