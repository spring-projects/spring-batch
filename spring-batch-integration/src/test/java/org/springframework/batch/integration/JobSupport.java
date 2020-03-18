package org.springframework.batch.integration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.lang.Nullable;

public class JobSupport implements Job {
	
	String name;
	
	public JobSupport(String name){
		this.name = name;
	}

	public void execute(JobExecution execution) {
	}

	public String getName() {
		return name;
	}

	public boolean isRestartable() {
		return false;
	}
	
	@Nullable
	public JobParametersIncrementer getJobParametersIncrementer() {
		return null;
	}
	
	public JobParametersValidator getJobParametersValidator() {
		return new DefaultJobParametersValidator();
	}

}
