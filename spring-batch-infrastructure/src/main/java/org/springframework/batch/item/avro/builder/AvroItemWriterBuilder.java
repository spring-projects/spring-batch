/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.batch.item.avro.builder;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.avro.AvroItemWriter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link AvroItemWriter}.
 *
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 4.2
 */
public class AvroItemWriterBuilder<T> {

	private @Nullable Class<T> type;

	private @Nullable WritableResource resource;

	private @Nullable Resource schema;

	private String name = AvroItemWriter.class.getSimpleName();

	/**
	 * @param resource the {@link WritableResource} used to write the serialized data.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> resource(WritableResource resource) {
		Assert.notNull(resource, "A 'resource' is required.");
		this.resource = resource;
		return this;
	}

	/**
	 * @param schema the Resource containing the schema JSON used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(Resource schema) {
		Assert.notNull(schema, "A 'schema' is required.");
		Assert.state(schema.exists(), "Resource " + schema.getFilename() + "does not exist.");
		this.schema = schema;
		return this;
	}

	/**
	 * @param schemaString the String containing the schema JSON used to serialize the
	 * output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(String schemaString) {
		Assert.hasText(schemaString, "A 'schemaString' is required.");
		this.schema = new ByteArrayResource(schemaString.getBytes());
		return this;
	}

	/**
	 * @param type the Class of objects to be serialized.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> type(Class<T> type) {
		Assert.notNull(type, "A 'type' is required.");
		this.type = type;
		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public AvroItemWriterBuilder<T> name(String name) {
		Assert.hasText(name, "A 'name' is required.");
		this.name = name;
		return this;
	}

	/**
	 * Build an instance of {@link AvroItemWriter}.
	 * @return the instance;
	 */
	public AvroItemWriter<T> build() {

		Assert.notNull(this.resource, "A 'resource' is required.");

		Assert.notNull(this.type, "A 'type' is required.");

		AvroItemWriter<T> avroItemWriter = this.schema != null
				? new AvroItemWriter<>(this.resource, this.schema, this.type)
				: new AvroItemWriter<>(this.resource, this.type);
		avroItemWriter.setName(this.name);
		return avroItemWriter;
	}

}
