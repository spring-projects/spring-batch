package org.springframework.batch.integration.retry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

@MessageEndpoint
public class SimpleService implements Service  {

	private Log logger = LogFactory.getLog(getClass());

	private List<String> processed = new CopyOnWriteArrayList<String>();

	private int count = 0;
	
	/**
	 * Public getter for the processed.
	 * @return the processed
	 */
	public List<String> getProcessed() {
		return processed;
	}

	@ServiceActivator(inputChannel = "requests", outputChannel = "replies")
	public String process(String message) {
		String result = message + ": " + (count++);
		logger.debug("Handling: " + message);
		processed.add(message);
		if ("fail".equals(message)) {
			throw new RuntimeException("Planned failure");
		}
		return result;
	}

}
