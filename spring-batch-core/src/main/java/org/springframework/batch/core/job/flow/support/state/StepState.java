package org.springframework.batch.core.job.flow.support.state;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.support.State;

/**
 * {@link State} implementation that delegates to a {@link FlowExecutor} to
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
	public String handle(FlowExecutor executor) throws Exception {
		return executor.executeStep(step);
	}

}