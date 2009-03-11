package org.springframework.batch.sample.common;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class SkipCheckingListener {

	@AfterStep
	public ExitStatus checkForSkips(StepExecution stepExecution) {
		if (!stepExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new ExitStatus("COMPLETED WITH SKIPS");
		}
		else {
			return null;
		}
	}

	@BeforeStep
	public void saveStepName(StepExecution stepExecution) {
		stepExecution.getExecutionContext().put("stepName", stepExecution.getStepName());
	}
}
