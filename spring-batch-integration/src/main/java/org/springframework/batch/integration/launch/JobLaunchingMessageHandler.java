package org.springframework.batch.integration.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.annotation.Handler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Message handler which uses strategies to convert a Message into a job and a
 * set of job parameters
 * @author Jonas Partner
 * @author Dave Syer
 * 
 */
public class JobLaunchingMessageHandler {

	private MessageToJobStrategy messageToJobStrategy;

	private MessageToJobParametersStrategy messageToJobParametersStrategy = new MessagePropertiesToJobParametersStrategy();

	private JobLauncher jobLauncher;

	public JobLaunchingMessageHandler(JobLauncher jobLauncher, MessageToJobStrategy messageToJobStrategy) {
		super();
		this.jobLauncher = jobLauncher;
		this.messageToJobStrategy = messageToJobStrategy;
	}

	@Handler
	public JobExecution handle(Message<?> message) {
		Job job = messageToJobStrategy.getJob(message);
		JobParameters jobParameters = messageToJobParametersStrategy.getJobParameters(message);

		try {
			JobExecution execution = jobLauncher.run(job, jobParameters);
			if (message.getHeader().getReturnAddress() != null) {
				return execution;
			}
			return null;
		}
		catch (JobExecutionException e) {
			throw new MessageHandlingException(message, "Exception executing job ");
		}
	}

	public void setMessageToJobParametersStrategy(MessageToJobParametersStrategy messageToJobParametersStrategy) {
		this.messageToJobParametersStrategy = messageToJobParametersStrategy;
	}

}
