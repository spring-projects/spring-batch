package org.springframework.batch.sample;

import org.springframework.batch.sample.dao.HibernateCreditWriter;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.orm.hibernate3.HibernateJdbcException;

/**
 * Test for HibernateJob - checks that customer credit has been updated to
 * expected value.
 * 
 * @author Dave Syer
 */
public class HibernateFailureJobFunctionalTests extends
		HibernateJobFunctionalTests {

	private HibernateCreditWriter writer;

	/**
	 * Public setter for the {@link HibernateCreditWriter} property.
	 * 
	 * @param writer
	 *            the writer to set
	 */
	public void setWriter(HibernateCreditWriter writer) {
		this.writer = writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.test.AbstractSingleSpringContextTests#onTearDown()
	 */
	protected void onTearDown() throws Exception {
		super.onTearDown();
		writer.setFailOnFlush(false);
	}

	protected String[] getConfigLocations() {
		return new String[] { "jobs/hibernateJob.xml" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.sample.AbstractValidatingBatchLauncherTests#testLaunchJob()
	 */
	public void testLaunchJob() throws Exception {
		writer.setFailOnFlush(true);
		
		int before = jdbcTemplate.queryForInt("SELECT COUNT(*) from CUSTOMER");
		assertTrue(before>0);
		try {
			super.testLaunchJob();
		} catch (HibernateJdbcException e) {
			// This is what would happen if the flush happened outside the RepeatContext:
			throw e;
		} catch (UncategorizedSQLException e) {
			// Expected, but check that the exception was registered:
			assertEquals(1, writer.getErrors().size());
			throw e;
		}
		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) from CUSTOMER");
		assertEquals(before, after);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.sample.AbstractCustomerCreditIncreaseTests#validatePostConditions()
	 */
	protected void validatePostConditions() throws Exception {
		// TODO: fix so that the postconditions in super class are true
		// super.validatePostConditions();
	}
}
