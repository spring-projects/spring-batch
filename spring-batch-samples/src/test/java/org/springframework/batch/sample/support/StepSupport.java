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
package org.springframework.batch.sample.support;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Basic no-op support implementation for use as base class for {@link Step}. Implements {@link BeanNameAware} so that
 * if no name is provided explicitly it will be inferred from the bean definition in Spring configuration.
 * 
 * @author Dave Syer
 * 
 */
public class StepSupport implements Step, BeanNameAware {

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete;

	/**
	 * Default constructor for {@link StepSupport}.
	 */
	public StepSupport() {
		super();
	}

	public StepSupport(String string) {
		super();
		this.name = string;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of the callbacks in a Spring container the
	 * name property will be set first if it is present. Care is needed with bean definition inheritance - if a parent
	 * bean has a name, then its children need an explicit name as well, otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for the shouldAllowStartIfComplete.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * Not supported but provided so that tests can easily create a step.
	 * 
	 * @throws UnsupportedOperationException always
	 * 
	 * @see org.springframework.batch.core.Step#execute(org.springframework.batch.core.StepExecution)
	 */
	public void execute(StepExecution stepExecution) throws JobInterruptedException, UnexpectedJobExecutionException {
		throw new UnsupportedOperationException(
		        "Cannot process a StepExecution.  Use a smarter subclass of StepSupport.");
	}
}
