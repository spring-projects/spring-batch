package org.springframework.batch.sample.dao;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ClearFailedException;
import org.springframework.batch.item.exception.FlushFailedException;
import org.springframework.batch.sample.domain.PlayerSummary;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.Assert;

public class JdbcPlayerSummaryDao extends JdbcDaoSupport implements ItemWriter {

	private static final String INSERT_SUMMARY = "INSERT into PLAYER_SUMMARY(ID,YEAR_NO,COMPLETES,ATTEMPTS," +
			"PASSING_YARDS,PASSING_TD,INTERCEPTIONS,RUSHES,RUSH_YARDS,RECEPTIONS,RECEPTIONS_YARDS," +
			"TOTAL_TD) values(?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public void write(Object output) {
		
		Assert.isInstanceOf(PlayerSummary.class, output, JdbcPlayerSummaryDao.class.getName() + " only " +
				"supports outputing " + PlayerSummary.class.getName() + " instances.");
		
		PlayerSummary summary = (PlayerSummary)output;
		
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
