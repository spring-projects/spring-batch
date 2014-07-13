/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.item;

import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.util.ClassUtils;

/**
 * Empty method implementation of {@link ItemStream}.
 *
 * @author Dave Syer
 * @author Dean de Bree
 * @author Jimmy Praet
 */
public abstract class ItemStreamSupport implements ItemStream {

	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();
	
	private String collisionDetectionKey = ClassUtils.getShortName(getClass());

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	@Override
	public void close() {
	}

	/**
	 * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
	 */
	@Override
	public void open(ExecutionContext executionContext) {
		executionContext.putInt(collisionDetectionKey, System.identityHashCode(this));
	}

	/**
	 * Checks for ExecutionContext component name collisions.
	 * 
	 * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
	 */
	@Override
	public void update(ExecutionContext executionContext) {
		if (executionContext.containsKey(collisionDetectionKey) &&
				executionContext.getInt(collisionDetectionKey) != System.identityHashCode(this)) {
			throw new IllegalStateException(String.format("ExecutionContext key collision detected. " 
					+ "You are updating multiple ItemStream components of type '%s' within the same step, "
					+ "without specifying a unique name.", getClass().getName()));
		}
	}
	
	/**
	 * The name of the component which will be used as a stem for keys in the
	 * {@link ExecutionContext}. Subclasses should provide a default value, e.g.
	 * the short form of the class name. When using multiple components of the same
	 * class in a single step, make sure you are specifying a unique name for each component.
	 * 
	 * @param name the name for the component
	 */
	public void setName(String name) {
		this.setExecutionContextName(name);
	}

	protected void setExecutionContextName(String name) {
		executionContextUserSupport.setName(name);
		collisionDetectionKey = ClassUtils.getShortName(getClass()) + "." + name;
	}

	public String getExecutionContextKey(String key) {
		return executionContextUserSupport.getKey(key);
	}

}
