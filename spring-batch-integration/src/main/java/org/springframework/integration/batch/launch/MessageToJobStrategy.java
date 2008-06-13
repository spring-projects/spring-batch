package org.springframework.integration.batch.launch;

import org.springframework.batch.core.Job;
import org.springframework.integration.message.Message;


/**
 * Interface for strategy implementations which convert from a Message to a Spring batch Job
 * @author Jonas Partner
 *
 */
public interface MessageToJobStrategy{
	
	public Job getJob(Message<?> message);

}
