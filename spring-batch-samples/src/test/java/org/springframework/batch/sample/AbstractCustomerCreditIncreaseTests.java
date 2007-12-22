package org.springframework.batch.sample;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.sample.item.processor.CustomerCreditIncreaseProcessor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test case for jobs that are expected to update customer credit value by fixed
 * amount.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public abstract class AbstractCustomerCreditIncreaseTests extends AbstractValidatingBatchLauncherTests {

	protected JdbcOperations jdbcTemplate;

	protected PlatformTransactionManager transactionManager;

	private static final BigDecimal CREDIT_INCREASE = CustomerCreditIncreaseProcessor.FIXED_AMOUNT;

	private static final String ALL_CUSTOMERS = "select * from CUSTOMER order by ID";

	private static final String CREDIT_COLUMN = "CREDIT";

	protected static final String ID_COLUMN = "ID";

	private List creditsBeforeUpdate;

	/**
	 * @param jdbcTemplate
	 */
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Public setter for the PlatformTransactionManager.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * All customers have the same credit
	 */
	protected void validatePreConditions() throws Exception {
		super.validatePreConditions();
		creditsBeforeUpdate = (List) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getBigDecimal(CREDIT_COLUMN);
					}
				});
			}
		});
	}

	/**
	 * Credit was increased by CREDIT_INCREASE
	 */
	protected void validatePostConditions() throws Exception {

		final List matches = new ArrayList();

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {

					public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
						final BigDecimal creditBeforeUpdate = (BigDecimal) creditsBeforeUpdate.get(rowNum);
						final BigDecimal expectedCredit = creditBeforeUpdate.add(CREDIT_INCREASE);
						if (expectedCredit.equals(rs.getBigDecimal(CREDIT_COLUMN))) {
							matches.add(rs.getBigDecimal(ID_COLUMN));
						}
						return null;
					}

				});
				return null;
			}
		});

		assertEquals(getExpectedMatches(), matches.size());
		checkMatches(matches);

	}

	/**
	 * @param matches
	 */
	protected void checkMatches(List matches) {
		// no-op...
	}

	/**
	 * @return the expected number of matches in the updated credits.
	 */
	protected int getExpectedMatches() {
		return creditsBeforeUpdate.size();
	}

}
