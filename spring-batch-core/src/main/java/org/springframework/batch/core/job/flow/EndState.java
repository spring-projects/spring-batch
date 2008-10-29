package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.flow.AbstractState;
import org.springframework.batch.flow.FlowExecution;
import org.springframework.batch.flow.State;

/**
 * {@link State} implementation for ending a job if it is in progress and
 * continuing if just starting.
 * 
 * @author Dave Syer
 * 
 */
public class EndState extends AbstractState<JobFlowExecutor> {

	private final BatchStatus status;

	/**
	 * @param name
	 */
	EndState(BatchStatus status, String name) {
		super(name);
		this.status = status;
	}

	/**
	 * Set the status as long the {@link JobExecution} is in progress. If this
	 * is the first place we came after a restart we do nothing (otherwise the
	 * same outcome that ended the job on the last run will occur).
	 * 
	 * @see State#handle(Object)
	 */
	@Override
	public String handle(JobFlowExecutor context) throws Exception {
		JobExecution jobExecution = context.getJobExecution();
		// If there are no step executions, then we are at the beginning of a
		// restart
		if (!jobExecution.getStepExecutions().isEmpty()) {
			jobExecution.upgradeStatus(status);
		}
		return FlowExecution.COMPLETED;
	}

}