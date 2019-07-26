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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} that serializes data to an {@link WritableResource} using Avro.
 *
 * This does not support restart on failure.
 *
 * @since 4.2
 * @author David Turanski
 */
public class AvroItemWriter<T> extends AbstractItemStreamItemWriter<T> {

	private DataFileWriter<T> dataFileWriter;

	private OutputStreamWriter<T> outputStreamWriter;

	private WritableResource resource;

	private Schema schema;

	private Resource schemaResource;

	private Class<T> clazz;

	private boolean embedSchema = true;


	/**
	 *
	 * @param resource a {@link WritableResource} to which the objects will be serialized.
	 * @param schema a {@link Resource} containing the Avro schema.
	 * @param clazz the data type to be serialized.
	 */
	public AvroItemWriter(WritableResource resource, Resource schema, Class<T> clazz) {
		this.schemaResource = schema;
		this.resource = resource;
		this.clazz = clazz;
	}

	/**
	 * This constructor will create an ItemWriter that does not embedded Avro schema.
	 *
	 * @param resource a {@link WritableResource} to which the objects will be serialized.
	 * @param clazz the data type to be serialized.
	 */
	public AvroItemWriter(WritableResource resource, Class<T> clazz) {
		this(resource, null, clazz);
		embedSchema = false;
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

	/**
	 * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
	 */
	@Override
	public void open(ExecutionContext executionContext) {
		try {
			initializeWriter();
		} catch (IOException e) {
			throw new ItemStreamException(e.getMessage(), e);
		}
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

	private void  initializeWriter() throws IOException {
		Assert.notNull(resource, "'resource' is required.");
		Assert.notNull(clazz, "'class' is required.");

		if (this.embedSchema) {
			Assert.notNull(this.schemaResource, "'schema' is required.");
			Assert.state(this.schemaResource.exists(),
					"'schema' " + this.schemaResource.getFilename() + " does not exist.");
			try {
				this.schema = new Schema.Parser().parse(this.schemaResource.getInputStream());
			} catch (IOException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			this.dataFileWriter = new DataFileWriter<>(datumWriterForClass(this.clazz));
			this.dataFileWriter.create(this.schema, this.resource.getOutputStream());
		} else {
			this.outputStreamWriter = createOutputStreamWriter(resource.getOutputStream(),
					datumWriterForClass(this.clazz));
		}

	}

	private static <T> DatumWriter<T> datumWriterForClass(Class<T> clazz) {
		if (GenericRecord.class.isAssignableFrom(clazz)) {
			return new GenericDatumWriter<>();

		}
		if (SpecificRecordBase.class.isAssignableFrom(clazz)){
			return new SpecificDatumWriter<>(clazz);
		}

		return new ReflectDatumWriter<>(clazz);
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
