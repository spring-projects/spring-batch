package org.springframework.batch.core.job.flow.support;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.JobFlowExecutor;

/**
 * {@link State} implementation that delegates to a {@link JobFlowExecutor} to
 * execute the specified {@link Step}.
 * 
 * @author Dave Syer
 * 
 */
public class StepState extends AbstractState {

	private final Step step;

	/**
	 * @param step the step that will be executed
	 */
	public StepState(Step step) {
		super(step.getName());
		this.step = step;
	}

	@Override
	public String handle(JobFlowExecutor executor) throws Exception {
		return executor.executeStep(step);
	}

}