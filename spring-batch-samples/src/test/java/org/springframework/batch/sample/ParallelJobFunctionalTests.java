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

package org.springframework.batch.sample;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/parallelJob.xml", "/job-runner-context.xml" })
class ParallelJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testLaunchJob() throws Exception {
		int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STAGING");
		JobExecution execution = jobLauncherTestUtils.launchJob();
		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STAGING");
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(after - before, execution.getStepExecutions().iterator().next().getReadCount());
	}

}
