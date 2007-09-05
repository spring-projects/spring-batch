/**
 * 
 */
package org.springframework.batch.sample.dao;

import javax.sql.DataSource;

import org.springframework.batch.sample.domain.NflPlayer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author venu.valmeti
 *
 */
public class SqlNflPlayerDao implements NflPlayerDao, InitializingBean {

	public static final String INSERT_PLAYER = "INSERT into players(player_id, " +
			"last_name, first_name, pos, year_of_birth, year_drafted)" +
			" values (?,?,?,?,?,?)";
	
	DataSource dataSource;
	JdbcTemplate jdbcTemplate;
	
	public void afterPropertiesSet() throws Exception {
		if(dataSource == null){
			throw new IllegalStateException("DataSource must not be null.");
		}
	}
	
	public SqlNflPlayerDao(DataSource dataSource){
		this.dataSource = dataSource;
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	/* (non-Javadoc)
	 * @see com.nfl.NflPlayerDao#savePlayer(com.nfl.NflPlayer)
	 */
	public void savePlayer(NflPlayer nflPlayer) {
		
		jdbcTemplate.update(INSERT_PLAYER, 
				new Object[]{nflPlayer.getID(),nflPlayer.getLastName(),
				nflPlayer.getFirstName(), nflPlayer.getPosition(), 
				new Integer(nflPlayer.getBirthYear()), 
				new Integer(nflPlayer.getDebutYear())});
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}


}
