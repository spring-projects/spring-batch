/*
 * Copyright 2009 the original author or authors.
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
package org.springframework.batch.sample.common;

/**
 * Item wrapper useful in "process indicator" usecase, where input is marked as
 * processed by the processor/writer. This requires passing a technical
 * identifier of the input data so that it can be modified in later stages.
 * 
 * @param <T> item type
 * 
 * @see StagingItemReader
 * @see StagingItemProcessor
 * 
 * @author Robert Kasanicky
 */
public class ProcessIndicatorItemWrapper<T> {

	private long id;

	private T item;

	public ProcessIndicatorItemWrapper(long id, T item) {
		this.id = id;
		this.item = item;
	}

	/**
	 * @return id identifying the input data (typically row in database)
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return item (domain object for business processing)
	 */
	public T getItem() {
		return item;
	}
}
