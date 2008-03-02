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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.execution.step.ItemOrientedStep;

/**
 * Adds listeners to {@link SimpleStepFactoryBean}.
 * 
 * @author Dave Syer
 *
 */
public class DefaultStepFactoryBean extends SimpleStepFactoryBean {

	private Object[] listeners = new Object[0];

	/**
	 * @param listeners
	 */
	public void setListeners(Object[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {
		super.applyConfiguration(step);
		step.setListeners(listeners);
	}
}
