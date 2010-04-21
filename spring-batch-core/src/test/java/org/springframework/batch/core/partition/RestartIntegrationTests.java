/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.partition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * 
 */
@ContextConfiguration(locations = "launch-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class RestartIntegrationTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	private SimpleJdbcTemplate jdbcTemplate;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Test
	public void testSimpleProperties() throws Exception {
		assertNotNull(jobLauncher);
	}

	@Before
	@After
	public void start() {
		ExampleItemReader.fail = false;
	}

	@Test
	public void testLaunchJob() throws Exception {

		// Force failure in one of the parallel steps
		ExampleItemReader.fail = true;
		JobParameters jobParameters = new JobParametersBuilder().addString("restart", "yes").toJobParameters();

		int beforeMaster = jdbcTemplate.queryForInt("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME='step1:master'");
		int beforePartition = jdbcTemplate.queryForInt("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME like 'step1:partition%'");

		ExampleItemWriter.clear();
		JobExecution execution = jobLauncher.run(job, jobParameters);
		assertEquals(BatchStatus.FAILED,execution.getStatus());
		// Only 4 because the others were in the failed step execution
		assertEquals(4, ExampleItemWriter.getItems().size());

		ExampleItemWriter.clear();
		assertNotNull(jobLauncher.run(job, jobParameters));
		// Only 4 because the others were processed in the first attempt
		assertEquals(4, ExampleItemWriter.getItems().size());

		int afterMaster = jdbcTemplate.queryForInt("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME='step1:master'");
		int afterPartition = jdbcTemplate.queryForInt("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME like 'step1:partition%'");

		// Two attempts
		assertEquals(2, afterMaster-beforeMaster);
		// One failure and two successes
		assertEquals(3, afterPartition-beforePartition);

	}

}
