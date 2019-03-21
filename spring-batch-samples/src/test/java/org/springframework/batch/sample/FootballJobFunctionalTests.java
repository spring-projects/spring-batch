/*
 * Copyright 2007-2014 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/footballJob.xml", "/job-runner-context.xml" })
public class FootballJobFunctionalTests {
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	private JdbcOperations jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void testLaunchJob() throws Exception {
        jdbcTemplate.update("DELETE FROM PLAYERS");
        jdbcTemplate.update("DELETE FROM GAMES");
        jdbcTemplate.update("DELETE FROM PLAYER_SUMMARY");

		jobLauncherTestUtils.launchJob();

		int count = jdbcTemplate.queryForObject("SELECT COUNT(*) from PLAYER_SUMMARY", Integer.class);
		assertTrue(count > 0);
	}
}
