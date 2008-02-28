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

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class CompositeStepListener implements StepListener {

	private List listeners = new ArrayList();

	/**
	 * Public setter for the listeners.
	 * 
	 * @param listeners
	 */
	public void setListeners(StepListener[] listeners) {
		this.listeners = Arrays.asList(listeners);
	}

	/**
	 * Public setter for the listeners. The result will be as if
	 * {@link #setListeners(StepListener[])} was called with an array of length
	 * one.
	 * 
	 * @param listener
	 */
	public void setListener(StepListener listener) {
		setListeners(new StepListener[] {listener});
	}

	/**
	 * Register additional listener.
	 * 
	 * @param stepListener
	 */
	public void register(StepListener stepListener) {
		if (!listeners.contains(stepListener)) {
			listeners.add(stepListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#close()
	 */
	public ExitStatus close() {
		ExitStatus status = null;
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			StepListener listener = (StepListener) iterator.next();
			ExitStatus close = listener.close();
			status = status!=null ? status.and(close): close;
		}
		return status;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#open(org.springframework.batch.core.domain.JobParameters)
	 */
	public void open(JobParameters jobParameters) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			StepListener listener = (StepListener) iterator.next();
			listener.open(jobParameters);
		}
	}

}
