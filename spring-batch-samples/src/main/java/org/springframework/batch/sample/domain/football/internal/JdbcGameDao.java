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

package org.springframework.batch.sample.domain.football.internal;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.football.Game;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcGameDao extends SimpleJdbcDaoSupport implements ItemWriter<Game> {

	private SimpleJdbcInsert insertGame;

	protected void initDao() throws Exception {
		super.initDao();
		insertGame = new SimpleJdbcInsert(getDataSource()).withTableName("GAMES").usingColumns("player_id", "year_no",
				"team", "week", "opponent", " completes", "attempts", "passing_yards", "passing_td", "interceptions",
				"rushes", "rush_yards", "receptions", "receptions_yards", "total_td");
	}

	public void write(List<? extends Game> games) {

		for (Game game : games) {

			SqlParameterSource values = new MapSqlParameterSource().addValue("player_id", game.getId()).addValue(
					"year_no", game.getYear()).addValue("team", game.getTeam()).addValue("week", game.getWeek())
					.addValue("opponent", game.getOpponent()).addValue("completes", game.getCompletes()).addValue(
							"attempts", game.getAttempts()).addValue("passing_yards", game.getPassingYards()).addValue(
							"passing_td", game.getPassingTd()).addValue("interceptions", game.getInterceptions())
					.addValue("rushes", game.getRushes()).addValue("rush_yards", game.getRushYards()).addValue(
							"receptions", game.getReceptions()).addValue("receptions_yards", game.getReceptionYards())
					.addValue("total_td", game.getTotalTd());
			this.insertGame.execute(values);

		}

	}

}
