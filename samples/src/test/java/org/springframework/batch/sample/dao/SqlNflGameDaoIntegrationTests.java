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

import org.springframework.batch.sample.domain.NflGame;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * @author Lucas Ward
 *
 */
public class SqlNflGameDaoIntegrationTests extends
	AbstractTransactionalDataSourceSpringContextTests {

	SqlNflGameDao gameDao;
	NflGame game = new NflGame();
	
	protected String[] getConfigLocations() {
		return new String[]{"data-source-context.xml"};
	}
	
	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		gameDao = new SqlNflGameDao();
		gameDao.setJdbcTemplate(getJdbcTemplate());
		
		game.setId("AbduKa00");
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
	
	public void testWrite(){
		
		gameDao.write(game);
		
		NflGame tempGame = (NflGame)getJdbcTemplate().queryForObject("SELECT * FROM GAMES", new NflGameRowMapper());
		assertEquals(tempGame, game);
	}
	
	public class NflGameRowMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int arg1) throws SQLException{
			
			if(rs == null){
				return null;
			}
			
			NflGame nflGame = new NflGame();
			nflGame.setId(rs.getString("PLAYER_ID"));
			nflGame.setYear(rs.getInt("year"));
			nflGame.setTeam(rs.getString("team"));
			nflGame.setWeek(rs.getInt("week"));
			nflGame.setOpponent(rs.getString("opponent"));
			nflGame.setCompletes(rs.getInt("completes"));
			nflGame.setAttempts(rs.getInt("attempts"));
			nflGame.setPassingYards(rs.getInt("passing_Yards"));
			nflGame.setPassingTd(rs.getInt("passing_Td"));
			nflGame.setInterceptions(rs.getInt("interceptions"));
			nflGame.setRushes(rs.getInt("rushes"));
			nflGame.setRushYards(rs.getInt("rush_Yards"));
			nflGame.setReceptions(rs.getInt("receptions"));
			nflGame.setReceptionYards(rs.getInt("receptions_Yards"));
			nflGame.setTotalTd(rs.getInt("total_Td"));
			
			return nflGame;
		}
	}
}
