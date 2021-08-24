/*
 * Copyright 2006-2021 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
@ContextConfiguration(locations="launch-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class VanillaIntegrationTests {

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
	public void testSimpleProperties() throws Exception {
		assertNotNull(jobLauncher);
	}

	@Test
	public void testLaunchJob() throws Exception {
		int beforeManager = jdbcTemplate.queryForObject("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME='step1:manager'", Integer.class);
		int beforePartition = jdbcTemplate.queryForObject("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME like 'step1:partition%'", Integer.class);
		assertNotNull(jobLauncher.run(job, new JobParameters()));
		int afterManager = jdbcTemplate.queryForObject("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME='step1:manager'", Integer.class);
		int afterPartition = jdbcTemplate.queryForObject("SELECT COUNT(*) from BATCH_STEP_EXECUTION where STEP_NAME like 'step1:partition%'", Integer.class);
		assertEquals(1, afterManager-beforeManager);
		// Should be same as grid size in step splitter
		assertEquals(2, afterPartition-beforePartition);
	}

}
