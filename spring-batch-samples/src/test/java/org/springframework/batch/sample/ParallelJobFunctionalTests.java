package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.batch.sample.common.StagingItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class ParallelJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private SimpleJdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Override
	protected void validatePostConditions() throws Exception {
		int count;
		count = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING where PROCESSED=?",
				StagingItemWriter.NEW);
		assertEquals(0, count);
		int total = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING");
		count = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING where PROCESSED=?",
				StagingItemWriter.DONE);
		assertEquals(total, count);
	}

}
