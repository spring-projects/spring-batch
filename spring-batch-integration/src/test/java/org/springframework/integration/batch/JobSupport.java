package org.springframework.integration.batch;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;

public class JobSupport implements Job {
	
	String name;
	
	public JobSupport(String name){
		this.name = name;
	}

	public void execute(JobExecution execution) throws JobExecutionException {
		// TODO Auto-generated method stub
	}

	public String getName() {
		return name;
	}

	@SuppressWarnings("unchecked")
	public List getSteps() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRestartable() {
		// TODO Auto-generated method stub
		return false;
	}

}
