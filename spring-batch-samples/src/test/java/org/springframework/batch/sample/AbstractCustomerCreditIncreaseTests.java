package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.sample.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
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

	protected SimpleJdbcTemplate simpleJdbcTemplate;

	protected PlatformTransactionManager transactionManager;

	private static final BigDecimal CREDIT_INCREASE = CustomerCreditIncreaseProcessor.FIXED_AMOUNT;
	
	private static String[] customers = { "INSERT INTO customer (id, version, name, credit) VALUES (1, 0, 'customer1', 100000)",
		"INSERT INTO customer (id, version, name, credit) VALUES (2, 0, 'customer2', 100000)",
		"INSERT INTO customer (id, version, name, credit) VALUES (3, 0, 'customer3', 100000)",
		"INSERT INTO customer (id, version, name, credit) VALUES (4, 0, 'customer4', 100000)"};
	
	private static String DELETE_CUSTOMERS = "DELETE FROM customer";

	private static final String ALL_CUSTOMERS = "select * from CUSTOMER order by ID";

	private static final String CREDIT_COLUMN = "CREDIT";

	protected static final String ID_COLUMN = "ID";

	private List<BigDecimal> creditsBeforeUpdate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * All customers have the same credit
	 */
	@SuppressWarnings("unchecked")
	protected void validatePreConditions() throws Exception {
		super.validatePreConditions();
		ensureState();
		creditsBeforeUpdate = (List<BigDecimal>) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {				
				return simpleJdbcTemplate.query(ALL_CUSTOMERS, new ParameterizedRowMapper<BigDecimal>() {
					public BigDecimal mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getBigDecimal(CREDIT_COLUMN);
					}
				});
			}
		});
	}

	/*
	 * Ensure the state of the database is accurate by delete all the contents of the
	 * customer table and reading the expected defaults.
	 */
	private void ensureState(){
		new TransactionTemplate(transactionManager).execute(new TransactionCallback(){

			public Object doInTransaction(TransactionStatus status) {
				simpleJdbcTemplate.update(DELETE_CUSTOMERS);
				for (String customer : customers) {
					simpleJdbcTemplate.update(customer);
				}
				return null;
			}
			});

	}
	
	/**
	 * Credit was increased by CREDIT_INCREASE
	 */
	protected void validatePostConditions() throws Exception {

		final List<BigDecimal> matches = new ArrayList<BigDecimal>();

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				simpleJdbcTemplate.getJdbcOperations().query(ALL_CUSTOMERS, new RowCallbackHandler() {

					private int i = 0;

					public void processRow(ResultSet rs) throws SQLException {
						final BigDecimal creditBeforeUpdate = creditsBeforeUpdate.get(i++);
						final BigDecimal expectedCredit = creditBeforeUpdate.add(CREDIT_INCREASE);
						if (expectedCredit.equals(rs.getBigDecimal(CREDIT_COLUMN))) {
							matches.add(rs.getBigDecimal(ID_COLUMN));
						}
					}
					
				});
				return null;
			}
		});

		assertEquals(getExpectedMatches(), matches.size());
		checkMatches(matches);		
	}

	protected void checkMatches(List<BigDecimal> matches) {
		// no-op...
	}

	/**
	 * @return the expected number of matches in the updated credits.
	 */
	protected int getExpectedMatches() {
		return creditsBeforeUpdate.size();
	}

}
