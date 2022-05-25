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

import java.io.ByteArrayOutputStream;

import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.avro.example.User;
import org.springframework.batch.item.avro.support.AvroItemWriterTestSupport;
import org.springframework.core.io.WritableResource;

/**
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 */
public class AvroItemWriterTests extends AvroItemWriterTestSupport {

	private ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

	private WritableResource output = new OutputStreamResource(outputStream);

	@Test
	public void itemWriterForAvroGeneratedClass() throws Exception {

		AvroItemWriter<User> avroItemWriter = new AvroItemWriter<>(this.output, this.schemaResource, User.class);
		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.avroGeneratedUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(this.outputStream.toByteArray(), this.avroGeneratedUsers(), User.class);
	}

	@Test
	public void itemWriterForGenericRecords() throws Exception {

		AvroItemWriter<GenericRecord> avroItemWriter = new AvroItemWriter<>(this.output,
				this.plainOldUserSchemaResource, GenericRecord.class);

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.genericPlainOldUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(this.outputStream.toByteArray(), this.genericPlainOldUsers(),
				GenericRecord.class);

	}

	@Test
	public void itemWriterForPojos() throws Exception {

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriter<>(this.output, this.plainOldUserSchemaResource,
				PlainOldUser.class);
		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.plainOldUsers());
		avroItemWriter.close();

		verifyRecordsWithEmbeddedHeader(this.outputStream.toByteArray(), this.plainOldUsers(), PlainOldUser.class);

	}

	@Test
	public void itemWriterWithNoEmbeddedHeaders() throws Exception {

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriter<>(this.output, PlainOldUser.class);
		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(this.plainOldUsers());
		avroItemWriter.close();

		verifyRecords(this.outputStream.toByteArray(), this.plainOldUsers(), PlainOldUser.class, false);

	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailWitNoOutput() {
		new AvroItemWriter<>(null, this.schemaResource, User.class).open(new ExecutionContext());

	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailWitNoType() {
		new AvroItemWriter<>(this.output, this.schemaResource, null).open(new ExecutionContext());
	}

}
