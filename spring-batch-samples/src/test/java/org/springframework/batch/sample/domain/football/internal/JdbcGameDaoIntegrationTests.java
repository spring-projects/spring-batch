/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.domain.football.internal;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.football.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lucas Ward
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-source-context.xml"})
public class JdbcGameDaoIntegrationTests {

	private JdbcGameDao gameDao;

	private Game game = new Game();

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
		gameDao = new JdbcGameDao();
		gameDao.setDataSource(dataSource);
		gameDao.afterPropertiesSet();
	}

	@Before
	public void onSetUpBeforeTransaction() throws Exception {

		game.setId("XXXXX00");
		game.setYear(1996);
		game.setTeam("mia");
		game.setWeek(10);
		game.setOpponent("nwe");
		game.setAttempts(0);
		game.setCompletes(0);
		game.setPassingYards(0);
		game.setPassingTd(0);
		game.setInterceptions(0);
		game.setRushes(29);
		game.setRushYards(109);
		game.setReceptions(1);
		game.setReceptionYards(16);
		game.setTotalTd(2);

	}

	@Transactional @Test
	public void testWrite() {

		gameDao.write(Collections.singletonList(game));

		Game tempGame = simpleJdbcTemplate.queryForObject("SELECT * FROM GAMES where PLAYER_ID=? AND YEAR_NO=?",
				new GameRowMapper(), "XXXXX00 ", game.getYear());
		assertEquals(tempGame, game);
	}

	private static class GameRowMapper implements ParameterizedRowMapper<Game> {

		public Game mapRow(ResultSet rs, int arg1) throws SQLException {

			if (rs == null) {
				return null;
			}

			Game game = new Game();
			game.setId(rs.getString("PLAYER_ID").trim());
			game.setYear(rs.getInt("year_no"));
			game.setTeam(rs.getString("team"));
			game.setWeek(rs.getInt("week"));
			game.setOpponent(rs.getString("opponent"));
			game.setCompletes(rs.getInt("completes"));
			game.setAttempts(rs.getInt("attempts"));
			game.setPassingYards(rs.getInt("passing_Yards"));
			game.setPassingTd(rs.getInt("passing_Td"));
			game.setInterceptions(rs.getInt("interceptions"));
			game.setRushes(rs.getInt("rushes"));
			game.setRushYards(rs.getInt("rush_Yards"));
			game.setReceptions(rs.getInt("receptions"));
			game.setReceptionYards(rs.getInt("receptions_Yards"));
			game.setTotalTd(rs.getInt("total_Td"));

			return game;
		}
	}
}
