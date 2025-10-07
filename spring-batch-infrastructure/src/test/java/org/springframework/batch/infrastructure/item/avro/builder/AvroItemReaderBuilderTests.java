/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.avro.builder;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.avro.AvroItemReader;
import org.springframework.batch.infrastructure.item.avro.example.User;
import org.springframework.batch.infrastructure.item.avro.support.AvroItemReaderTestSupport;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author David Turanski
 */
class AvroItemReaderBuilderTests extends AvroItemReaderTestSupport {

	@Test
	void itemReaderWithSchemaResource() throws Exception {

		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>().resource(dataResource)
			.embeddedSchema(false)
			.schema(schemaResource)
			.build();

		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	void itemReaderWithGeneratedData() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
			.resource(dataResourceWithSchema)
			.schema(schemaResource)
			.build();
		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	void itemReaderWithSchemaString() throws Exception {
		AvroItemReader<GenericRecord> avroItemReader = new AvroItemReaderBuilder<GenericRecord>()
			.schema(schemaString(schemaResource))
			.resource(dataResourceWithSchema)
			.build();

		verify(avroItemReader, genericAvroGeneratedUsers());
	}

	@Test
	void itemReaderWithEmbeddedHeader() throws Exception {
		AvroItemReader<User> avroItemReader = new AvroItemReaderBuilder<User>().resource(dataResourceWithSchema)
			.type(User.class)
			.build();
		verify(avroItemReader, avroGeneratedUsers());
	}

	@Test
	void itemReaderForSpecificType() throws Exception {
		AvroItemReader<User> avroItemReader = new AvroItemReaderBuilder<User>().type(User.class)
			.resource(dataResourceWithSchema)
			.build();
		verify(avroItemReader, avroGeneratedUsers());
	}

	@Test
	void itemReaderWithNoSchemaStringShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new AvroItemReaderBuilder<GenericRecord>().schema("").resource(dataResource).build());
	}

	@Test
	void itemReaderWithPartialConfigurationShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new AvroItemReaderBuilder<GenericRecord>().resource(dataResource).build());
	}

	@Test
	void itemReaderWithNoInputsShouldFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new AvroItemReaderBuilder<GenericRecord>().schema(schemaResource).build());
	}

}
