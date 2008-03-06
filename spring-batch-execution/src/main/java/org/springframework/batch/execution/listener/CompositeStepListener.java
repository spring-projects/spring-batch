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
package org.springframework.batch.execution.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Lucas Ward
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
	public ExitStatus afterStep(StepExecution stepExecution) {
		ExitStatus status = null;
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			StepListener listener = (StepListener) iterator.next();
			ExitStatus close = listener.afterStep(stepExecution);
			status = status!=null ? status.and(close): close;
		}
		return status;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#open(org.springframework.batch.core.domain.JobParameters)
	 */
	public void beforeStep(StepExecution stepExecution) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			StepListener listener = (StepListener) iterator.next();
			listener.beforeStep(stepExecution);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#onError(java.lang.Throwable)
	 */
	public ExitStatus onErrorInStep(Throwable e) {
		ExitStatus status = null;
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			StepListener listener = (StepListener) iterator.next();
			ExitStatus close = listener.onErrorInStep(e);
			status = status!=null ? status.and(close): close;
		}
		return status;
	}
}
