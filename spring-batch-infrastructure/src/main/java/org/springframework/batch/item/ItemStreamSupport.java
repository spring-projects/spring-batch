/*
 * Copyright 2006-2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.util.ExecutionContextUserSupport;

/**
 * Empty method implementation of {@link ItemStream}.
 *
 * @author Dave Syer
 * @author Dean de Bree
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 *
 */
public abstract class ItemStreamSupport implements ItemStream {

	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();

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
	public @Nullable String getName() {
		return executionContextUserSupport.getName();
	}

	protected void setExecutionContextName(String name) {
		executionContextUserSupport.setName(name);
	}

	public String getExecutionContextKey(String key) {
		return executionContextUserSupport.getKey(key);
	}

}
