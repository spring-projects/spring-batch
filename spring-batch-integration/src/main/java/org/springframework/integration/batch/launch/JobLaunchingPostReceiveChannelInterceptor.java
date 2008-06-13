package org.springframework.integration.batch.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Channel interceptor which launches the configured job after the message has
 * been received
 * @author Jonas Partner
 * 
 */
public class JobLaunchingPostReceiveChannelInterceptor extends ChannelInterceptorAdapter {

	private JobLauncher jobLauncher;

	private Job job;

	private MessageToJobParametersStrategy messageToJobParametersStrategy = new MessagePropertiesToJobParametersStrategy();

	/**
	 * 
	 * @param job The job to launch
	 * @param jobLauncher
	 * @param errorHandler
	 */
	public JobLaunchingPostReceiveChannelInterceptor(Job job, JobLauncher jobLauncher) {
		super();
		this.job = job;
		this.jobLauncher = jobLauncher;
	}

	public MessageToJobParametersStrategy getMessageToJobParametersStrategy() {
		return messageToJobParametersStrategy;
	}

	public void setMessageToJobParametersStrategy(MessageToJobParametersStrategy messageToJobParametersStrategy) {
		this.messageToJobParametersStrategy = messageToJobParametersStrategy;
	}

	@Override
	public void postReceive(Message<?> message, MessageChannel channel) {
		JobParameters parameters = messageToJobParametersStrategy.getJobParameters(message);
		try {
			jobLauncher.run(job, parameters);
		}
		catch (JobExecutionException e) {
			throw new MessageHandlingException(message, "Excpetion executing job ", e);
		}

	}

}
