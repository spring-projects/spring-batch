/*
 * Copyright 2006-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
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
public class VanillaIntegrationTests {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testSimpleProperties() {
		assertNotNull(jobOperator);
	}

	@Test
	void testLaunchJob() throws Exception {
		int beforeManager = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME='step1:manager'");
		int beforePartition = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME like 'step1:partition%'");
		assertNotNull(jobOperator.start(job, new JobParameters()));
		int afterManager = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME='step1:manager'");
		int afterPartition = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "BATCH_STEP_EXECUTION",
				"STEP_NAME like 'step1:partition%'");
		assertEquals(1, afterManager - beforeManager);
		// Should be same as grid size in step splitter
		assertEquals(2, afterPartition - beforePartition);
	}

}
