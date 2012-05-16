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
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is an abstract test class to be used by test classes to test the
 * {@link AbstractJobTests} class.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/job-runner-context.xml" })
public abstract class AbstractSampleJobTests {

	private SimpleJdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

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
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchJob().getStatus());
		this.verifyTasklet(1);
		this.verifyTasklet(2);
	}

	@Test(expected = IllegalStateException.class)
	public void testNonExistentStep() {
		jobLauncherTestUtils.launchStep("nonExistent");
	}

	@Test
	public void testStep1Execution() {
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step1").getStatus());
		this.verifyTasklet(1);
	}

	@Test
	public void testStep2Execution() {
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step2").getStatus());
		this.verifyTasklet(2);
	}

	@Test
	@Repeat(10)
	public void testStep3Execution() throws Exception {
		// logging only, may complete in < 1ms (repeat so that it's likely to for at least one of those times)
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step3").getStatus());
	}

	@Test
	public void testStepLaunchJobContextEntry() {
		ExecutionContext jobContext = new ExecutionContext();
		jobContext.put("key1", "value1");
		assertEquals(BatchStatus.COMPLETED, jobLauncherTestUtils.launchStep("step2", jobContext).getStatus());
		this.verifyTasklet(2);
		assertTrue(tasklet2.jobContextEntryFound);
	}

	private void verifyTasklet(int id) {
		assertEquals(id, jdbcTemplate.queryForInt("SELECT ID from TESTS where NAME = 'SampleTasklet" + id + "'"));
	}

}
