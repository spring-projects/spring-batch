/*
 * Copyright 2006-2023 the original author or authors.
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

import org.springframework.batch.sample.domain.football.Player;
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
class JdbcPlayerDaoIntegrationTests {

	private JdbcPlayerDao playerDao;

	private Player player;

	private static final String GET_PLAYER = "SELECT * from PLAYERS";

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void init(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		playerDao = new JdbcPlayerDao();
		playerDao.setDataSource(dataSource);

		player = new Player();
		player.setId("AKFJDL00");
		player.setFirstName("John");
		player.setLastName("Doe");
		player.setPosition("QB");
		player.setBirthYear(1975);
		player.setDebutYear(1998);
	}

	@BeforeEach
	void onSetUpInTransaction() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "PLAYERS");
	}

	@Test
	@Transactional
	void testSavePlayer() {
		playerDao.savePlayer(player);
		jdbcTemplate.query(GET_PLAYER, rs -> {
			assertEquals(rs.getString("PLAYER_ID"), "AKFJDL00");
			assertEquals(rs.getString("LAST_NAME"), "Doe");
			assertEquals(rs.getString("FIRST_NAME"), "John");
			assertEquals(rs.getString("POS"), "QB");
			assertEquals(rs.getInt("YEAR_OF_BIRTH"), 1975);
			assertEquals(rs.getInt("YEAR_DRAFTED"), 1998);
		});
	}

}
