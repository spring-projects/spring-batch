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

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.avro.support.AvroItemWriterTestSupport;
import org.springframework.batch.item.avro.AvroItemWriter;
import org.springframework.batch.item.avro.example.User;

/**
 * @author David Turanski
 */
public class AvroItemWriterBuilderTests extends AvroItemWriterTestSupport {

	private ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

    @Test
    public void itemWriterForAvroGeneratedClass() throws Exception {

        AvroItemWriter<User> avroItemWriter = new AvroItemWriterBuilder<User>()
                .schema(schemaResource)
                .outputStream(outputStream)
                .type(User.class)
                .build();

        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.avroGeneratedUsers());
        avroItemWriter.close();

        verifyRecordsWithEmbeddedHeader(outputStream.toByteArray(), this.avroGeneratedUsers(), User.class);
    }


	@Test
	public void itemWriterForGenericRecords() throws Exception {

		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(PlainOldUser.SCHEMA);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

		AvroItemWriter<GenericRecord> avroItemWriter = new AvroItemWriterBuilder<GenericRecord>()
				.dataFileWriter(dataFileWriter).outputStream(outputStream).schema(PlainOldUser.SCHEMA).build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.genericPlainOldUsers());
		avroItemWriter.close();

		byte[] bytes = outputStream.toByteArray();
        verifyRecordsWithEmbeddedHeader(bytes, this.genericPlainOldUsers(), GenericRecord.class);

	}

	@Test
	public void itemWriterForPojos() throws Exception {

		final DatumWriter<PlainOldUser> writer = new ReflectDatumWriter<>(PlainOldUser.class);

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriterBuilder<PlainOldUser>()
                .outputStream(outputStream)
                .datumWriter(writer)
                .build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.plainOldUsers());
		avroItemWriter.close();

		byte[] bytes = outputStream.toByteArray();

		verifyRecords(bytes, this.plainOldUsers(), PlainOldUser.class, false);

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithMultipleOutputs() {

    	new AvroItemWriterBuilder<GenericRecord>()
				.outputStream(outputStream)
				.outputFile(new File("output"))
				.schema(schemaResource)
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWitNoOutputs() {

		new AvroItemWriterBuilder<GenericRecord>()
				.schema(schemaResource)
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithMultipleWriters() {

		new AvroItemWriterBuilder<GenericRecord>()
				.outputStream(outputStream)
				.schema(schemaResource)
				.datumWriter(new GenericDatumWriter<>())
				.dataFileWriter(new DataFileWriter<>(new GenericDatumWriter<>()))
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithNoWriters() {

		new AvroItemWriterBuilder<GenericRecord>()
				.outputStream(outputStream)
				.schema(schemaResource)
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithWriterAndType() {

		new AvroItemWriterBuilder<GenericRecord>()
				.outputStream(outputStream)
				.schema(schemaResource)
				.type(GenericRecord.class)
				.dataFileWriter(new DataFileWriter<>(new GenericDatumWriter<>()))
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithDatumWriterAndSchema() {

		new AvroItemWriterBuilder<GenericRecord>()
				.outputStream(outputStream)
				.schema(schemaResource)
				.type(GenericRecord.class)
				.datumWriter(new GenericDatumWriter<>())
				.build();

	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailWithDataFileWriterAndSchema() {

		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(PlainOldUser.SCHEMA);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

		new AvroItemWriterBuilder<GenericRecord>()
				.schema(schemaResource)
				.dataFileWriter(dataFileWriter)
				.build();

	}


}
