package org.springframework.batch.core.repository.dao.gemfire;

import java.util.Collection;

import javax.annotation.Resource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Implementation of the ExecutionContextDao mainly to support the Gemfire 
 * scenario that the Step/JobExecution regions persist their own execution context 
 * 
 * @author Will Schipp
 *
 */
public class GemfireExecutionContextDaoImpl implements ExecutionContextDao {
	
	@Autowired
	private GemfireJobExecutionDao jobExecutionDao;
	
	@Autowired
	private GemfireStepExecutionDao stepExecutionDao;
	
	@Override
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		//as this is already a persistent object - just return from the job execution
		return jobExecution.getExecutionContext();
	}

	@Override
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		//as this is already a persistent object - just return from the step execution		
		return stepExecution.getExecutionContext();
	}

	@Override
	public void saveExecutionContext(JobExecution jobExecution) {
		jobExecutionDao.save(jobExecution);//called inline with an execution context change
	}

	@Override
	public void saveExecutionContext(StepExecution stepExecution) {
		stepExecutionDao.save(stepExecution);//called inline with an execution context change
	}

	@Override
	public void saveExecutionContexts(Collection<StepExecution> stepExecutions) {
		//save the step executions
		stepExecutionDao.save(stepExecutions);
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		this.saveExecutionContext(jobExecution);
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		this.saveExecutionContext(stepExecution);
	}

}
