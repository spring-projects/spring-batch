package org.springframework.batch.execution.repository.dao;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepInstance;

public interface StepInstanceDao {

	/**
	 * Find a step with the given JobId and Step Name. Return null if none are
	 * found.
	 * 
	 * @param jobInstance
	 * @param stepName
	 * @return StepInstance
	 */
	StepInstance findStepInstance(JobInstance jobInstance, String stepName);

	/**
	 * Create a StepInstance for the given name and JobInstance.
	 * 
	 * @param jobInstance
	 * @param stepName
	 * 
	 * @return
	 */
	StepInstance createStepInstance(JobInstance jobInstance, String stepName);

}
