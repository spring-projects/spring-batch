/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.test.AbstractIntegrationTests;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/footballSkipJob.xml" })
public class FootballJobSkipIntegrationTests extends AbstractIntegrationTests {

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	private DatabaseType databaseType;

	@Autowired
	public void setDataSource(DataSource dataSource) throws Exception {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		databaseType = DatabaseType.fromMetaData(dataSource);
	}

	@Test
	public void testLaunchJob() throws Exception {
		try {
			if (databaseType == DatabaseType.POSTGRES || databaseType == DatabaseType.ORACLE) {
				// Extra special test for these platforms (would have failed
				// the job with UNKNOWN status in Batch 2.0):
                jdbcTemplate.update("SET CONSTRAINTS ALL DEFERRED");
			}
		}
		catch (Exception e) {
			// Ignore (wrong platform)
		}
		JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().addLong("skip.limit", 0L)
				.toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		for (StepExecution stepExecution : execution.getStepExecutions()) {
			logger.info("Processed: " + stepExecution);
		}
		// They all skip on the second execution because of a primary key
		// violation
		long retryLimit = 2L;
		execution = jobLauncher.run(job,
				new JobParametersBuilder().addLong("skip.limit", 100000L).addLong("retry.limit", retryLimit)
						.toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		for (StepExecution stepExecution : execution.getStepExecutions()) {
			logger.info("Processed: " + stepExecution);
			if (stepExecution.getStepName().equals("playerload")) {
				// The effect of the retries is to increase the number of
				// rollbacks
				int commitInterval = stepExecution.getReadCount() / (stepExecution.getCommitCount() - 1);
				// Account for the extra empty commit if the read count is
				// commensurate with the commit interval
				int effectiveCommitCount = stepExecution.getReadCount() % commitInterval == 0 ? stepExecution
						.getCommitCount() - 1 : stepExecution.getCommitCount();
				long expectedRollbacks = Math.max(1, retryLimit) * effectiveCommitCount + stepExecution.getReadCount();
				assertEquals(expectedRollbacks, stepExecution.getRollbackCount());
				assertEquals(stepExecution.getReadCount(), stepExecution.getWriteSkipCount());
			}
		}

	}

}
