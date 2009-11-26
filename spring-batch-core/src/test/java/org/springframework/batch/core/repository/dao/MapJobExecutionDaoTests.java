package org.springframework.batch.core.repository.dao;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import static org.junit.Assert.*;

@RunWith(JUnit4ClassRunner.class)
public class MapJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		return new MapJobExecutionDao();
	}
	
	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	@Test
	public void testPersistentCopy() {
		JobExecutionDao tested = new MapJobExecutionDao();
		JobExecution jobExecution = new JobExecution(new JobInstance((long) 1, new JobParameters(), "mapJob"));
		
		assertNull(jobExecution.getStartTime());
		tested.saveJobExecution(jobExecution);
		jobExecution.setStartTime(new Date());
		
		JobExecution retrieved = tested.getJobExecution(jobExecution.getId());
		assertNull(retrieved.getStartTime());
		
		tested.updateJobExecution(jobExecution);
		jobExecution.setEndTime(new Date());
		assertNull(retrieved.getEndTime());
		
	}

}
