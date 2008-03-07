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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.repository.DuplicateJobException;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.util.Assert;

/**
 * Simple map-based implementation of {@link JobRegistry}. Access to the map is
 * synchronized, guarded by an internal lock.
 * 
 * @author Dave Syer
 * 
 */
public class MapJobRegistry implements ListableJobRegistry {

	private Map map = new HashMap();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationRegistry#registerJobConfiguration(org.springframework.batch.container.common.configuration.JobConfiguration)
	 */
	public void register(JobFactory jobFactory) throws DuplicateJobException {
		Assert.notNull(jobFactory);
		String name = jobFactory.getJobName();
		Assert.notNull(name, "Job configuration must have a name.");
		synchronized (map) {
			if (map.containsKey(name)) {
				throw new DuplicateJobException("A job configuration with this name [" + name
						+ "] was already registered");
			}
			map.put(name, jobFactory);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationRegistry#unregister(org.springframework.batch.container.common.configuration.JobConfiguration)
	 */
	public void unregister(String name) {
		Assert.notNull(name, "Job configuration must have a name.");
		synchronized (map) {
			map.remove(name);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.JobConfigurationLocator#getJobConfiguration(java.lang.String)
	 */
	public Job getJob(String name) throws NoSuchJobException {
		synchronized (map) {
			if (!map.containsKey(name)) {
				throw new NoSuchJobException("No job configuration with the name [" + name + "] was registered");
			}
			return (Job) ((JobFactory) map.get(name)).createJob();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.configuration.ListableJobConfigurationRegistry#getJobConfigurations()
	 */
	public Collection getJobNames() {
		synchronized (map) {
			return Collections.unmodifiableCollection(new HashSet(map.keySet()));
		}
	}

}
