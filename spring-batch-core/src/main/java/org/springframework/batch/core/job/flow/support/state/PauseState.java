package org.springframework.batch.core.job.flow.support.state;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutor;

/**
 * @author Dave Syer
 * 
 */
public class PauseState extends AbstractState {

	/**
	 * @param name
	 */
	public PauseState(String name) {
		super(name);
	}

	@Override
	public String handle(FlowExecutor executor) throws Exception {

		JobExecution jobExecution = executor.getJobExecution();

		// This state is just a toggle for the status of the job execution. If
		// not already paused we pause it, and expect the flow to respect the
		// status.
		synchronized (jobExecution) {
			if (!jobExecution.isWaiting()) {
				jobExecution.pauseAndWait();
				return FlowExecution.PAUSED;
			}

			// ...otherwise set the status to show that it has resumed
			jobExecution.setStatus(BatchStatus.STARTED);
			return FlowExecution.COMPLETED;
		}
	}

}