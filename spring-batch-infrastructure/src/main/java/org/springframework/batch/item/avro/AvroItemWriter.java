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

package org.springframework.batch.item.avro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} that serializes data to an OutputStream or File using Avro.
 *
 * Injecting a {@link DataFileWriter} or a {@link Schema} will result in a Schema header embedded the serialized output.
 *
 * To exclude the Schema header, use a constructor which provides a {@link DatumWriter} or a Class and no Schema.
 *
 * @since 4.2
 * @author David Turanski
 */
public class AvroItemWriter<T> extends AbstractItemStreamItemWriter<T> {

	private DataFileWriter<T> dataFileWriter;

	private OutputStreamWriter<T> outputStreamWriter;

	/**
	 *
	 * @param outputStream the OutputStream where the serialized data will be written.
	 * @param dataFileWriter the {@link DataFileWriter} used to serialize objects.
	 * @param schema the schema used to serialize the objects.
	 */
	public AvroItemWriter(OutputStream outputStream, DataFileWriter<T> dataFileWriter, Schema schema) {
		this.avroItemWriter(dataFileWriter, outputStream, schema);
	}

	/**
	 *
	 * @param outputStream the OutputStream where the serialized data will be written.
	 * @param schema a {@link Resource} containing the schema used to serialize the objects.
	 * @param dataFileWriter the {@link DataFileWriter} used to serialize objects.
	 */
	public AvroItemWriter(OutputStream outputStream, DataFileWriter<T> dataFileWriter, Resource schema) {
		Assert.notNull(dataFileWriter, "'dataFileWriter' is required");
		Assert.notNull(outputStream, "'outputStream' is required");
		Assert.notNull(schema, "'schema' is required");
		Assert.state(schema.exists(), "'schema' " + schema.getFilename() + " does not exist");

		try {
			this.avroItemWriter(dataFileWriter, outputStream, new Schema.Parser().parse(schema.getInputStream()));
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param file the File where the serialized data will be written.
	 * @param dataFileWriter the {@link DataFileWriter} used to serialize objects.
	 * @param schema the schema used to serialize the objects.
	 */
	public AvroItemWriter(File file, DataFileWriter<T> dataFileWriter, Schema schema) {
		Assert.notNull(dataFileWriter, "'dataFileWriter' is required");
		Assert.notNull(file, "'file' is required");
		Assert.notNull(schema, "'schema' is required");
		this.dataFileWriter = dataFileWriter;
		try {
			this.dataFileWriter.create(schema, file);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param file the File where the serialized data will be written.
	 * @param dataFileWriter the {@link DataFileWriter} used to serialize objects.
	 * @param schema the File containing the schema used to serialize the objects.
	 */
	public AvroItemWriter(File file, DataFileWriter<T> dataFileWriter, File schema) {
		Assert.notNull(dataFileWriter, "'dataFileWriter' is required");
		Assert.notNull(file, "'file' is required");
		Assert.notNull(schema, "'schema' is required");
		Assert.state(schema.exists(), "'schema' " + schema.getName() + " does not exist");

		this.dataFileWriter = dataFileWriter;
		try {
			this.dataFileWriter.create(new Schema.Parser().parse(schema), file);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param outputStream the OutputStream where the serialized data will be written.
	 * @param clazz the class of the objects to be serialized. This should be an Avro generated class.
	 * @param schema the schema used to generate the class.
	 */
	public AvroItemWriter(OutputStream outputStream, Class<T> clazz, Schema schema) {
		this(outputStream, new DataFileWriter<>(datumWriterForClass(clazz)), schema);
	}

	/**
	 *
	 * @param file the File where the serialized data will be written.
	 * @param clazz the class of the objects to be serialized. This should be an Avro generated class.
	 * @param schema the schema used to generate the class.
	 */
	public AvroItemWriter(File file, Class<T> clazz, Schema schema) {
		this(file, new DataFileWriter<>(datumWriterForClass(clazz)), schema);
	}

	/**
	 * Creates an AvroItemWriter that does not embed an Avro schema header in the serialized output.
	 *
	 * @param outputStream the OutputStream where the serialized data will be written.
	 * @param datumWriter the {@link DatumWriter} to use.
	 */
	public AvroItemWriter(OutputStream outputStream, DatumWriter<T> datumWriter) {
		this.outputStreamWriter = this.createOutputStreamWriter(outputStream, datumWriter);
	}

	/**
	 * Creates an AvroItemWriter that does not embed an Avro schema header in the serialized output.
	 *
	 * @param outputStream the OutputStream where the serialized data will be written.
	 * @param clazz the type of Data to serialize.
	 */
	public AvroItemWriter(OutputStream outputStream, Class<T> clazz) {
		this(outputStream, datumWriterForClass(clazz));
	}

	/**
	 *
	 * @param file the File where the serialized data will be written.
	 * @param clazz the class of the objects to be serialized. This should be an Avro generated class.
	 * @param schema the {@link Resource} containing schema used to generate the class.
	 */
	public AvroItemWriter(File file, Class<T> clazz, Resource schema) {
		Assert.notNull(file, "'file' is required");
		Assert.notNull(clazz, "class is required");
		Assert.notNull(schema, "'schema' is required");
		Assert.state(schema.exists(), "'schema' " + schema.getFilename() + " does not exist");

		try {
			avroItemWriter(new DataFileWriter<>(datumWriterForClass(clazz)), new FileOutputStream(file),
					new Schema.Parser().parse(schema.getInputStream()));
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

	}

	/**
	 *
	 * @param file the File where the serialized data will be written.
	 * @param clazz the class of the objects to be serialized. This should be an Avro generated class.
	 * @param schema the File containing the schema used to generate the class.
	 */
	public AvroItemWriter(File file, Class<T> clazz, File schema) {
		Assert.notNull(file, "'file' is required");
		Assert.notNull(clazz, "class is required");
		Assert.notNull(schema, "'schema' is required");
		Assert.state(schema.exists(), "'schema' " + schema.getName() + " does not exist");

		try {
			avroItemWriter(new DataFileWriter<>(datumWriterForClass(clazz)), new FileOutputStream(file),
					new Schema.Parser().parse(schema));
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

	}

	private static <T> DatumWriter<T> datumWriterForClass(Class<T> clazz) {
		return SpecificRecordBase.class.isAssignableFrom(clazz) ? new SpecificDatumWriter<>(clazz)
				: new ReflectDatumWriter<>(clazz);
	}

	private AvroItemWriter avroItemWriter(DataFileWriter<T> dataFileWriter, OutputStream outputStream, Schema schema) {
		Assert.notNull(dataFileWriter, "'dataFileWriter' is required");
		Assert.notNull(outputStream, "'outputStream' is required");
		Assert.notNull(schema, "'schema' is required");

		this.dataFileWriter = dataFileWriter;
		try {
			this.dataFileWriter.create(schema, outputStream);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return this;

	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		items.forEach(item -> {
			try {
				if (this.dataFileWriter != null) {
					this.dataFileWriter.append(item);
				}
				else {
					this.outputStreamWriter.write(item);
				}
			}
			catch (Exception e) {
				throw new ItemStreamException(e.getMessage(), e);
			}
		});
	}

	@Override
	public void close() {
		try {
			if (this.dataFileWriter != null) {
				dataFileWriter.close();
			}
			else {
				this.outputStreamWriter.close();
			}
		}
		catch (IOException e) {
			throw new ItemStreamException(e.getMessage(), e);
		}
	}

	private AvroItemWriter.OutputStreamWriter<T> createOutputStreamWriter(OutputStream outputStream,
			DatumWriter<T> datumWriter) {
		return new AvroItemWriter.OutputStreamWriter<>(outputStream, datumWriter);
	}

	private static class OutputStreamWriter<T> {
		private final DatumWriter<T> datumWriter;

		private final BinaryEncoder binaryEncoder;

		private final OutputStream outputStream;

		private OutputStreamWriter(OutputStream outputStream, DatumWriter<T> datumWriter) {
			this.outputStream = outputStream;
			this.datumWriter = datumWriter;
			this.binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
		}

		private void write(T datum) throws Exception {
			datumWriter.write(datum, binaryEncoder);
			binaryEncoder.flush();
		}

		private void close() {
			try {
				this.outputStream.close();
			}
			catch (IOException e) {
				throw new ItemStreamException(e.getMessage(), e);
			}
		}
	}
}
