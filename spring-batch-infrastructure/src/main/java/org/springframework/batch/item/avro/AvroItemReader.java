/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.batch.item.avro;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} that deserializes data from a {@link Resource} containing serialized Avro objects.
 *
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @since 4.2
 */
public class AvroItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

	private boolean embeddedSchema = true;

	private InputStreamReader<T> inputStreamReader;

	private DataFileStream<T> dataFileReader;

	private InputStream inputStream;

	private DatumReader<T> datumReader;

	/**
	 *
	 * @param resource the {@link Resource} containing objects serialized with Avro.
	 * @param clazz the data type to be deserialized.
	 */
	public AvroItemReader(Resource resource, Class<T> clazz) {
		Assert.notNull(resource, "'resource' is required.");
		Assert.notNull(clazz, "'class' is required.");

		try {
			this.inputStream = resource.getInputStream();
			this.datumReader = datumReaderForClass(clazz);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param data the {@link Resource} containing the data to be read.
	 * @param schema the {@link Resource} containing the Avro schema.
	 */
	public AvroItemReader(Resource data, Resource schema) {
		Assert.notNull(data, "'data' is required.");
		Assert.state(data.exists(), "'data' " + data.getFilename() +" does not exist.");
		Assert.notNull(schema, "'schema' is required");
		Assert.state(schema.exists(), "'schema' " + schema.getFilename() +" does not exist.");
		try {
			this.inputStream = data.getInputStream();
			Schema avroSchema = new Schema.Parser().parse(schema.getInputStream());
			this.datumReader = new GenericDatumReader<>(avroSchema);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Disable or enable reading an embedded Avro schema. True by default.
	 * @param embeddedSchema set to false to if the input does not embed an Avro schema.
	 */
	public void setEmbeddedSchema(boolean embeddedSchema) {
		this.embeddedSchema = embeddedSchema;
	}


	@Nullable
	@Override
	protected T doRead() throws Exception {
	    if (this.inputStreamReader != null) {
            return this.inputStreamReader.read();
        }
	    return this.dataFileReader.hasNext()? this.dataFileReader.next(): null;
	}

	@Override
	protected void doOpen() throws Exception {
		initializeReader();
	}

	@Override
	protected void doClose() throws Exception {
		if (this.inputStreamReader != null) {
			this.inputStreamReader.close();
			return;
		}
		this.dataFileReader.close();
	}

	private void  initializeReader() throws IOException {
		if (this.embeddedSchema) {
			this.dataFileReader = new DataFileStream<>(this.inputStream, this.datumReader);
		} else {
			this.inputStreamReader = createInputStreamReader(this.inputStream, this.datumReader);
		}

	}

	private InputStreamReader<T> createInputStreamReader(InputStream inputStream, DatumReader<T> datumReader) {
		return new InputStreamReader<>(inputStream, datumReader);
	}

	private static <T> DatumReader<T> datumReaderForClass(Class<T> clazz) {
		if (SpecificRecordBase.class.isAssignableFrom(clazz)){
			return new SpecificDatumReader<>(clazz);
		}
		if (GenericRecord.class.isAssignableFrom(clazz)) {
			return new GenericDatumReader<>();
		}
		return new ReflectDatumReader<>(clazz);
	}


	private static class InputStreamReader<T> {
        private final DatumReader<T> datumReader;

        private final BinaryDecoder binaryDecoder;

        private final InputStream inputStream;

        private InputStreamReader(InputStream inputStream, DatumReader<T> datumReader) {
            this.inputStream = inputStream;
            this.datumReader = datumReader;
            this.binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        }

        private T read() throws Exception {
            if (!this.binaryDecoder.isEnd()) {
                return this.datumReader.read(null, this.binaryDecoder);
            }
            return null;
        }

        private void close() {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				throw new ItemStreamException(e.getMessage(), e);
			}
		}
    }
}
