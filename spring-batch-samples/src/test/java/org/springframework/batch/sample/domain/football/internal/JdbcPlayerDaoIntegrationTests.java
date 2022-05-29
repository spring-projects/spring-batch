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
package org.springframework.batch.sample.domain.football.internal;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.football.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/data-source-context.xml" })
public class JdbcPlayerDaoIntegrationTests {

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

	@Before
	public void onSetUpInTransaction() throws Exception {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "PLAYERS");
	}

	@Test
	@Transactional
	public void testSavePlayer() {
		playerDao.savePlayer(player);
		jdbcTemplate.query(GET_PLAYER, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				assertEquals(rs.getString("PLAYER_ID"), "AKFJDL00");
				assertEquals(rs.getString("LAST_NAME"), "Doe");
				assertEquals(rs.getString("FIRST_NAME"), "John");
				assertEquals(rs.getString("POS"), "QB");
				assertEquals(rs.getInt("YEAR_OF_BIRTH"), 1975);
				assertEquals(rs.getInt("YEAR_DRAFTED"), 1998);
			}
		});
	}

}
