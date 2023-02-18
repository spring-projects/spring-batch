/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.item;

import org.springframework.batch.item.util.ExecutionContextUserSupport;

/**
 * Empty method implementation of {@link ItemStream}.
 *
 * @author Dave Syer
 * @author Dean de Bree
 * @author Mahmoud Ben Hassine
 *
 */
public abstract class ItemStreamSupport implements ItemStream {

	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#close()
	 * @deprecated since 5.0 in favor of {@link ItemStream#close()}. Scheduled for removal
	 * in 5.2.
	 */
	@Deprecated(since = "5.0", forRemoval = true)
	@Override
	public void close() {
	}

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
	 * @deprecated since 5.0 in favor of {@link ItemStream#open(ExecutionContext)} ()}.
	 * Scheduled for removal in 5.2.
	 */
	@Override
	@Deprecated(since = "5.0", forRemoval = true)
	public void open(ExecutionContext executionContext) {
	}

	/**
	 * Return empty {@link ExecutionContext}.
	 * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
	 * @deprecated since 5.0 in favor of {@link ItemStream#update(ExecutionContext)} ()}.
	 * Scheduled for removal in 5.2.
	 */
	@Override
	@Deprecated(since = "5.0", forRemoval = true)
	public void update(ExecutionContext executionContext) {
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
	 * Get the name of the component
	 * @return the name of the component
	 */
	public String getName() {
		return executionContextUserSupport.getName();
	}

	protected void setExecutionContextName(String name) {
		executionContextUserSupport.setName(name);
	}

	public String getExecutionContextKey(String key) {
		return executionContextUserSupport.getKey(key);
	}

}
