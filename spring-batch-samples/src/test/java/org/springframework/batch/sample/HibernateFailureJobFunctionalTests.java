package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.sample.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.batch.sample.domain.trade.internal.HibernateCreditDao;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.hibernate3.HibernateJdbcException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test for HibernateJob - checks that customer credit has been updated to
 * expected value.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/hibernate-context.xml", "/jobs/hibernateJob.xml",
		"/job-runner-context.xml" })
public class HibernateFailureJobFunctionalTests {

	@Autowired
	private HibernateCreditDao writer;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private PlatformTransactionManager transactionManager;

	private static final BigDecimal CREDIT_INCREASE = CustomerCreditIncreaseProcessor.FIXED_AMOUNT;
	
	private static String[] customers = { "INSERT INTO CUSTOMER (id, version, name, credit) VALUES (1, 0, 'customer1', 100000)",
		"INSERT INTO CUSTOMER (id, version, name, credit) VALUES (2, 0, 'customer2', 100000)",
		"INSERT INTO CUSTOMER (id, version, name, credit) VALUES (3, 0, 'customer3', 100000)",
		"INSERT INTO CUSTOMER (id, version, name, credit) VALUES (4, 0, 'customer4', 100000)"};
	
	private static String DELETE_CUSTOMERS = "DELETE FROM CUSTOMER";

	private static final String ALL_CUSTOMERS = "select * from CUSTOMER order by ID";

	private static final String CREDIT_COLUMN = "CREDIT";

	protected static final String ID_COLUMN = "ID";

	private List<BigDecimal> creditsBeforeUpdate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Test
	public void testLaunchJob() throws Exception {
		
		validatePreConditions();
		
		JobParameters params = new JobParametersBuilder().addString("key", "failureJob").toJobParameters();
		writer.setFailOnFlush(2);

		try {
			jobLauncherTestUtils.launchJob(params);
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
		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from CUSTOMER");
		assertEquals(4, after);
		
		validatePostConditions();
		
	}

	/**
	 * All customers have the same credit
	 */
	@SuppressWarnings("unchecked")
	protected void validatePreConditions() throws Exception {
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

		assertEquals((creditsBeforeUpdate.size() - 1), matches.size());
		assertFalse(matches.contains(new BigDecimal(2)));		
	}
}
