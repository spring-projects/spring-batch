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
package org.springframework.batch.sample.domain.football.internal;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.sample.domain.football.PlayerSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig(locations = { "/data-source-context.xml" })
class JdbcPlayerSummaryDaoIntegrationTests {

	private JdbcPlayerSummaryDao playerSummaryDao;

	private PlayerSummary summary;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void init(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		playerSummaryDao = new JdbcPlayerSummaryDao();
		playerSummaryDao.setDataSource(dataSource);

		summary = new PlayerSummary();
		summary.setId("AikmTr00");
		summary.setYear(1997);
		summary.setCompletes(294);
		summary.setAttempts(517);
		summary.setPassingYards(3283);
		summary.setPassingTd(19);
		summary.setInterceptions(12);
		summary.setRushes(25);
		summary.setRushYards(79);
		summary.setReceptions(0);
		summary.setReceptionYards(0);
		summary.setTotalTd(0);
	}

	@BeforeEach
	void onSetUpInTransaction() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "PLAYER_SUMMARY");
	}

	@Test
	@Transactional
	void testWrite() {
		playerSummaryDao.write(Chunk.of(summary));

		PlayerSummary testSummary = jdbcTemplate.queryForObject("SELECT * FROM PLAYER_SUMMARY",
				new PlayerSummaryMapper());

		assertEquals(summary, testSummary);
	}

}
