package org.springframework.batch.core.scope;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

public class JobStartupRunner {

	private Step step;
	
	public void setStep(Step step) {
		this.step = step;
	}
	
	@PostConstruct
	public void launch() throws Exception {
		StepExecution stepExecution = new StepExecution("step", new JobExecution(1L), 0L);
		step.execute(stepExecution);
		// expect no errors
	}

}
