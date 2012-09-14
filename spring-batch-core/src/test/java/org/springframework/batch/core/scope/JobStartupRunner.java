package org.springframework.batch.core.scope;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.InitializingBean;

public class JobStartupRunner implements InitializingBean {

	private Job job;

	public void setJob(Job job) {
		this.job = job;
	}

	public void afterPropertiesSet() throws Exception {
		JobExecution jobExecution = new JobExecution(11L);
		job.execute(jobExecution);
		// expect no errors
	}

}
