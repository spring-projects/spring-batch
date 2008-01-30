package org.springframework.batch.sample.dao;

import java.util.Properties;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.batch.sample.domain.Game;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.Assert;

public class JdbcGameDao extends JdbcDaoSupport implements ItemWriter {

	private static final String INSERT_GAME = "INSERT into GAMES(player_id,year_no,team,week,opponent,"
			+ "completes,attempts,passing_yards,passing_td,interceptions,rushes,rush_yards,"
			+ "receptions,receptions_yards,total_td) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public void write(Object output) {
		Assert.isTrue(output instanceof Game,
				"Only Game objects can be written out" + "using this Dao");

		Game game = (Game) output;

		Object[] args = new Object[] { game.getId(),
				new Integer(game.getYear()), game.getTeam(),
				new Integer(game.getWeek()), game.getOpponent(),
				new Integer(game.getCompletes()),
				new Integer(game.getAttempts()),
				new Integer(game.getPassingYards()),
				new Integer(game.getPassingTd()),
				new Integer(game.getInterceptions()),
				new Integer(game.getRushes()),
				new Integer(game.getRushYards()),
				new Integer(game.getReceptions()),
				new Integer(game.getReceptionYards()),
				new Integer(game.getTotalTd()) };

		this.getJdbcTemplate().update(INSERT_GAME, args);
	}

	/**
	 * Do nothing.
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open() throws Exception {
		// no-op
	}
	
	/**
	 * Do nothing.
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	public void close() throws Exception {
		// no-op
	}

	/**
	 * Return empty {@link StreamContext}.
	 * @see org.springframework.batch.item.ItemStream#getRestartData()
	 */
	public StreamContext getRestartData() {
		return new GenericStreamContext(new Properties());
	}
	
	/**
	 * Do nothing.
	 * @see org.springframework.batch.item.ItemStream#restoreFrom(org.springframework.batch.item.StreamContext)
	 */
	public void restoreFrom(StreamContext data) {
		
	}

}
