package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "sql-dao-test.xml")
public class JdbcJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	private JobInstanceDao jobInstanceDao;

	@Autowired
	private JobExecutionDao jobExecutionDao;

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT",
				"BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS", 
				"BATCH_JOB_EXECUTION", "BATCH_JOB_INSTANCE");
		return jobInstanceDao;
	}

	@Transactional
	@Test
	public void testFindJobInstanceByExecution() {

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = dao.createJobInstance("testInstance",
				jobParameters);
		JobExecution jobExecution = new JobExecution(jobInstance, 2L, jobParameters);
		jobExecutionDao.saveJobExecution(jobExecution);

		JobInstance returnedInstance = dao.getJobInstance(jobExecution);
		assertEquals(jobInstance, returnedInstance);
	}

	@Test
	public void testHexing() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] bytes = digest.digest("f78spx".getBytes("UTF-8"));
		StringBuffer output = new StringBuffer();
		for (byte bite : bytes) {
			output.append(String.format("%02x", bite));
		}
		assertEquals("Wrong hash: " + output, 32, output.length());
		String value = String.format("%032x", new BigInteger(1, bytes));
		assertEquals("Wrong hash: " + value, 32, value.length());
		assertEquals(value, output.toString());
	}
	
	@Test
	public void testJobInstanceWildcard() {
		//look up a job using a wildcard (* substituted to %)
		// unrelated job instance that should be ignored by the query
		dao.createJobInstance("anotherJob", new JobParameters());
		// we need two instances of the same job to check ordering
		dao.createJobInstance("someJob", new JobParameters());
		//now look for them
		List<JobInstance> jobInstances = dao.findJobInstancesByName("*Job", 0, 2);
		assertEquals(2, jobInstances.size());
		for (JobInstance instance : jobInstances) {
			assertTrue(instance.getJobName().contains("Job"));
		}//end for
		//try with after wildcards
		jobInstances = dao.getJobInstances("Job*", 0, 2);
		assertTrue(jobInstances.isEmpty());
	}	
}
