package org.springframework.batch.sample.dao;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.sample.domain.NflPlayerSummary;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.Assert;

public class SqlNflPlayerSummaryDao extends JdbcDaoSupport implements OutputSource {

	private static final String INSERT_SUMMARY = "INSERT into PLAYER_SUMMARY(ID,YEAR,COMPLETES,ATTEMPTS," +
			"PASSING_YARDS,PASSING_TD,INTERCEPTIONS,RUSHES,RUSH_YARDS,RECEPTIONS,RECEPTIONS_YARDS," +
			"TOTAL_TD) values(?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public void write(Object output) {
		
		Assert.isInstanceOf(NflPlayerSummary.class, output, SqlNflPlayerSummaryDao.class + " only " +
				"supports outputing " + NflPlayerSummary.class + " instances.");
		
		NflPlayerSummary summary = (NflPlayerSummary)output;
		
		Object[] args = new Object[]{summary.getId(), Integer.valueOf(summary.getYear()),
				Integer.valueOf(summary.getCompletes()), Integer.valueOf(summary.getAttempts()),
				Integer.valueOf(summary.getPassingYards()), Integer.valueOf(summary.getPassingTd()),
				Integer.valueOf(summary.getInterceptions()), Integer.valueOf(summary.getRushes()),
				Integer.valueOf(summary.getRushYards()), Integer.valueOf(summary.getReceptions()),
				Integer.valueOf(summary.getReceptionYards()), Integer.valueOf(summary.getTotalTd()) };
		
		getJdbcTemplate().update(INSERT_SUMMARY, args);
	}

}
