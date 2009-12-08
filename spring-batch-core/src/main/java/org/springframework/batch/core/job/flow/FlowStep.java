package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

public class FlowStep extends AbstractStep {
	
	private Flow flow;
	
	/**
	 * Public setter for the flow.
	 * 
	 * @param flow the flow to set
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}
	
	/**
	 * Ensure that the flow is set.
	 * @see AbstractStep#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(flow!=null, "A Flow must be provided");
	}

	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		try {
			StepHandler stepHandler = new SimpleStepHandler(getJobRepository());
			FlowExecutor executor = new JobFlowExecutor(stepHandler, stepExecution.getJobExecution());
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

}
