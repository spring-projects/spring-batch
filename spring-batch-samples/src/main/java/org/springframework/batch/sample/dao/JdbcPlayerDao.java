/**
 * 
 */
package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.Player;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * @author Lucas Ward
 *
 */
public class JdbcPlayerDao  extends JdbcDaoSupport implements PlayerDao  {

	public static final String INSERT_PLAYER = "INSERT into players(player_id, " +
			"last_name, first_name, pos, year_of_birth, year_drafted)" +
			" values (?,?,?,?,?,?)";
	
	public void savePlayer(Player player) {
		
		getJdbcTemplate().update(INSERT_PLAYER, 
				new Object[]{player.getID(),player.getLastName(),
				player.getFirstName(), player.getPosition(), 
				new Integer(player.getBirthYear()), 
				new Integer(player.getDebutYear())});
	}
}
