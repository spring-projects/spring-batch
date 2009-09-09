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

/**
 * A {@link JobFactory} that just keeps a reference to a {@link Job}. It never
 * modifies its {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public class ReferenceJobFactory implements JobFactory {

	private Job job;

	/**
	 * @param job the {@link Job} to return from {@link #createJob()}.
	 */
	public ReferenceJobFactory(Job job) {
		this.job = job;
	}

	/**
	 * Just return the instance passed in on initialization.
	 * 
	 * @see JobFactory#createJob()
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
