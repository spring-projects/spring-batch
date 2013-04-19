package org.springframework.batch.integration.retry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;

/**
 * @author Dave Syer
 *
 */
public final class SimpleRecoverer implements MethodInvocationRecoverer<String> {

	private Log logger = LogFactory.getLog(getClass());

	private final List<String> recovered = new ArrayList<String>();

	/**
	 * Public getter for the recovered.
	 * @return the recovered
	 */
	public List<String> getRecovered() {
		return recovered;
	}

	public String recover(Object[] data, Throwable cause) {
		if (data == null) {
			return null;
		}
		String payload = (String) data[0];
		logger.debug("Recovering: " + payload);
		recovered.add(payload);
		return null;
	}
}