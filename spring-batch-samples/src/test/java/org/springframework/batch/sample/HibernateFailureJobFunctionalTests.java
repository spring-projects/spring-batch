package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.sample.trade.HibernateCreditDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.orm.hibernate3.HibernateJdbcException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test for HibernateJob - checks that customer credit has been updated to
 * expected value.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class HibernateFailureJobFunctionalTests extends AbstractCustomerCreditIncreaseTests {

	private HibernateCreditDao writer;

	/**
	 * Public setter for the {@link HibernateCreditDao} property.
	 * 
	 * @param writer
	 *            the writer to set
	 */
	@Autowired
	public void setWriter(HibernateCreditDao writer) {
		this.writer = writer;
	}

	@Transactional @Test
	public void testLaunchJob() throws Exception {
		JobParameters params = new JobParametersBuilder().addString("key", "failureJob").toJobParameters();
		setJobParameters(params);
		writer.setFailOnFlush(2);

		int before = jdbcTemplate.queryForInt("SELECT COUNT(*) from CUSTOMER");
		assertTrue(before > 0);
		try {
			super.testLaunchJob();
		} catch (HibernateJdbcException e) {
			// This is what would happen if the flush happened outside the
			// RepeatContext:
			throw e;
		} catch (UncategorizedSQLException e) {
			// This is what would happen if the job wasn't configured to skip
			// exceptions at the step level.
			// assertEquals(1, writer.getErrors().size());
			throw e;
		}
		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) from CUSTOMER");
		assertEquals(before, after);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.sample.AbstractCustomerCreditIncreaseTests#checkMatches(java.util.List)
	 */
	protected void checkMatches(List<BigDecimal> matches) {
		assertFalse(matches.contains(new BigDecimal(2)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.sample.AbstractCustomerCreditIncreaseTests#getExpectedMatches()
	 */
	protected int getExpectedMatches() {
		// One record was skipped, so it won't be processed in the final state.
		return super.getExpectedMatches() - 1;
	}
}
