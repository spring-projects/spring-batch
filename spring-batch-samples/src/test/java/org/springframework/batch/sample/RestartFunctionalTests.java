/*
 * Copyright 2006-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Simple restart scenario.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/restartSample.xml",
		"/job-runner-context.xml" })
public class RestartFunctionalTests {
	private JdbcOperations jdbcTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeTransaction
	public void onTearDown() throws Exception {
        jdbcTemplate.update("DELETE FROM TRADE");
	}

	/**
	 * Job fails on first run, because the module throws exception after
	 * processing more than half of the input. On the second run, the job should
	 * finish successfully, because it continues execution where the previous
	 * run stopped (module throws exception after fixed number of processed
	 * records).
	 *
	 * @throws Exception
	 */
	@Test
	public void testLaunchJob() throws Exception {
		int before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TRADE", Integer.class);

		JobExecution jobExecution = runJobForRestartTest();
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

		Throwable ex = jobExecution.getAllFailureExceptions().get(0);
		if (ex.getMessage().toLowerCase().indexOf("planned") < 0) {
			if (ex instanceof Exception) {
				throw (Exception) ex;
			}
			throw new RuntimeException(ex);
		}

		int medium = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TRADE", Integer.class);
		// assert based on commit interval = 2
		assertEquals(before + 2, medium);

		jobExecution = runJobForRestartTest();
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		int after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TRADE", Integer.class);

		assertEquals(before + 5, after);
	}

	// load the application context and launch the job
	private JobExecution runJobForRestartTest() throws Exception {
		return jobLauncherTestUtils
				.launchJob(new DefaultJobParametersConverter()
						.getJobParameters(PropertiesConverter
								.stringToProperties("run.id(long)=1,parameter=true,run.date=20070122,input.file=classpath:data/fixedLengthImportJob/input/20070122.teststream.ImportTradeDataStep.txt")));
	}
}
