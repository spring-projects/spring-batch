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
package org.springframework.batch.execution.facade;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.JobExecutionContext;
import org.springframework.batch.core.runtime.JobExecutionRegistry;
import org.springframework.batch.core.runtime.JobIdentifier;

/**
 * Simple in-memory implementation of {@link JobExecutionRegistry}.
 * Synchronizes all access to the underlying storage. Good for most purposes.
 * 
 * @author Dave Syer
 * 
 */
public class VolatileJobExecutionRegistry implements JobExecutionRegistry {

	private Map contexts = new HashMap();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#findByName(java.lang.String)
	 */
	public Collection findByName(String name) {
		Set values = new HashSet();
		HashMap contexts;
		synchronized (this.contexts) {
			contexts = new HashMap(this.contexts);
		}
		for (Iterator iter = contexts.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String runtimeName = ((JobIdentifier) entry.getKey()).getName();
			if ((name == null && runtimeName == null) || name.equals(runtimeName)) {
				values.add(entry.getValue());
			}
		}
		return values;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#findAll()
	 */
	public Collection findAll() {

		synchronized (this.contexts) {
			return new HashSet(contexts.values());
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#findByRuntimeInformation(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public JobExecutionContext get(JobIdentifier runtimeInformation) {

		synchronized (this.contexts) {
			return (JobExecutionContext) contexts.get(runtimeInformation);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#isRegistered(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public boolean isRegistered(JobIdentifier runtimeInformation) {

		synchronized (this.contexts) {
			return contexts.containsKey(runtimeInformation);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#register(org.springframework.batch.container.common.runtime.JobRuntimeInformation,
	 * org.springframework.batch.container.common.domain.JobExecution)
	 */
	public JobExecutionContext register(JobIdentifier jobIdentifier, JobInstance job) {
		if (isRegistered(jobIdentifier)) {
			return get(jobIdentifier);
		}
		JobExecutionContext context = new JobExecutionContext(jobIdentifier, job);

		synchronized (this.contexts) {
			contexts.put(jobIdentifier, context);
		}
		
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.common.executor.JobExecutionRegistry#unregister(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public void unregister(JobIdentifier runtimeInformation) {

		synchronized (this.contexts) {
			contexts.remove(runtimeInformation);
		}

	}

}
