/*
 * Copyright 2017-2025 the original author or authors.
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
import org.springframework.batch.item.data.MongoItemWriter.Mode;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link MongoItemWriter}
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 4.0
 * @see MongoItemWriter
 */
public class MongoItemWriterBuilder<T> {

	private MongoOperations template;

	private String collection;

	private Mode mode = Mode.UPSERT;

	/**
	 * Set the operating {@link Mode} to be applied by this writer. Defaults to
	 * {@link Mode#UPSERT}.
	 * @param mode the mode to be used.
	 * @return The current instance of the builder
	 * @see MongoItemWriter#setMode(Mode)
	 * @since 5.1
	 */
	public MongoItemWriterBuilder<T> mode(final Mode mode) {
		this.mode = mode;

		return this;
	}

	/**
	 * Set the {@link MongoOperations} to be used to save items to be written.
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
	 * @return a {@link MongoItemWriter}
	 */
	public MongoItemWriter<T> build() {
		Assert.notNull(this.template, "template is required.");

		MongoItemWriter<T> writer = new MongoItemWriter<>();
		writer.setTemplate(this.template);
		writer.setMode(this.mode);
		writer.setCollection(this.collection);

		return writer;
	}

}
