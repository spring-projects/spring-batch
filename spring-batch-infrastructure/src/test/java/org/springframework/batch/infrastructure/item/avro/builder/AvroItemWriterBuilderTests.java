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

import java.io.ByteArrayOutputStream;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.avro.AvroItemWriter;
import org.springframework.batch.infrastructure.item.avro.example.User;
import org.springframework.batch.infrastructure.item.avro.support.AvroItemWriterTestSupport;
import org.springframework.core.io.WritableResource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author David Turanski
 */
class AvroItemWriterBuilderTests extends AvroItemWriterTestSupport {

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

	private final WritableResource output = new OutputStreamResource(outputStream);

	@Test
	void itemWriterForAvroGeneratedClass() throws Exception {

		AvroItemWriter<User> avroItemWriter = new AvroItemWriterBuilder<User>().resource(output)
			.schema(schemaResource)
			.type(User.class)
			.build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.avroGeneratedUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(outputStream.toByteArray(), this.avroGeneratedUsers(), User.class);
	}

	@Test
	void itemWriterForGenericRecords() throws Exception {

		AvroItemWriter<GenericRecord> avroItemWriter = new AvroItemWriterBuilder<GenericRecord>()
			.type(GenericRecord.class)
			.schema(plainOldUserSchemaResource)
			.resource(output)
			.build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.genericPlainOldUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(outputStream.toByteArray(), this.genericPlainOldUsers(), GenericRecord.class);

	}

	@Test
	void itemWriterForPojos() throws Exception {

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriterBuilder<PlainOldUser>().resource(output)
			.schema(plainOldUserSchemaResource)
			.type(PlainOldUser.class)
			.build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.plainOldUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(outputStream.toByteArray(), this.plainOldUsers(), PlainOldUser.class);

	}

	@Test
	void itemWriterWithNoEmbeddedSchema() throws Exception {

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriterBuilder<PlainOldUser>().resource(output)
			.type(PlainOldUser.class)
			.build();
		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.plainOldUsers());
		avroItemWriter.close();

		verifyRecords(outputStream.toByteArray(), this.plainOldUsers(), PlainOldUser.class, false);

	}

	@Test
	void shouldFailWitNoOutput() {
		assertThrows(IllegalArgumentException.class,
				() -> new AvroItemWriterBuilder<GenericRecord>().type(GenericRecord.class).build());
	}

	@Test
	void shouldFailWitNoType() {
		assertThrows(IllegalArgumentException.class,
				() -> new AvroItemWriterBuilder<>().resource(output).schema(schemaResource).build());
	}

}
