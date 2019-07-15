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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} that deserializes data from a File or Input Stream containing serialized Avro objects.
 *
 * Inject a {@link DataFileStream}, typically a {@link DataFileReader}, if the serialized input contains an Avro Schema header.
 * This is the case if the input was created with an {@link DataFileStream} or {@link org.apache.avro.file.DataFileWriter}.
 * Otherwise, this expects the input to only contain the data items.
 *
 * @author David Turanski
 * @since 4.2
 */
public class AvroItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

	private boolean embeddedHeader;

	private InputStreamReader<T> inputStreamReader;

	private DataFileStream<T> dataFileReader;

	private Class<T> clazz;

	private InputStream inputStream;

	private DatumReader<T> datumReader;

	/**
	 *
	 * @param inputStream the InputStream containing objects serialized with Avro.
	 * @param datumReader the {@link DatumReader} used to read the data items.
	 */
	public AvroItemReader(InputStream inputStream, DatumReader<T> datumReader) {
		Assert.notNull(inputStream, "'inputStream' is required");
		Assert.notNull(datumReader, "'datumReader' is required");
		this.inputStream = inputStream;
		this.datumReader = datumReader;
	}


	/**
	 *
	 * @param file the File containing objects serialized with Avro.
	 * @param clazz the class of the objects to be deserialized. This should be an Avro generated class.
	 */
	public AvroItemReader(File file, Class<T> clazz) {
		Assert.notNull(file, "'file' is required");
		Assert.state(file.exists(), "'file' " + file.getName() +" does not exist");
		Assert.notNull(clazz, "class is required");
        try {
        	this.inputStream = new FileInputStream(file);
        	this.datumReader = datumReaderForClass(clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

	/**
	 *
	 * @param inputStream the InputStream containing objects serialized with Avro.
	 * @param clazz the class of the objects to be deserialized. This should be an Avro generated class.
	 */
	public AvroItemReader(InputStream inputStream, Class<T> clazz) {
		Assert.notNull(inputStream, "'inputStream' is required");
		Assert.notNull(clazz, "class is required");
		this.inputStream = inputStream;
		this.datumReader = datumReaderForClass(clazz);
	}

	/**
	 *
	 * @param resource the {@link Resource} containing objects serialized with Avro.
	 * @param clazz the class of the objects to be deserialized. This should be an Avro generated class.
	 */
	public AvroItemReader(Resource resource, Class<T> clazz) {
		Assert.notNull(resource, "'resource' is required");
		Assert.notNull(clazz, "'class' is required");

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
	 * @param dataFileReader a {@link DataFileStream}, typically a {@link DataFileReader}.
	 */
	public AvroItemReader(DataFileStream<T> dataFileReader) {
		Assert.notNull(dataFileReader, "'dataFileReader' is required");
		this.dataFileReader = dataFileReader;
	}

	/**
	 *
	 * @param resource the {@link Resource} containing objects serialized with Avro.
	 * @param datumReader the {@link DatumReader} used to read the data items.
	 */
	public AvroItemReader(Resource resource, DatumReader<T> datumReader) {
		Assert.notNull(resource, "'resource' is required");
		Assert.state(resource.exists(), "'resource' " + resource.getFilename() +" does not exist");
		Assert.notNull(datumReader, "'datumReader' is required");

		try {
			this.inputStream = resource.getInputStream();
			this.datumReader = datumReader;
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param inputStream the InputStream containing objects serialized with Avro.
	 * @param schema the schema used to serialize the data.
	 */
	public AvroItemReader(InputStream inputStream, Schema schema) {
		Assert.notNull(inputStream, "'inputStream' is required");
		Assert.notNull(schema, "'schema' is required");
		this.inputStream = inputStream;
		this.datumReader = new GenericDatumReader<>(schema);
	}

	/**
	 *
	 * @param data the {@link Resource} containing the data to be read.
	 * @param schema the {@link Resource} containing the
	 */
	public AvroItemReader(Resource data, Resource schema) {
		Assert.notNull(data, "'data' is required");
		Assert.state(data.exists(), "'data' " + data.getFilename() +" does not exist");
		Assert.notNull(data, "'schema' is required");
		Assert.state(data.exists(), "'schema' " + schema.getFilename() +" does not exist");
		try {
			this.inputStream = data.getInputStream();
			Schema avroSchema = new Schema.Parser().parse(schema.getInputStream());
			this.datumReader = new GenericDatumReader<>(avroSchema);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public void setEmbeddedHeader(boolean embeddedHeader) {
		this.embeddedHeader = embeddedHeader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeReader();
	}


	@Override
	protected T doRead() throws Exception {
	    if (inputStreamReader != null) {
            return inputStreamReader.read();
        }
	    return dataFileReader.hasNext()? dataFileReader.next(): null;
	}

	@Override
	protected void doOpen() {
	}

	@Override
	protected void doClose() throws Exception {
		if (this.inputStreamReader != null) {
			inputStreamReader.close();
			return;
		}
		dataFileReader.close();
	}

	private void  initializeReader() throws IOException {
		if (this.dataFileReader != null) {
			return;
		}
		if (this.embeddedHeader) {
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
            if (!binaryDecoder.isEnd()) {
                return datumReader.read(null, binaryDecoder);
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
