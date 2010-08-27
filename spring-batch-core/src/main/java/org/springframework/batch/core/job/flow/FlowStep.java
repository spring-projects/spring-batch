package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step;

/**
 * A {@link Step} implementation that delegates to a {@link Flow}. Useful for
 * logical grouping of steps, and especially for partitioning with multiple
 * steps per execution. If the flow has steps then when the {@link FlowStep}
 * executes, all steps including the parent {@link FlowStep} will have
 * executions in the {@link JobRepository} (one for the parent and one each for
 * the flow steps).
 * 
 * @author Dave Syer
 * 
 */
public class FlowStep extends AbstractStep {

	private Flow flow;

	/**
	 * Default constructor convenient for configuration purposes.
	 */
	public FlowStep() {
		super(null);
	}

	/**
	 * Constructor for a {@link FlowStep} that sets the flow and of the step
	 * explicitly.
	 */
	public FlowStep(Flow flow) {
		super(flow.getName());
	}

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
		Assert.state(flow != null, "A Flow must be provided");
	}

	/**
	 * Delegate to the flow provided for the execution of the step.
	 * 
	 * @see AbstractStep#doExecute(StepExecution)
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		try {
			StepHandler stepHandler = new SimpleStepHandler(getJobRepository(), stepExecution.getExecutionContext());
			FlowExecutor executor = new JobFlowExecutor(getJobRepository(), stepHandler, stepExecution.getJobExecution());
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
			stepExecution.upgradeStatus(executor.getJobExecution().getStatus());
			stepExecution.setExitStatus(executor.getJobExecution().getExitStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

}
