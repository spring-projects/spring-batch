/*
 * Copyright 2006-2012 the original author or authors.
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

package org.springframework.batch.samples.football.internal;

import javax.sql.DataSource;

import org.springframework.batch.samples.football.Player;
import org.springframework.batch.samples.football.PlayerDao;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Lucas Ward
 * @author Yanming Zhou
 *
 */
public class JdbcPlayerDao implements PlayerDao {

	public static final String INSERT_PLAYER = "INSERT into PLAYERS (player_id, last_name, first_name, pos, year_of_birth, year_drafted)"
			+ " values (:id, :lastName, :firstName, :position, :birthYear, :debutYear)";

	private JdbcClient jdbcClient;

	@Override
	public void savePlayer(Player player) {
		jdbcClient.sql(INSERT_PLAYER).paramSource(new BeanPropertySqlParameterSource(player)).update();
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcClient = JdbcClient.create(dataSource);
	}

}
