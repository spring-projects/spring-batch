package org.springframework.batch.sample.common;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class SkipCheckingDecider implements JobExecutionDecider {

	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
		if (!stepExecution.getExitStatus().getExitCode().equals(
				ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new FlowExecutionStatus("COMPLETED WITH SKIPS");
		} else {
			return new FlowExecutionStatus(ExitStatus.COMPLETED.getExitCode());
		}
	}
}