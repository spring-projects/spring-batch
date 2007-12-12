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
package org.springframework.batch.execution.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.springframework.batch.core.configuration.DuplicateJobConfigurationException;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationRegistry;
import org.springframework.batch.core.configuration.ListableJobConfigurationRegistry;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.util.Assert;

/**
 * Simple map-based implementation of {@link JobConfigurationRegistry}. Access
 * to the map is synchronized, guarded by an internal lock.
 * 
 * @author Dave Syer
 * 
 */
public class MapJobConfigurationRegistry implements ListableJobConfigurationRegistry {

	private Map map = new HashMap();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationRegistry#registerJobConfiguration(org.springframework.batch.container.common.configuration.JobConfiguration)
	 */
	public void register(JobConfiguration jobConfiguration) throws DuplicateJobConfigurationException {
		Assert.notNull(jobConfiguration);
		String name = jobConfiguration.getName();
		Assert.notNull(name, "Job configuration must have a name.");
		synchronized (map) {
			if (map.containsKey(name) && jobConfiguration.equals(map.get(name))) {
				throw new DuplicateJobConfigurationException("A job configuration with this name [" + name
						+ "] was already registered");
			}
			// allow replacing job configuration with new instance
			map.put(name, jobConfiguration);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationRegistry#unregister(org.springframework.batch.container.common.configuration.JobConfiguration)
	 */
	public void unregister(JobConfiguration jobConfiguration) {
		String name = jobConfiguration.getName();
		Assert.notNull(name, "Job configuration must have a name.");
		synchronized (map) {
			map.remove(name);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationLocator#getJobConfiguration(java.lang.String)
	 */
	public JobConfiguration getJobConfiguration(String name) throws NoSuchJobConfigurationException {
		synchronized (map) {
			if (!map.containsKey(name)) {
				throw new NoSuchJobConfigurationException("No job configuration with the name [" + name
						+ "] was registered");
			}
			return (JobConfiguration) map.get(name);
		}
	}


	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.ListableJobConfigurationRegistry#getJobConfigurations()
	 */
	public Collection getJobConfigurations() {
		synchronized (map) {
			return Collections.unmodifiableCollection(new HashSet(map.values()));
		}
	}

}
