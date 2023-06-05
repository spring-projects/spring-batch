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

package org.springframework.batch.sample.iosample;

import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * @since 2.0
 */
@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/ioSampleJob.xml", "/jobs/iosample/jdbcPaging.xml" })
class TwoJobInstancesPagingFunctionalTests {

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private Job job;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testLaunchJobTwice() throws Exception {
		int first = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "CUSTOMER", "credit>1000");
		JobExecution jobExecution = launcher.run(this.job, getJobParameters(1000.));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(first, jobExecution.getStepExecutions().iterator().next().getWriteCount());
		int second = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "CUSTOMER", "credit>1000000");
		assertNotSame(first, second, "The number of records above the threshold did not change");
		jobExecution = launcher.run(this.job, getJobParameters(1000000.));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(second, jobExecution.getStepExecutions().iterator().next().getWriteCount());
	}

	protected JobParameters getJobParameters(double amount) {
		return new JobParametersBuilder().addLong("timestamp", new Date().getTime())
			.addDouble("credit", amount)
			.toJobParameters();
	}

}
