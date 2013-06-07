package org.springframework.batch.core.repository.dao.gemfire;

import java.util.List;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.data.gemfire.repository.GemfireRepository;

/**
 * Spring Data Gemfire compliant StepExecution Repository
 * 
 * 
 * @author Will Schipp
 *
 */
public interface GemfireStepExecutionDao extends GemfireRepository<StepExecution, Long>, StepExecutionDao {

	/**
	 * execution method to retrieve step executions by their job execution id
	 * @param jobExecutionId
	 * @return
	 */
	public List<StepExecution> findByJobExecutionId(Long jobExecutionId);

	/**
	 * retrieve a single execution by its id and its job execution id
	 * @param jobExecutionId
	 * @param stepExecutionId
	 * @return
	 */
	public StepExecution findByJobExecutionIdAndId(Long jobExecutionId, Long stepExecutionId);
	
}
