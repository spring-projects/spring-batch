package org.springframework.batch.integration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersIncrementer;

public class JobSupport implements Job {
	
	String name;
	
	public JobSupport(String name){
		this.name = name;
	}

	public void execute(JobExecution execution) throws JobExecutionException {
	}

	public String getName() {
		return name;
	}

	public boolean isRestartable() {
		return false;
	}
	
	public JobParametersIncrementer getJobParametersIncrementer() {
		return null;
	}

}
