package org.springframework.batch.core.repository.dao;

import java.util.Date;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;


public class MapJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	protected JobExecutionDao getJobExecutionDao() {
		MapJobExecutionDao.clear();
		MapJobInstanceDao.clear();
		return new MapJobExecutionDao();
	}
	
	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	public void testPersistentCopy() {
		
		JobExecutionDao tested = new MapJobExecutionDao();
		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "mapJob");
		JobExecution jobExecution = new JobExecution(jobInstance);
		
		assertNull(jobExecution.getStartTime());
		tested.saveJobExecution(jobExecution);
		jobExecution.setStartTime(new Date());
		
		JobExecution retrieved = tested.getLastJobExecution(jobInstance);
		assertNull(retrieved.getStartTime());
		
		tested.updateJobExecution(jobExecution);
		jobExecution.setEndTime(new Date());
		assertNull(retrieved.getEndTime());
		
		tested.saveOrUpdateExecutionContext(jobExecution);
		jobExecution.getExecutionContext().put("key", "value");
		ExecutionContext stored = tested.findExecutionContext(jobExecution);
		assertTrue(stored.isEmpty());
		
	}
	

}
