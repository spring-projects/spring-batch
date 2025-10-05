package org.springframework.batch.integration.partition;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * A {@link MessageEndpoint} that can handle a {@link StepExecutionRequest} and return a
 * {@link StepExecution} as the result. Typically these need to be aggregated into a
 * response to a partition handler.
 *
 * @author Dave Syer
 *
 */
@MessageEndpoint
public class StepExecutionRequestHandler {

	private JobRepository jobRepository;

	private StepLocator stepLocator;

	/**
	 * Used to locate a {@link Step} to execute for each request.
	 * @param stepLocator a {@link StepLocator}
	 */
	public void setStepLocator(StepLocator stepLocator) {
		this.stepLocator = stepLocator;
	}

	/**
	 * A job repository that should be used to check for {@link StepExecution} completion.
	 * @param jobRepository a {@link JobRepository} that is linked to the shared
	 * repository used by all remote workers.
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@ServiceActivator
	public StepExecution handle(StepExecutionRequest request) {

		Long stepExecutionId = request.getStepExecutionId();
		StepExecution stepExecution = jobRepository.getStepExecution(stepExecutionId);
		if (stepExecution == null) {
			throw new RuntimeException("No StepExecution could be located for this request: " + request);
		}

		String stepName = request.getStepName();
		Step step = stepLocator.getStep(stepName);
		if (step == null) {
			throw new RuntimeException(String.format("No Step with name [%s] could be located.", stepName));
		}

		try {
			step.execute(stepExecution);
		}
		catch (JobInterruptedException e) {
			stepExecution.setStatus(BatchStatus.STOPPED);
			// The receiver should update the stepExecution in repository
		}
		catch (Throwable e) {
			stepExecution.addFailureException(e);
			stepExecution.setStatus(BatchStatus.FAILED);
			// The receiver should update the stepExecution in repository
		}

		return stepExecution;

	}

}
