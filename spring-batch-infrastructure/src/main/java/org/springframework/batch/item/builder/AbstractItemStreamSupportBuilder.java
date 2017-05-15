/*
 * Copyright 2017 the original author or authors.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.builder;

import org.springframework.batch.item.ItemStreamSupport;

/*
 * Abstract superclass for builders that create streams that utilize {@link ItemStreamSupport}.
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class AbstractItemStreamSupportBuilder<T> {
	protected String name;

	protected boolean saveState = true;

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link AbstractItemStreamSupportBuilder#saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public T name(String name) {
		this.name = name;
		return (T) this;
	}

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public T saveState(boolean saveState) {
		this.saveState = saveState;
		return (T) this;
	}
}
