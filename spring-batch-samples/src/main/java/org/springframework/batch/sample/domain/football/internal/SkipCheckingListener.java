package org.springframework.batch.sample.domain.football.internal;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class SkipCheckingListener implements StepExecutionListener {

	public ExitStatus afterStep(StepExecution stepExecution) {
		if (!stepExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new ExitStatus(false, "COMPLETED WITH SKIPS");
		} else {
			return null;
		}
	}

	public void beforeStep(StepExecution stepExecution) {
	}
}
