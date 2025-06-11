/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.core.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.jdbc.JdbcJobExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

@SpringJUnitConfig
public class JobLauncherIntegrationTests {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testLaunchAndRelaunch() throws Exception {

		int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");

		JobExecution jobExecution = launch(true, 0);
		launch(false, jobExecution.getId());
		launch(false, jobExecution.getId());

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_INSTANCE");
		assertEquals(before + 1, after);

	}

	private JobExecution launch(boolean start, long jobExecutionId) throws Exception {

		if (start) {

			JobParametersBuilder builder = new JobParametersBuilder();
			builder.addString("name", "foo");
			JobParameters jobParameters = builder.toJobParameters();

			return jobLauncher.run(job, jobParameters);

		}
		else {

			JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
			dao.setJdbcTemplate(jdbcTemplate);
			JobExecution execution = dao.getJobExecution(jobExecutionId);

			if (execution != null) {
				return jobLauncher.run(job, execution.getJobParameters());
			}

			return null;

		}

	}

}
