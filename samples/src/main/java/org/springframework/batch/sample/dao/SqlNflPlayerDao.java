/**
 * 
 */
package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.NflPlayer;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * @author Lucas Ward
 *
 */
public class SqlNflPlayerDao  extends JdbcDaoSupport implements NflPlayerDao  {

	public static final String INSERT_PLAYER = "INSERT into players(player_id, " +
			"last_name, first_name, pos, year_of_birth, year_drafted)" +
			" values (?,?,?,?,?,?)";
	
	/* (non-Javadoc)
	 * @see com.nfl.NflPlayerDao#savePlayer(com.nfl.NflPlayer)
	 */
	public void savePlayer(NflPlayer nflPlayer) {
		
		getJdbcTemplate().update(INSERT_PLAYER, 
				new Object[]{nflPlayer.getID(),nflPlayer.getLastName(),
				nflPlayer.getFirstName(), nflPlayer.getPosition(), 
				new Integer(nflPlayer.getBirthYear()), 
				new Integer(nflPlayer.getDebutYear())});
	}
}
