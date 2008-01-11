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
package org.springframework.batch.execution.runtime;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.JobInstancePropertiesBuilder;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

/**
 * @author Dave Syer
 * @author Lucas Ward
 *
 */
public class DefaultJobIdentifier extends SimpleJobIdentifier implements
		JobIdentifier {

	public static final String JOB_KEY = "job.key";

	/**
	 * Default constructor package access only.
	 */
	DefaultJobIdentifier() {
		this(null);
	}

	/**
	 * @param name the name for the job
	 */
	public DefaultJobIdentifier(String name) {
		super(name);
	}

	/**
	 * @param name the name for the job
	 */
	public DefaultJobIdentifier(String name, String key) {
		this(name, new JobInstancePropertiesBuilder().addString(JOB_KEY, key).toJobParameters());
	}
	
	public DefaultJobIdentifier(String name, JobInstanceProperties parameters){
		super(name, parameters);
	}

	public String getJobKey() {
		return getRuntimeParameters().getString(JOB_KEY);
	}
	
}
