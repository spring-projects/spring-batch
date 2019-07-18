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

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.Test;

import org.springframework.batch.item.avro.support.AvroItemReaderTestSupport;
import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.batch.item.avro.example.User;

/**
 * @author David Turanski
 */
public class AvroItemReaderBuilderTests extends AvroItemReaderTestSupport {

	@Test
	public void itemReaderWithSpecificDatumReader() throws Exception {

		AvroItemReader<User> avroItemReader = new AvroItemReaderBuilder<User>()
				.datumReader(new SpecificDatumReader<>(User.class)).resource(dataResource).build();
		verify(avroItemReader, avroGeneratedUsers());
	}

	@Test
	public void itemReaderWithSchemaResource() throws Exception {

		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>().resource(dataResource)
				.schema(schemaResource).build();

		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	public void itemReaderWithInputStream() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
				.inputStream(dataResource.getInputStream()).schema(schemaResource).build();

		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	public void itemReaderWithSchema() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
				.inputStream(dataResource.getInputStream())
				.schema(new Schema.Parser().parse(schemaResource.getInputStream())).build();
		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	public void itemReaderWithSchemaString() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
				.schema(schemaString(schemaResource)).resource(dataResource).build();

		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	public void itemReaderWithFile() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
				.schema(schemaResource.getFile()).inputFile(dataResource.getFile()).build();
		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	public void itemReaderWithEmbeddedHeader() throws Exception {
		AvroItemReader<User> avroItemReader = new AvroItemReaderBuilder<User>()
				.inputFile(dataResourceWithSchema.getFile()).type(User.class).embeddedHeader(true).build();
		verify(avroItemReader, avroGeneratedUsers());
	}

	@Test
	public void itemReaderForSpecificType() throws Exception {
		AvroItemReader<User> avroItemReader = new AvroItemReaderBuilder<User>().type(User.class).embeddedHeader(true)
				.resource(dataResourceWithSchema).build();
		verify(avroItemReader, avroGeneratedUsers());
	}

	@Test(expected = IllegalArgumentException.class)
	public void itemReaderWithNoSchemaStringShouldFail() {
		new AvroItemReaderBuilder<GenericRecord>().schema("").resource(dataResource).build();

	}

	@Test(expected = IllegalArgumentException.class)
	public void itemReaderWithPartialConfigurationShouldFail() {
		new AvroItemReaderBuilder<GenericRecord>().resource(dataResource).build();
	}

	@Test(expected = IllegalStateException.class)
	public void itemReaderWithNoInputsShouldFail() {
		new AvroItemReaderBuilder<GenericRecord>().schema(schemaResource).build();
	}

	@Test(expected = IllegalStateException.class)
	public void itemReaderWithMultipleInputsShouldFail() throws IOException {
		new AvroItemReaderBuilder<GenericRecord>().schema(schemaResource).resource(dataResource)
				.inputFile(dataResource.getFile()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void itemReaderWithDataFileWriterAndTypeShouldFail() throws IOException {
		DatumReader<User> userDatumReader = new SpecificDatumReader<>(User.class);
		DataFileReader<User> dataFileReader = new DataFileReader<>(dataResourceWithSchema.getFile(), userDatumReader);

		new AvroItemReaderBuilder<User>().type(User.class).embeddedHeader(true).dataFileReader(dataFileReader).build();
	}
}
