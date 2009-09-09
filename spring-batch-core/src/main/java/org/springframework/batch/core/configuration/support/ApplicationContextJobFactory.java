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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A {@link JobFactory} that creates its own {@link ApplicationContext} and
 * pulls a bean out when asked to create a {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public class ApplicationContextJobFactory implements JobFactory {

	private final Job job;

	/**
	 * @param jobName the id of the {@link Job} in the application context to be
	 * created
	 * @param applicationContextFactory a factory for an application context
	 * containing a job with the job name provided
	 */
	public ApplicationContextJobFactory(String jobName, ApplicationContextFactory applicationContextFactory) {
		ConfigurableApplicationContext context = applicationContextFactory.createApplicationContext();
		this.job = (Job) context.getBean(jobName, Job.class);
	}

	/**
	 * Create an {@link ApplicationContext} from the factory provided and pull
	 * out a bean with the name given during initialization.
	 * 
	 * @see org.springframework.batch.core.configuration.JobFactory#createJob()
	 */
	public final Job createJob() {
		return job;
	}
	
	/**
	 * Just return the name of instance passed in on initialization.
	 * 
	 * @see JobFactory#getJobName()
	 */
	public String getJobName() {
		return job.getName();
	}

}
