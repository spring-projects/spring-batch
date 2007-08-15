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
package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.beans.factory.BeanNameAware;

/**
 * A {@link StepConfiguration} implementation that provides common behaviour to
 * subclasses. Implements {@link BeanNameAware} so that if no name is provided
 * explicitly it will be inferred from the bean definition in Spring
 * configuration.
 * 
 * @author Dave Syer
 * 
 */
public class AbstractStepConfiguration extends StepConfigurationSupport implements BeanNameAware {

	private int skipLimit = 0;

	private boolean saveRestartData = false;

	private ExceptionHandler exceptionHandler;

	/**
	 * Default constructor.
	 */
	public AbstractStepConfiguration() {
		super();
	}

	/**
	 * Convenent constructor for setting only the name property.
	 * @param name
	 */
	public AbstractStepConfiguration(String name) {
		super(name);
	}

	/**
	 * Set the name property if it has not already been set explicitly (and is
	 * therefore not null).
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (getName() == null) {
			setName(name);
		}
	}

	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	public int getSkipLimit() {
		return skipLimit;
	}

	public void setSaveRestartData(boolean saveRestartData) {
		this.saveRestartData = saveRestartData;
	}

	public boolean isSaveRestartData() {
		return saveRestartData;
	}

}