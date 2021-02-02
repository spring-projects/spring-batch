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

import org.apache.avro.Schema;

import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link AvroItemReader}.
 *
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @since 4.2
 */
public class AvroItemReaderBuilder<T> {

	private boolean saveState = true;

	private String name = AvroItemReader.class.getSimpleName();

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private Resource schema;

	private Resource resource;

	private Class<T> type;

	private boolean embeddedSchema =true;


	/**
	 * Configure a {@link Resource} containing Avro serialized objects.
	 * @param resource an existing Resource.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> resource(Resource resource) {
		Assert.notNull(resource, "A 'resource' is required.");
		Assert.state(resource.exists(), "Resource " + resource.getFilename() + " does not exist.");
		this.resource = resource;
		return this;
	}


	/**
	 * Configure an Avro {@link Schema} from a {@link Resource}.
	 * @param schema an existing schema Resource.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> schema(Resource schema) {
		Assert.notNull(schema, "A 'schema' Resource is required.");
		Assert.state(schema.exists(), "Resource " + schema.getFilename() + " does not exist.");
		this.schema = schema;
		return this;
	}

	/**
	 * Configure an Avro {@link Schema} from a String.
	 * @param schemaString the schema String.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> schema(String schemaString) {
		Assert.hasText(schemaString, "A 'schema' is required.");
		this.schema = new ByteArrayResource(schemaString.getBytes());
		return this;
	}

	/**
	 * Configure a type to be deserialized.
	 * @param type the class to be deserialized.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> type(Class<T> type) {
		Assert.notNull(type, "A 'type' is required.");
		this.type = type;
		return this;
	}

	/**
	 * Disable or enable reading an embedded Avro schema. True by default.
	 * @param embeddedSchema set to false to if the input does not contain an Avro schema.
	 * @return The current instance of the builder.   
	 */
	public AvroItemReaderBuilder<T> embeddedSchema(boolean embeddedSchema) {
		this.embeddedSchema = embeddedSchema;
		return this;
	}

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;
		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public AvroItemReaderBuilder<T> name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public AvroItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public AvroItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;
		return this;
	}


	/**
	 * Build an instance of {@link AvroItemReader}.
	 * @return the instance;
	 */
	public AvroItemReader<T> build() {
		AvroItemReader<T> avroItemReader;

		Assert.notNull(this.resource, "A 'resource' is required.");

		if (this.type != null) {
			avroItemReader = buildForType();
		}
		else {
			avroItemReader = buildForSchema();
		}

		avroItemReader.setSaveState(this.saveState);

		if(this.saveState) {
			Assert.state(StringUtils.hasText(this.name),
					"A name is required when saveState is set to true.");
		}

		avroItemReader.setName(this.name);
		avroItemReader.setCurrentItemCount(this.currentItemCount);
		avroItemReader.setMaxItemCount(this.maxItemCount);
		avroItemReader.setEmbeddedSchema(this.embeddedSchema);

		return avroItemReader;
	}


	private AvroItemReader<T> buildForType() {
		Assert.isNull(this.schema, "You cannot specify a schema and 'type'.");
		return new AvroItemReader<>(this.resource, this.type);
	}

	private AvroItemReader<T> buildForSchema() {
		Assert.notNull(this.schema, "'schema' is required.");
		return new AvroItemReader<>(this.resource, this.schema);
	}


}
