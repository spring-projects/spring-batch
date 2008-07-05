package org.springframework.batch.integration.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.annotation.Handler;

/**
 * Message handler which uses strategies to convert a Message into a job and a
 * set of job parameters
 * @author Jonas Partner
 * @author Dave Syer
 * 
 */
public class JobLaunchingMessageHandler {

	private final JobLauncher jobLauncher;

	/**
	 * @param jobLauncher
	 */
	public JobLaunchingMessageHandler(JobLauncher jobLauncher) {
		super();
		this.jobLauncher = jobLauncher;
	}

	@Handler
	public JobExecution launch(JobLaunchRequest request) {
		Job job = request.getJob();
		JobParameters jobParameters = request.getJobParameters();

		try {
			JobExecution execution = jobLauncher.run(job, jobParameters);
			return execution;
		}
		catch (JobExecutionException e) {
			throw new UnexpectedJobExecutionException("Exception executing job: ["+request+"]", e);
		}
	}

}
