package org.springframework.integration.batch.launch;

import java.util.Set;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.integration.message.Message;

/**
 * Builds an instance of JobParameters from the properties in the message header
 * @author Jonas Partner
 *
 */
public class MessagePropertiesToJobParametersStrategy implements MessageToJobParametersStrategy {

	public JobParameters getJobParameters(Message<?> message) {
		JobParametersBuilder parametersBuilder = new JobParametersBuilder();
		Set<String> propertyNames = message.getHeader().getPropertyNames();
		for (String key : propertyNames) {
			parametersBuilder.addString(key, message.getHeader().getProperty(key));
		}
		return parametersBuilder.toJobParameters();
	}

}
