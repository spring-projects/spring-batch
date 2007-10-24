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
package org.springframework.batch.sample;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.execution.bootstrap.JobLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Dave Syer
 *
 */
public abstract class AbstractBatchLauncherTests extends AbstractDependencyInjectionSpringContextTests {

	protected JobLauncher launcher;
	private JobConfiguration jobConfiguration;
	
	protected ConfigurableApplicationContext createApplicationContext(
			String[] locations) {
		String[] allLocations = new String[locations.length+1];
		System.arraycopy(locations, 0, allLocations, 1, locations.length);
		allLocations[0] = "simple-container-definition.xml";
		return super.createApplicationContext(allLocations);
	}

	/**
	 * Subclasses can provide name of job to run. We guess it by looking at the
	 * unique job configuration name.
	 */
	protected String getJobName() {
		return jobConfiguration.getName();
	}

	/**
	 * @param jobConfiguration the jobConfiguration to set
	 */
	public void setJobConfiguration(JobConfiguration jobConfiguration) {
		this.jobConfiguration = jobConfiguration;
	}

	/**
	 * Public setter for the {@link JobLauncher} property.
	 *
	 * @param launcher the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}

}
