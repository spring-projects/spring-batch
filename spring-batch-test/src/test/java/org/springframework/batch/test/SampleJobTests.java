package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/sampleJob.xml" })
public class SampleJobTests extends AbstractSimpleJobTests {

	private SimpleJdbcTemplate jdbcTemplate;

	@Autowired
	public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Before
	public void setUp() {
		this.jdbcTemplate.update("create table TESTS (ID integer, NAME varchar(40))");
	}

	@After
	public void tearDown() {
		this.jdbcTemplate.update("drop table TESTS");
	}

	@Test
	public void test1() {
		assertEquals(BatchStatus.COMPLETED, this.launchStep("step1").getStatus());
		this.verifyTasklet(1);
	}

	private void verifyTasklet(int id) {
		assertEquals(id, jdbcTemplate.queryForInt("SELECT ID from TESTS where NAME = 'SampleTasklet" + id + "'"));
	}

	@Test
	public void testJob() {
		assertEquals(BatchStatus.COMPLETED,this.launchJob().getStatus());
		this.verifyTasklet(1);
		this.verifyTasklet(2);
	}
	
	@Test(expected=IllegalStateException.class)
	public void voidTestNonExistentStep(){
		launchStep("nonExistent");
	}

	@Test
	public void test2() {
		assertEquals(BatchStatus.COMPLETED, this.launchStep("step2").getStatus());
		this.verifyTasklet(2);
	}
}
