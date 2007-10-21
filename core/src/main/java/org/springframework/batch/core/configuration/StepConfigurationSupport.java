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
package org.springframework.batch.core.configuration;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Basic no-op support implementation for use as base class for
 * {@link StepConfiguration}.
 * 
 * @author Dave Syer
 * 
 */
public class StepConfigurationSupport implements StepConfiguration,
		BeanNameAware {

	private String name;
	private int startLimit = Integer.MAX_VALUE;
	private Tasklet tasklet;
	private boolean allowStartIfComplete;
	private boolean saveRestartData = false;

	/**
	 * Default constructor for {@link StepConfigurationSupport}.
	 */
	public StepConfigurationSupport() {
		super();
	}

	/**
	 * @param string
	 */
	public StepConfigurationSupport(String string) {
		super();
		this.name = string;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.configuration.StepConfiguration#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property. Always overrides the default value if this
	 * object is a Spring bean.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.configuration.StepConfiguration#getStartLimit()
	 */
	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit
	 *            the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.configuration.StepConfiguration#getTasklet()
	 */
	public Tasklet getTasklet() {
		return this.tasklet;
	}

	/**
	 * Public setter for the tasklet.
	 * 
	 * @param tasklet
	 *            the tasklet to set
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.configuration.StepConfiguration#shouldAllowStartIfComplete()
	 */
	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for the shouldAllowStartIfComplete.
	 * 
	 * @param allowStartIfComplete
	 *            the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	public void setSaveRestartData(boolean saveRestartData) {
		this.saveRestartData = saveRestartData;
	}

	public boolean isSaveRestartData() {
		return saveRestartData;
	}

}
