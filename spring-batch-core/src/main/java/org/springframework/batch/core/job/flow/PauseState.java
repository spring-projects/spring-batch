package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

/**
 * @author Dave Syer
 * 
 */
public class PauseState extends AbstractState<JobFlowExecutor> {

	/**
	 * @param name
	 */
	PauseState(String name) {
		super(name);
	}

	@Override
	public String handle(JobFlowExecutor context) throws Exception {

		JobExecution jobExecution = context.getJobExecution();

		// This state is just a toggle for the status of the job execution. If
		// not already paused we pause it, and expect the flow to respect the
		// status.
		if (!jobExecution.isPaused()) {
			jobExecution.pause();
			return FlowExecution.PAUSED;
		}
		
		// ...otherwise set the status to show that it has resumed
		jobExecution.setStatus(BatchStatus.STARTED);
		return FlowExecution.COMPLETED;

	}

}