package org.springframework.batch.sample.football;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class JdbcPlayerSummaryDao extends JdbcDaoSupport implements ItemWriter<PlayerSummary> {

	private static final String INSERT_SUMMARY = "INSERT into PLAYER_SUMMARY(ID,YEAR_NO,COMPLETES,ATTEMPTS," +
			"PASSING_YARDS,PASSING_TD,INTERCEPTIONS,RUSHES,RUSH_YARDS,RECEPTIONS,RECEPTIONS_YARDS," +
			"TOTAL_TD) values(?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public void write(PlayerSummary summary) {
		
		Object[] args = new Object[]{summary.getId(), new Integer(summary.getYear()),
				new Integer(summary.getCompletes()), new Integer(summary.getAttempts()),
				new Integer(summary.getPassingYards()), new Integer(summary.getPassingTd()),
				new Integer(summary.getInterceptions()), new Integer(summary.getRushes()),
				new Integer(summary.getRushYards()), new Integer(summary.getReceptions()),
				new Integer(summary.getReceptionYards()), new Integer(summary.getTotalTd()) };
		
		getJdbcTemplate().update(INSERT_SUMMARY, args);
	}

	public void close() throws Exception {
	}

	public void clear() throws ClearFailedException {
	}

	public void flush() throws FlushFailedException {
	}

}
