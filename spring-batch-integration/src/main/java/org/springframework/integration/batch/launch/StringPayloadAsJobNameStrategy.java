package org.springframework.integration.batch.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Takes the string payload of a message and delegates to a JobLocator
 * @author Jonas Partner
 *
 */
public class StringPayloadAsJobNameStrategy implements MessageToJobStrategy{
	
	private JobLocator jobLocator;
	
	public StringPayloadAsJobNameStrategy(JobLocator jobLocator){
		this.jobLocator = jobLocator;
	}

	public Job getJob(Message<?> message) {
		String name = (String)message.getPayload();
		try {
			return jobLocator.getJob(name);
		}
		catch (NoSuchJobException e) {
			throw new MessageHandlingException(message, "Could not find job with name " + name, e);
		}
	}

}
