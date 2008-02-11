package org.springframework.batch.execution.repository.dao;

import java.util.List;

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
	 * Find all StepInstances of the given JobInstance.
	 * 
	 * @param jobInstance the job to use as a search key
	 * @return list of {@link StepInstance}
	 */
	List findStepInstances(JobInstance jobInstance);

	/**
	 * Create a StepInstance for the given name and JobInstance.
	 * 
	 * @param jobInstance
	 * @param stepName
	 * 
	 * @return
	 */
	StepInstance createStepInstance(JobInstance jobInstance, String stepName);

	/**
	 * Update an existing StepInstance.
	 * 
	 * Preconditions: StepInstance must have an ID.
	 * 
	 * @param job
	 */
	void updateStepInstance(StepInstance stepInstance);
}
