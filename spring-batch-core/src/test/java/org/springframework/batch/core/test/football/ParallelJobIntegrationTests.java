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

package org.springframework.batch.core.test.football;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
 *
 */
@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/parallelJob.xml" })
public class ParallelJobIntegrationTests {

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobLauncher jobLauncher;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Job job;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void clear() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "PLAYER_SUMMARY", "GAMES", "PLAYERS");
	}

	@Test
	void testLaunchJob() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		for (StepExecution stepExecution : execution.getStepExecutions()) {
			logger.info("Processed: " + stepExecution);
		}
	}

}
