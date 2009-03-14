package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.security.MessageDigest;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "sql-dao-test.xml")
public class JdbcJobInstanceDaoTests extends AbstractJobInstanceDaoTests {
	
	protected JobInstanceDao getJobInstanceDao() {
		deleteFromTables("BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE");
		return (JobInstanceDao) applicationContext.getBean("jobInstanceDao");
	}

	@Test
	public void testFindJobInstanceByExecution(){
		
		JobExecutionDao jobExecutionDao = (JobExecutionDao) applicationContext.getBean("jobExecutionDao");
		
		JobInstance jobInstance = dao.createJobInstance("testInstance", new JobParameters());
		JobExecution jobExecution = new JobExecution(jobInstance, 2L);
		jobExecutionDao.saveJobExecution(jobExecution);
		
		JobInstance returnedInstance = dao.getJobInstance(jobExecution);
		Assert.assertEquals(jobInstance, returnedInstance);
	}
	
	@Test
	public void testHexing() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] bytes = digest.digest("f78spx".getBytes("UTF-8"));
		StringBuffer output = new StringBuffer();
		for (byte bite : bytes) {
			output.append(String.format("%02x", bite));
		}
		assertEquals("Wrong hash: "+output,32, output.length());
		String value = String.format("%032x", new BigInteger(1, bytes));
		assertEquals("Wrong hash: "+value,32, value.length());
		assertEquals(value, output.toString());
	}
}
