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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.springframework.batch.item.avro.AvroItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @since 4.2
 */
public class AvroItemWriterBuilder<T> {
	private Class<T> type;

	private DataFileWriter<T> dataFileWriter;

	private OutputStream outputStream;

	private DatumWriter<T> datumWriter;

	private File file;

	private Schema schema;

	private boolean embedHeader;

	private String name  = AvroItemWriter.class.getSimpleName();


	/**
	 *
	 * @param outputStream the OutputStream used to write the serialized data.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> outputStream(OutputStream outputStream) {
		Assert.notNull(outputStream, "An OutputStream is required.");
		this.outputStream = outputStream;
		return this;

	}

	/**
	 *
	 * @param file the File used to write the serialized data.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> outputFile(File file) {
		Assert.notNull(file, "A File is required.");
		this.file = file;
		return this;
	}

	/**
	 *
	 * @param schema the {@link Schema} used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(Schema schema) {
		Assert.notNull(schema, "A Schema is required.");
		this.schema = schema;
		return this;
	}

	/**
	 *
	 * @param schemaResource the Resource containing the schema JSON used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(Resource schemaResource) {
		Assert.notNull(schemaResource, "A 'schemaResource' is required.");
		Assert.state(schemaResource.exists(), "Resource " + schemaResource.getFilename() + "does not exist");
		try {
			this.schema = new Schema.Parser().parse(schemaResource.getInputStream());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return this;
	}

	/**
	 *
	 * @param schemaFile the File containing the schema JSON used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(File schemaFile) {
		Assert.notNull(schemaFile, "A 'schemaFile' is required.");
		Assert.state(schemaFile.exists(), "File " + schemaFile.getAbsolutePath() + "does not exist");
		try {
			this.schema = new Schema.Parser().parse(schemaFile);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return this;
	}

	/**
	 *
	 * @param schemaString the String containing the schema JSON used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> schema(String schemaString) {
		Assert.hasText(schemaString, "A 'schemaString' is required.");
		this.schema = new Schema.Parser().parse(schemaString);
		return this;
	}

	/**
	 *
	 * @param dataFileWriter the {@link DataFileWriter} used to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> dataFileWriter(DataFileWriter<T> dataFileWriter) {
		this.dataFileWriter = dataFileWriter;
		return this;
	}

	/**
	 *
	 * @param type the Class of objects to be serialized.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> type(Class<T> type) {
		Assert.notNull(type, "A 'type' is required.");
		this.type = type;
		return this;
	}

	/**
	 *
	 * @param datumWriter the {@link DatumWriter} to use to serialize the output.
	 * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> datumWriter(DatumWriter<T> datumWriter) {
		Assert.notNull(datumWriter, "A 'datumWriter' is required.");
		this.datumWriter = datumWriter;
		return this;
	}

	/**
	 *
	 * @param embedHeader set to true to embed an Avro schema header in the serialized output.
	 * * @return The current instance of the builder.
	 */
	public AvroItemWriterBuilder<T> embedHeader(boolean embedHeader) {
		this.embedHeader = embedHeader;
		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 *
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
	 *
	 * @return the instance;
	 */
	public AvroItemWriter<T> build() {

		AvroItemWriter<T> avroItemWriter;

		Assert.state(onlyOneOf(this.outputStream, this.file), "You cannot specify both 'outputStream' and 'fie'.");

		Assert.state(exactlyOneOf(this.outputStream, this.file), "One of 'outputStream' or 'file' is required.");

		Assert.state(onlyOneOf(this.dataFileWriter, this.type, this.datumWriter),
				"You cannot specify more than one of 'dataFileWriter', 'type', or 'datumWriter'.");

		Assert.state(exactlyOneOf(this.dataFileWriter, this.type, this.datumWriter),
				"One of dataFileWriter','type', or 'datumWriter' is required.");

		if (this.dataFileWriter != null) {
			avroItemWriter = buildWithDataFileWriter();
		}
		else if (this.datumWriter != null) {
			try {
				avroItemWriter = buildWithDatumWriter();
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		else {
			avroItemWriter = buildWithType();
		}

		if (StringUtils.hasText(this.name)) {
			avroItemWriter.setName(this.name);
		}

		return avroItemWriter;
	}

	private AvroItemWriter<T> buildWithDatumWriter() throws IOException {
		Assert.isNull(this.schema, "You cannot specify both 'datumWriter' and 'schema'.");
		return this.file != null ? new AvroItemWriter<>(new FileOutputStream(file), this.datumWriter)
				: new AvroItemWriter<>(this.outputStream, this.datumWriter);

	}

	private AvroItemWriter<T> buildWithType() {
		return this.file != null ? new AvroItemWriter<>(this.file, this.type, this.schema)
				: new AvroItemWriter<>(this.outputStream, this.type, this.schema);
	}

	private AvroItemWriter<T> buildWithDataFileWriter() {
		Assert.notNull(this.schema, " A Schema is required.");
		return this.file != null ? new AvroItemWriter<>(this.file, this.dataFileWriter, this.schema)
				: new AvroItemWriter<>(this.outputStream, this.dataFileWriter, this.schema);
	}

	private boolean onlyOneOf(Object... objects) {
		int count = 0;
		for (Object obj : objects) {
			if (obj != null) {
				count++;
			}
		}
		return count <= 1;
	}

	private boolean exactlyOneOf(Object... objects) {
		int count = 0;
		for (Object obj : objects) {
			if (obj != null) {
				count++;
			}
		}
		return count == 1;
	}

}
