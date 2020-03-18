/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link MongoItemWriter}
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see MongoItemWriter
 */
public class MongoItemWriterBuilder<T> {

	private MongoOperations template;

	private String collection;

	private boolean delete = false;

	/**
	 * Indicates if the items being passed to the writer are to be saved or removed from
	 * the data store. If set to false (default), the items will be saved. If set to true,
	 * the items will be removed.
	 *
	 * @param delete removal indicator
	 * @return The current instance of the builder
	 * @see MongoItemWriter#setDelete(boolean)
	 */
	public MongoItemWriterBuilder<T> delete(boolean delete) {
		this.delete = delete;

		return this;
	}

	/**
	 * Set the {@link MongoOperations} to be used to save items to be written.
	 *
	 * @param template the template implementation to be used.
	 * @return The current instance of the builder
	 * @see MongoItemWriter#setTemplate(MongoOperations)
	 */
	public MongoItemWriterBuilder<T> template(MongoOperations template) {
		this.template = template;

		return this;
	}

	/**
	 * Set the name of the Mongo collection to be written to.
	 *
	 * @param collection the name of the collection.
	 * @return The current instance of the builder
	 * @see MongoItemWriter#setCollection(String)
	 * 
	 */
	public MongoItemWriterBuilder<T> collection(String collection) {
		this.collection = collection;

		return this;
	}

	/**
	 * Validates and builds a {@link MongoItemWriter}.
	 *
	 * @return a {@link MongoItemWriter}
	 */
	public MongoItemWriter<T> build() {
		Assert.notNull(this.template, "template is required.");

		MongoItemWriter<T> writer = new MongoItemWriter<>();
		writer.setTemplate(this.template);
		writer.setDelete(this.delete);
		writer.setCollection(this.collection);

		return writer;
	}

}
