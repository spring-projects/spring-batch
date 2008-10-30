package org.springframework.batch.core.job.flow.support.state;

import org.springframework.batch.core.job.flow.FlowExecutor;


/**
 * @author Dave Syer
 * 
 */
public class DecisionState extends AbstractState {

	private final JobExecutionDecider decider;

	/**
	 * @param name
	 */
	public DecisionState(JobExecutionDecider decider, String name) {
		super(name);
		this.decider = decider;
	}

	@Override
	public String handle(FlowExecutor executor) throws Exception {
		return decider.decide(executor.getJobExecution(), executor.getStepExecution());
	}

}