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
package org.springframework.batch.sample.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.Game;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * @author Lucas Ward
 * 
 */
public class JdbcGameDaoIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	private JdbcGameDao gameDao;

	private Game game = new Game();

	protected String[] getConfigLocations() {
		return new String[] { "data-source-context.xml" };
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();

		gameDao = new JdbcGameDao();
		gameDao.setJdbcTemplate(getJdbcTemplate());

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

	public void testWrite() {

		gameDao.write(game);

		Game tempGame = (Game) getJdbcTemplate().queryForObject("SELECT * FROM GAMES where PLAYER_ID=? AND YEAR_NO=?",
				new Object[] { game.getId(), new Integer(game.getYear()) }, new GameRowMapper());
		assertEquals(tempGame, game);
	}

	public static class GameRowMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int arg1) throws SQLException {

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
