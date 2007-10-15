package org.springframework.batch.sample;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

/**
 * Test case for jobs that are expected to update customer credit value by fixed amount.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractCustomerCreditIncreaseTests extends AbstractLifecycleSpringContextTests {

	private JdbcOperations jdbcTemplate;
	
	private static final BigDecimal CREDIT_INCREASE = new BigDecimal(1000);
	
	private static final String ALL_CUSTOMERS = "select * from CUSTOMER";
	
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
		creditsBeforeUpdate = jdbcTemplate.queryForList(ALL_CUSTOMERS);
		jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				creditsBeforeUpdate.set(rowNum, rs.getBigDecimal(CREDIT_COLUMN));
				return null;
			}
		});
	}

	/**
	 * Credit was increased by REDIT_INCREASE
	 */
	protected void validatePostConditions() throws Exception {
		
		
		jdbcTemplate.query(ALL_CUSTOMERS, new RowMapper() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				final BigDecimal CREDIT_BEFORE_UPDATE = (BigDecimal) creditsBeforeUpdate.get(rowNum);
				final BigDecimal EXPECTED_CREDIT = CREDIT_BEFORE_UPDATE.add(CREDIT_INCREASE);
				assertTrue(EXPECTED_CREDIT.compareTo(rs.getBigDecimal(CREDIT_COLUMN)) == 0);
				return null;
			}
			
		});
	}

}
