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
package org.springframework.batch.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.sample.SampleTasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * This is an abstract test class.
 *
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/job-runner-context.xml" })
abstract class AbstractSampleJobTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	@Qualifier("tasklet2")
	private SampleTasklet tasklet2;

	@BeforeEach
	void setUp() {
		this.jdbcTemplate.update("create table TESTS (ID integer, NAME varchar(40))");
		tasklet2.jobContextEntryFound = false;
	}

	@AfterEach
	void tearDown() {
		JdbcTestUtils.dropTables(this.jdbcTemplate, "TESTS");
	}

	@Test
	void testJob(@Autowired Job job) throws Exception {
		this.jobLauncherTestUtils.setJob(job);
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchJob().getStatus());
		this.verifyTasklet(1);
		this.verifyTasklet(2);
	}

	@Test
	void testNonExistentStep() {
		assertThrows(IllegalStateException.class, () -> jobLauncherTestUtils.launchStep("nonExistent"));
	}

	@Test
	void testStep1Execution() {
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step1").getStatus());
		this.verifyTasklet(1);
	}

	@Test
	void testStep2Execution() {
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step2").getStatus());
		this.verifyTasklet(2);
	}

	@RepeatedTest(10)
	void testStep3Execution() {
		// logging only, may complete in < 1ms (repeat so that it's likely to for at least
		// one of those times)
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step3").getStatus());
	}

	@Test
	void testStepLaunchJobContextEntry() {
		ExecutionContext jobContext = new ExecutionContext();
		jobContext.put("key1", "value1");
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step2", jobContext).getStatus());
		this.verifyTasklet(2);
		assertTrue(tasklet2.jobContextEntryFound);
	}

	private void verifyTasklet(int id) {
		assertEquals(id,
				jdbcTemplate
					.queryForObject("SELECT ID from TESTS where NAME = 'SampleTasklet" + id + "'", Integer.class)
					.intValue());
	}

}
