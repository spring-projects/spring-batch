/*
 * Copyright 2019-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} that serializes data to an {@link WritableResource} using Avro.
 * <p>
 * This does not support restart on failure.
 *
 * <p>
 * This writer is <b>not</b> thread-safe.
 * </p>
 *
 * @since 4.2
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 */
public class AvroItemWriter<T> extends AbstractItemStreamItemWriter<T> {

	private @Nullable DataFileWriter<T> dataFileWriter;

	private @Nullable OutputStreamWriter<T> outputStreamWriter;

	private final WritableResource resource;

	private @Nullable Resource schemaResource;

	private final Class<T> clazz;

	/**
	 * @param resource a {@link WritableResource} to which the objects will be serialized.
	 * @param schema a {@link Resource} containing the Avro schema.
	 * @param clazz the data type to be serialized.
	 */
	public AvroItemWriter(WritableResource resource, Resource schema, Class<T> clazz) {
		this(resource, clazz);
		this.schemaResource = schema;
	}

	/**
	 * This constructor will create an ItemWriter that does not embedded Avro schema.
	 * @param resource a {@link WritableResource} to which the objects will be serialized.
	 * @param clazz the data type to be serialized.
	 */
	public AvroItemWriter(WritableResource resource, Class<T> clazz) {
		this.resource = resource;
		this.clazz = clazz;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void write(Chunk<? extends T> items) throws Exception {
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
		super.open(executionContext);
		try {
			initializeWriter();
		}
		catch (IOException e) {
			throw new ItemStreamException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void close() {
		try {
			if (this.dataFileWriter != null) {
				this.dataFileWriter.close();
			}
			else {
				this.outputStreamWriter.close();
			}
		}
		catch (IOException e) {
			throw new ItemStreamException(e.getMessage(), e);
		}
	}

	private void initializeWriter() throws IOException {
		Assert.notNull(this.resource, "'resource' is required.");
		Assert.notNull(this.clazz, "'class' is required.");

		if (this.schemaResource != null) {
			Assert.state(this.schemaResource.exists(),
					"'schema' " + this.schemaResource.getFilename() + " does not exist.");
			Schema schema;
			try {
				schema = new Schema.Parser().parse(this.schemaResource.getInputStream());
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			this.dataFileWriter = new DataFileWriter<>(datumWriterForClass(this.clazz));
			this.dataFileWriter.create(schema, this.resource.getOutputStream());
		}
		else {
			this.outputStreamWriter = createOutputStreamWriter(this.resource.getOutputStream(),
					datumWriterForClass(this.clazz));
		}
	}

	private static <T> DatumWriter<T> datumWriterForClass(Class<T> clazz) {
		if (SpecificRecordBase.class.isAssignableFrom(clazz)) {
			return new SpecificDatumWriter<>(clazz);
		}
		if (GenericRecord.class.isAssignableFrom(clazz)) {
			return new GenericDatumWriter<>();
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
			this.datumWriter.write(datum, this.binaryEncoder);
			this.binaryEncoder.flush();
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
