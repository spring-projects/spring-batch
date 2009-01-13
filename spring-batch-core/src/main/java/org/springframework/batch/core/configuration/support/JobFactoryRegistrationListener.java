/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
