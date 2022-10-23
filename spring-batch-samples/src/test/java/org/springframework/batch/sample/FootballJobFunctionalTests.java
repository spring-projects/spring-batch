/*
 * Copyright 2007-2022 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/footballJob.xml", "/job-runner-context.xml" })
class FootballJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testLaunchJob() throws Exception {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "PLAYERS", "GAMES", "PLAYER_SUMMARY");

		jobLauncherTestUtils.launchJob();

		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "PLAYER_SUMMARY");
		assertTrue(count > 0);
	}

}
