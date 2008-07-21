package org.springframework.batch.sample.dao;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.Game;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class JdbcGameDao extends JdbcDaoSupport implements ItemWriter<Game> {

	private static final String INSERT_GAME = "INSERT into GAMES(player_id,year_no,team,week,opponent,"
			+ "completes,attempts,passing_yards,passing_td,interceptions,rushes,rush_yards,"
			+ "receptions,receptions_yards,total_td) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public void write(Game game) {

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

	public void clear() throws ClearFailedException {
	}

	public void flush() throws FlushFailedException {
	}

}
