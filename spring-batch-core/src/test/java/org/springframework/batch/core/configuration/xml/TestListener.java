package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.repeat.ExitStatus;

public class TestListener extends AbstractTestComponent implements StepExecutionListener {

	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	public void beforeStep(StepExecution stepExecution) {
		executed = true;
	}

}
