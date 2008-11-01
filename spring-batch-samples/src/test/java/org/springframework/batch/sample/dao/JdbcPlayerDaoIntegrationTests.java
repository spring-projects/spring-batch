/**
 * 
 */
package org.springframework.batch.sample.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.Player;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * @author Lucas Ward
 *
 */
public class JdbcPlayerDaoIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {
	
	private JdbcPlayerDao playerDao;
	private Player player;
	private static final String GET_PLAYER = "SELECT * from PLAYERS";
	
	protected String[] getConfigLocations() {
		return new String[] {"data-source-context.xml"};
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		playerDao = new JdbcPlayerDao();
		playerDao.setJdbcTemplate(this.jdbcTemplate);
		
		player = new Player();
		player.setID("AKFJDL00");
		player.setFirstName("John");
		player.setLastName("Doe");
		player.setPosition("QB");
		player.setBirthYear(1975);
		player.setDebutYear(1998);
	}


	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();

		jdbcTemplate.execute("delete from PLAYERS");

	}

	public void testSavePlayer(){
		
		playerDao.savePlayer(player);
		
		getJdbcTemplate().query(GET_PLAYER, new RowCallbackHandler(){

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
