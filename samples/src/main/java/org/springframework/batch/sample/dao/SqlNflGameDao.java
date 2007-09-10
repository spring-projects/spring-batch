package org.springframework.batch.sample.dao;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.sample.domain.NflGame;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.Assert;

public class SqlNflGameDao extends JdbcDaoSupport implements OutputSource {

	private static final String INSERT_GAME = "INSERT into GAMES(player_id,year,team,week,opponent,"
			+ "completes,attempts,passing_yards,passing_td,interceptions,rushes,rush_yards,"
			+ "receptions,receptions_yards,total_td) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public void write(Object output) {
		Assert.isTrue(output instanceof NflGame,
				"Only NflGame objects can be written out" + "using this Dao");

		NflGame game = (NflGame) output;

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

	public void close() {
		// TODO Auto-generated method stub

	}

	public void open() {
		// TODO Auto-generated method stub

	}

}
