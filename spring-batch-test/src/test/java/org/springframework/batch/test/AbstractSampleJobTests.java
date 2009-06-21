package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.sample.SampleTasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * This is an abstract test class to be used by test classes to test the
 * {@link AbstractJobTests} class.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractSampleJobTests extends AbstractJobTests {

	private SimpleJdbcTemplate jdbcTemplate;

	@Autowired
	@Qualifier("tasklet2")
	private SampleTasklet tasklet2;

	@Autowired
	public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Before
	public void setUp() {
		this.jdbcTemplate.update("create table TESTS (ID integer, NAME varchar(40))");
		tasklet2.jobContextEntryFound = false;
	}

	@After
	public void tearDown() {
		this.jdbcTemplate.update("drop table TESTS");
	}

	@Test
	public void testJob() throws Exception {
		assertEquals(BatchStatus.COMPLETED, this.launchJob().getStatus());
		this.verifyTasklet(1);
		this.verifyTasklet(2);
	}

	@Test(expected = IllegalStateException.class)
	public void testNonExistentStep() {
		launchStep("nonExistent");
	}

	@Test
	public void testStep1Execution() {
		assertEquals(BatchStatus.COMPLETED, this.launchStep("step1").getStatus());
		this.verifyTasklet(1);
	}

	@Test
	public void testStep2Execution() {
		assertEquals(BatchStatus.COMPLETED, this.launchStep("step2").getStatus());
		this.verifyTasklet(2);
	}

	@Test
	public void testStepLaunchJobContextEntry() {
		ExecutionContext jobContext = new ExecutionContext();
		jobContext.put("key1", "value1");
		assertEquals(BatchStatus.COMPLETED, this.launchStep("step2", jobContext).getStatus());
		this.verifyTasklet(2);
		assertTrue(tasklet2.jobContextEntryFound);
	}

	private void verifyTasklet(int id) {
		assertEquals(id, jdbcTemplate.queryForInt("SELECT ID from TESTS where NAME = 'SampleTasklet" + id + "'"));
	}

}
