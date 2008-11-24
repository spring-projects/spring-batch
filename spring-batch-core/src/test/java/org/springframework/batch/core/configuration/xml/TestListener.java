package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterRead;

public class TestListener extends AbstractTestComponent implements StepExecutionListener {

	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	public void beforeStep(StepExecution stepExecution) {
		executed = true;
	}
	
	public void destroy(){
		
	}
	
	@AfterRead
	public void logItem(){
		
	}

}
