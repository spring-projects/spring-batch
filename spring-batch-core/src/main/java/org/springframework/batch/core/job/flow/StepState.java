package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.Step;

/**
 * {@link State} implementation that delegates to a {@link JobFlowExecutor} to
 * execute the specified {@link Step}.
 * 
 * @author Dave Syer
 * 
 */
public class StepState extends AbstractState<JobFlowExecutor> {

	private final Step step;

	/**
	 * @param step the step that will be executed
	 */
	StepState(Step step) {
		super(step.getName());
		this.step = step;
	}

	@Override
	public String handle(JobFlowExecutor context) throws Exception {
		return context.executeStep(step);
	}

}