package org.springframework.batch.core.job.flow;

import org.springframework.batch.flow.AbstractState;

/**
 * @author Dave Syer
 * 
 */
public class DecisionState extends AbstractState<JobFlowExecutor> {

	private final JobExecutionDecider decider;

	/**
	 * @param name
	 */
	DecisionState(JobExecutionDecider decider, String name) {
		super(name);
		this.decider = decider;
	}

	@Override
	public String handle(JobFlowExecutor context) throws Exception {
		return decider.decide(context.getJobExecution());
	}

}