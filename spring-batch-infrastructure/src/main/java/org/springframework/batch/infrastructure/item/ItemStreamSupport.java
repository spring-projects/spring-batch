/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.util.ExecutionContextUserSupport;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.ClassUtils;

/**
 * Support class for {@link ItemStream}s. Provides a default name for components that can
 * be used as a prefix for keys in the {@link ExecutionContext} and which can be
 * overridden by the bean name if the component is a Spring managed bean.
 *
 * @author Dave Syer
 * @author Dean de Bree
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @author Jimmy Praet
 *
 */
public abstract class ItemStreamSupport implements ItemStream, BeanNameAware {

	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();

	private final String defaultName = ClassUtils.getShortName(getClass());

	private @Nullable String name;

	public ItemStreamSupport() {
		setName(defaultName);
	}

	/**
	 * The name of the component which will be used as a stem for keys in the
	 * {@link ExecutionContext}. Subclasses should provide a default value, e.g. the short
	 * form of the class name.
	 * @param name the name for the component
	 */
	public void setName(String name) {
		this.setExecutionContextName(name);
	}

	/**
	 * Set the name of the bean in the bean factory that created this bean. The bean name
	 * will only be used as name of the component in case it hasn't already been
	 * explicitly set to a value other than the default. {@link #setName(String)}
	 * @see BeanNameAware#setBeanName(String)
	 * @since 6.0
	 */
	@Override
	public void setBeanName(String name) {
		if (defaultName.equals(this.name)) {
			setName(name);
		}
	}

	/**
	 * Get the name of the component
	 * @return the name of the component
	 */
	public @Nullable String getName() {
		return executionContextUserSupport.getName();
	}

	protected void setExecutionContextName(String name) {
		this.name = name;
		executionContextUserSupport.setName(name);
	}

	public String getExecutionContextKey(String key) {
		return executionContextUserSupport.getKey(key);
	}

}
