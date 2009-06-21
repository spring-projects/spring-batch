package org.springframework.batch.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/sample-steps.xml" })
public class SampleStepTests implements ApplicationContextAware {

	@Autowired
	private SimpleJdbcTemplate jdbcTemplate;

	private StepRunner stepRunner;

	private ApplicationContext context;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobRepository jobRepository;

	@Before
	public void setUp() {
		jdbcTemplate.update("create table TESTS (ID integer, NAME varchar(40))");
		stepRunner = new StepRunner(jobLauncher, jobRepository);
	}

	@After
	public void tearDown() {
		this.jdbcTemplate.update("drop table TESTS");
	}

	@Test
	public void testTasklet() {
		Step step = (Step) context.getBean("s2");
		assertEquals(BatchStatus.COMPLETED, stepRunner.launchStep(step).getStatus());
		assertEquals(2, jdbcTemplate.queryForInt("SELECT ID from TESTS where NAME = 'SampleTasklet2'"));
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

}
