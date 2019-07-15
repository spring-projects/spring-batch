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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;

import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link AvroItemReader}
 *
 * @author David Turanski
 * @since 4.2
 */
public class AvroItemReaderBuilder<T> {

	private boolean saveState = true;

	private String name = AvroItemReader.class.getSimpleName();

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private DatumReader<T> datumReader;

	private Schema schema;

	private File inputFile;

	private InputStream inputStream;

	private Resource resource;

	private Class<T> type;

	private boolean embeddedHeader;

	private DataFileStream<T> dataFileReader;

	/**
	 * Configure a {@link File} containing Avro serialized objects.
	 * @param inputFile an existing File.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> inputFile(File inputFile) {
		Assert.notNull(inputFile, "An File is required.");
		Assert.state(inputFile.exists(), "File " + inputFile.getName() + " does not exist.");
		this.inputFile=inputFile;
		return this;
	}

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
	 * Configure an {@link InputStream} containing Avro serialized objects.
	 * @param inputStream an existing InputStream.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> inputStream(InputStream inputStream) {
		Assert.notNull(inputStream, "An 'inputStream' is required.");
		this.inputStream = inputStream;
		return this;
	}

	public AvroItemReaderBuilder<T> schema(Schema schema) {
		Assert.notNull(schema, "A 'schema' is required.");
		this.schema = schema;
		return this;
	}


	/**
	 * Configure an Avro {@link Schema} from a {@link Resource}.
	 * @param schemaResource an existing schema Resource.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> schema(Resource schemaResource) {
		Assert.notNull(schemaResource, "A 'schemaResource' is required.");
		Assert.state(schemaResource.exists(), "Resource " + schemaResource.getFilename() + " does not exist.");
		try {
			return schema(schemaResource.getFile());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Configure an Avro {@link Schema} from a String.
	 * @param schemaString the schema String.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> schema(String schemaString) {
		Assert.hasText(schemaString, "A 'schema' is required.");
		try {
			this.schema = new Schema.Parser().parse(schemaString);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return this;
	}

	/**
	 * Configure an Avro {@link Schema} from a File.
	 * @param schemaFile an existing schema File.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> schema(File schemaFile) {
		Assert.notNull(schemaFile, "A 'schemaFile' is required.");
		Assert.state(schemaFile.exists(), "File " + schemaFile.getAbsolutePath() + "does not exist.");
		try {
			this.schema = new Schema.Parser().parse(schemaFile);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return this;
	}

	/**
	 * Configure a type to be deserialized.
	 * @param type the class.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> type(Class<T> type) {
		Assert.notNull(type, "A 'type' is required.");
		this.type = type;
		return this;
	}

	/**
	 *
	 * @param embeddedHeader set to true if input contains an embedded Schema header.
	 * This is the case if it was created by a {@link org.apache.avro.file.DataFileWriter}
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> embeddedHeader(boolean embeddedHeader) {
		this.embeddedHeader = embeddedHeader;
		return this;
	}

	/**
	 * Configure a {@link DatumReader}.
	 *
	 * @param datumReader the DatumReader.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> datumReader(DatumReader<T> datumReader) {
		Assert.notNull(datumReader, "A 'datumReader' is required.");
		this.datumReader = datumReader;
		return this;
	}

	/**
	 * Configure a {@link DataFileStream}, normally a {@link DataFileReader}. No other properties are required.
	 *
	 * @param dataFileReader the DataFileReader.
	 * @return The current instance of the builder.
	 */
	public AvroItemReaderBuilder<T> dataFileReader(DataFileStream<T> dataFileReader) {
		Assert.notNull(dataFileReader, "A 'dataFileReader' is required.");
		this.dataFileReader = dataFileReader;
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

		if (this.dataFileReader != null) {
			avroItemReader = buildWithDataFileReader();
		}

		else {
			Assert.state(onlyOneOf(this.inputStream, this.inputFile, this.resource),
					"You cannot specify more than one of 'inputStream', 'resource', and 'inputFile'.");

			Assert.state(exactlyOneOf(this.inputStream, this.inputFile, this.resource),
					"One of 'inputStream', 'resource', or 'inputFile' is required.");


			if (this.type != null) {
				avroItemReader = buildForType();

			}
			else if (this.datumReader != null) {
				Assert.isNull(this.schema, "You cannot specify a Schema and a DatumReader.");
				avroItemReader = buildForDatumReader(this.datumReader);
			}
			else {
				avroItemReader = buildForGenericDatumReader();
			}
		}

		avroItemReader.setSaveState(this.saveState);

		if(this.saveState) {
			Assert.state(StringUtils.hasText(this.name),
					"A name is required when saveState is set to true.");
		}

		avroItemReader.setName(this.name);
		avroItemReader.setCurrentItemCount(this.currentItemCount);
		avroItemReader.setMaxItemCount(this.maxItemCount);
		avroItemReader.setEmbeddedHeader(this.embeddedHeader);

		return avroItemReader;
	}



	private AvroItemReader<T> buildWithDataFileReader() {
		Assert.isNull(this.schema, "You cannot specify a Schema and a DataFileReader.");
		Assert.isNull(this.type, "You cannot specify a type and a DataFileReader.");
		Assert.isNull(this.datumReader, "You cannot specify a DatumReader and a DataFileReader.");
		Assert.isNull(this.inputStream, "You cannot specify an InputStream and a DataFileReader.");
		Assert.isNull(this.inputFile, "You cannot specify a File and a DataFileReader.");
		Assert.isNull(this.resource, "You cannot specify a Resource and a DataFileReader.");
		return new AvroItemReader<T>(dataFileReader);
	}

	private AvroItemReader<T> buildForType() {
		Assert.isNull(this.datumReader,
				"You cannot specify a DatumReader and a 'type'");
		Assert.isNull(this.schema, "You cannot specify a schema and 'type'.");

		if (this.inputFile != null) {
			return new AvroItemReader<>(this.inputFile, this.type);
		} else if (this.resource != null) {
			return new AvroItemReader<>(this.resource, this.type);
		}
		return new AvroItemReader<>(this.inputStream, this.type);
	}

	private AvroItemReader<T> buildForGenericDatumReader() {
		Assert.notNull(this.schema, "'schema' is required.");
		return buildForDatumReader(new GenericDatumReader<>(schema));
	}

	private AvroItemReader<T> buildForDatumReader(DatumReader<T> datumReader) {
		if (this.inputStream == null) {
			try {
				if (this.inputFile != null) {
					this.inputStream = new FileInputStream(this.inputFile);
				} else {
					this.inputStream = this.resource.getInputStream();
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}
		return new AvroItemReader<>(this.inputStream, datumReader);
	}


	private boolean onlyOneOf(Object ... objects) {
		int count = 0;
		for (Object obj: objects) {
			if (obj != null) {
				count++;
			}
		}
		return count <= 1;
	}

	private boolean exactlyOneOf(Object ... objects) {
		int count = 0;
		for (Object obj: objects) {
			if (obj != null) {
				count++;
			}
		}
		return count == 1;
	}

}
