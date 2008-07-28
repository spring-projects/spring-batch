/**
 * 
 */
package org.springframework.batch.sample.domain.football.internal;

import org.springframework.batch.sample.domain.football.Player;
import org.springframework.batch.sample.domain.football.PlayerDao;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

/**
 * @author Lucas Ward
 *
 */
public class JdbcPlayerDao  extends SimpleJdbcDaoSupport implements PlayerDao  {

	public static final String INSERT_PLAYER =
			"INSERT into players(player_id, last_name, first_name, pos, year_of_birth, year_drafted)" +
			" values (:id, :lastName, :firstName, :position, :birthYear, :debutYear)";
	
	public void savePlayer(Player player) {
		
		getSimpleJdbcTemplate().update(INSERT_PLAYER,
		new BeanPropertySqlParameterSource(player));
		
	}
}
