package org.springframework.batch.sample;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.batch.sample.item.processor.CustomerCreditIncreaseProcessor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

/**
 * Test case for jobs that are expected to update customer credit value by fixed
 * amount.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public abstract class AbstractCustomerCreditIncreaseTests extends
		AbstractValidatingBatchLauncherTests {

	protected JdbcOperations jdbcTemplate;

	private static final BigDecimal CREDIT_INCREASE = CustomerCreditIncreaseProcessor.FIXED_AMOUNT;

	private static final String ALL_CUSTOMERS = "select * from CUSTOMER order by ID";

	private static final String CREDIT_COLUMN = "CREDIT";

	private List creditsBeforeUpdate;

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * All customers have the same credit
	 */
	protected void validatePreConditions() throws Exception {
		super.validatePreConditions();
		creditsBeforeUpdate = jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getBigDecimal(CREDIT_COLUMN);
			}
		});
	}

	/**
	 * Credit was increased by CREDIT_INCREASE
	 */
	protected void validatePostConditions() throws Exception {

		jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				final BigDecimal creditBeforeUpdate = (BigDecimal) creditsBeforeUpdate.get(rowNum);
				final BigDecimal expectedCredit = creditBeforeUpdate
						.add(CREDIT_INCREASE);
				assertEquals(expectedCredit, rs.getBigDecimal(CREDIT_COLUMN));
				return null;
			}

		});

	}

}
