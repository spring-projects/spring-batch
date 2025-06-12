/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.samples.jobstep;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sample using a step to launch a job.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */

@SpringJUnitConfig(locations = { "classpath:/org/springframework/batch/samples/jobstep/job/jobStepSample.xml",
		"classpath:/simple-job-launcher-context.xml" })
class JobStepFunctionalTests {

	@Autowired
	private Job jobStepJob;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testJobLaunch() throws Exception {
		jobLauncherTestUtils.setJob(jobStepJob);
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");

		JobParameters jobParameters = new JobParametersBuilder()
			.addString("input.file", "org/springframework/batch/samples/jobstep/data/ImportTradeDataStep.txt")
			.toJobParameters();
		jobLauncherTestUtils.launchJob(jobParameters);

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
		assertEquals(5, after);
	}

}
