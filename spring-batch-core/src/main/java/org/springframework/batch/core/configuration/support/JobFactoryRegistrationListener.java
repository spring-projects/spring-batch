package org.springframework.batch.core.configuration.support;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;

/**
 * Generic service that can bind and unbind a {@link JobFactory} in a
 * {@link JobRegistry}.
 * 
 * @author Dave Syer
 * 
 */
public class JobFactoryRegistrationListener {

	private Log logger = LogFactory.getLog(getClass());

	private JobRegistry jobRegistry;

	/**
	 * Public setter for a {@link JobRegistry} to use for all the bind and
	 * unbind events.
	 * 
	 * @param jobRegistry {@link JobRegistry}
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Take the {@link JobFactory} provided and register it with the
	 * {@link JobRegistry}.
	 * @param jobFactory a {@link JobFactory}
	 * @param params not needed by this listener.
	 * @throws Exception if there is a problem
	 */
	public void bind(JobFactory jobFactory, Map<String, ?> params) throws Exception {
		logger.info("Binding JobFactory: " + jobFactory.getJobName());
		jobRegistry.register(jobFactory);
	}

	/**
	 * Take the {@link JobFactory} provided and unregister it with the
	 * {@link JobRegistry}.
	 * @param jobFactory a {@link JobFactory}
	 * @param params not needed by this listener.
	 * @throws Exception if there is a problem
	 */
	public void unbind(JobFactory jobFactory, Map<String, ?> params) throws Exception {
		logger.info("Unbinding JobFactory: " + jobFactory.getJobName());
		jobRegistry.unregister(jobFactory.getJobName());
	}

}
