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
package org.springframework.batch.core.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobListener;
import org.springframework.batch.core.domain.StepListener;

/**
 * @author Dave Syer
 * 
 */
public class CompositeJobListener implements JobListener {

	private List listeners = new ArrayList();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param listeners
	 */
	public void setListeners(JobListener[] listeners) {
		this.listeners = Arrays.asList(listeners);
	}

	/**
	 * Public setter for the listeners. The result will be as if
	 * {@link #setListeners(StepListener[])} was called with an array of length
	 * one.
	 * 
	 * @param listener
	 */
	public void setListener(JobListener listener) {
		setListeners(new JobListener[] {listener});
	}

	/**
	 * Register additional listener.
	 * 
	 * @param stepListener
	 */
	public void register(JobListener stepListener) {
		if (!listeners.contains(stepListener)) {
			listeners.add(stepListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#close()
	 */
	public void afterJob() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JobListener listener = (JobListener) iterator.next();
			listener.afterJob();
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#open(org.springframework.batch.core.domain.JobParameters)
	 */
	public void beforeJob(JobExecution jobExecution) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JobListener listener = (JobListener) iterator.next();
			listener.beforeJob(jobExecution);
		}
	}

}
