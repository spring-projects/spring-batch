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
package org.springframework.batch.core.partition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig(locations = "launch-context.xml")
public class RestartIntegrationTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testSimpleProperties() {
		assertNotNull(jobLauncher);
	}

	@BeforeEach
	@AfterEach
	void start() {
		ExampleItemReader.fail = false;
	}

	@Test
	void testLaunchJob() throws Exception {

		// Force failure in one of the parallel steps
		ExampleItemReader.fail = true;
		JobParameters jobParameters = new JobParametersBuilder().addString("restart", "yes").toJobParameters();

		int beforeManager = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME='step1:manager'");
		int beforePartition = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME like 'step1:partition%'");

		ExampleItemWriter.clear();
		JobExecution execution = jobLauncher.run(job, jobParameters);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		// Only 4 because the others were in the failed step execution
		assertEquals(4, ExampleItemWriter.getItems().size());

		ExampleItemWriter.clear();
		assertNotNull(jobLauncher.run(job, jobParameters));
		// Only 4 because the others were processed in the first attempt
		assertEquals(4, ExampleItemWriter.getItems().size());

		int afterManager = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME='step1:manager'");
		int afterPartition = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME like 'step1:partition%'");

		// Two attempts
		assertEquals(2, afterManager - beforeManager);
		// One failure and two successes
		assertEquals(3, afterPartition - beforePartition);

	}

}
