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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanNameAware;

/**
 * Batch domain object representing a job configuration. JobConfiguration is an
 * explicit abstraction representing the configuration of a job specified by a
 * developer. It should be noted that restart policy is applied to the job as a
 * whole and not to a step.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class JobConfiguration implements BeanNameAware {

	private List stepConfigurations = new ArrayList();

	private String name;

	private boolean restartable = false;

	private int startLimit = Integer.MAX_VALUE;

	/**
	 * Default constructor.
	 */
	public JobConfiguration() {
		super();
	}

	/**
	 * Convenience constructor to immediately add name (which is mandatory but
	 * not final).
	 * 
	 * @param name
	 */
	public JobConfiguration(String name) {
		super();
		this.name = name;
	}

	/**
	 * The callback from {@link BeanNameAware} comes after the setters, so it
	 * will always overwrite the name with the bean id.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List getStepConfigurations() {
		return stepConfigurations;
	}

	public void setSteps(List stepConfigurations) {
		this.stepConfigurations.clear();
		this.stepConfigurations.addAll(stepConfigurations);
	}

	public void addStep(StepConfiguration stepConfiguration) {
		this.stepConfigurations.add(stepConfiguration);
	}

	public int getStartLimit() {
		return startLimit;
	}

	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	public boolean isRestartable() {
		return restartable;
	}
}
