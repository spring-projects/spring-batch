package org.springframework.batch.core.repository.dao.gemfire;

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * GemFire implementation of the StepExecutionDao
 *  (note - naming breaks with the convention of exiting Batch implementations due to Spring Data compliance)
 * 
 * @author Will Schipp
 *
 */
public class GemfireStepExecutionDaoImpl implements StepExecutionDao {

	@Autowired
	private GemfireStepExecutionDao stepExecutionDao;
	
	@Autowired
	private GemfireJobExecutionDao jobExecutionDao;
	
	@Override
	public void saveStepExecution(StepExecution stepExecution) {
		Assert.isNull(stepExecution.getId(),
				"to-be-saved (not updated) StepExecution can't already have an id assigned");
		Assert.isNull(stepExecution.getVersion(),
				"to-be-saved (not updated) StepExecution can't already have a version assigned");		
		//validate
		validateStepExecution(stepExecution);
		//generate id
		stepExecution.setId(this.generateId(stepExecution.getJobExecutionId(), stepExecution.getStepName()));
		//set the version
		stepExecution.incrementVersion(); //Should be 0
		//persist
		stepExecutionDao.save(stepExecution);
		
	}

	@Override
	public void saveStepExecutions(Collection<StepExecution> stepExecutions) {
		Assert.notNull(stepExecutions, "Attempt to save a null collection of step executions");
        for (StepExecution stepExecution : stepExecutions) {
    		//validate
    		validateStepExecution(stepExecution);
    		//generate id
    		stepExecution.setId(this.generateId(stepExecution.getJobExecutionId(), stepExecution.getStepName()));
    		//set the version
    		stepExecution.incrementVersion(); //Should be 0
        }//end for
        //now persist
        stepExecutionDao.save(stepExecutions);
        
	}

	@Override
	public void updateStepExecution(StepExecution stepExecution) {
		//validate
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution Id cannot be null. StepExecution must saved  before it can be updated.");
		//sync
		synchronized (stepExecution) {
			stepExecutionDao.save(stepExecution);
			//increment
			stepExecution.incrementVersion();
		}//end sync
		
	}

	@Override
	public StepExecution getStepExecution(JobExecution jobExecution,Long stepExecutionId) {
		return stepExecutionDao.findByJobExecutionIdAndId(jobExecution.getId(),stepExecutionId);
	}

	@Override
	public void addStepExecutions(JobExecution jobExecution) {
		//select by id and add
		jobExecution.addStepExecutions(stepExecutionDao.findByJobExecutionId(jobExecution.getId()));
	}

	/**
	 * generates an id that is based on the jobexecution parent-child relationship
	 * and further ensures uniqueness by using the ordinal position within the job execution
	 * @param jobExecutionId
	 * @param stepName
	 * @return
	 */
	protected Long generateId(long jobExecutionId,String stepName) {
		//init
		int count = 0;
		//get the existing stepexecution count for the job execution
		List<StepExecution> steps = stepExecutionDao.findByJobExecutionId(jobExecutionId);
		if (steps != null) {
			count = steps.size() + 1;
		}//end if
		//build the id and return (jobExecution id (unique) + count)
		return jobExecutionId + count;
	}
	
	
	/**
	 * Validate StepExecution. At a minimum, JobId, StartTime, and Status cannot
	 * be null. EndTime can be null for an unfinished job.
	 *
	 * @throws IllegalArgumentException
	 */
	private void validateStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution);
		Assert.notNull(stepExecution.getStepName(), "StepExecution step name cannot be null.");
		Assert.notNull(stepExecution.getStartTime(), "StepExecution start time cannot be null.");
		Assert.notNull(stepExecution.getStatus(), "StepExecution status cannot be null.");
	}
	
	
}
