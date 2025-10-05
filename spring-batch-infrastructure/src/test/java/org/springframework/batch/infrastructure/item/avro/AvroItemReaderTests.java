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

package org.springframework.batch.infrastructure.item.avro;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.avro.example.User;
import org.springframework.batch.infrastructure.item.avro.support.AvroItemReaderTestSupport;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author David Turanski
 */
class AvroItemReaderTests extends AvroItemReaderTestSupport {

	@Test
	void readGenericRecordsUsingResources() throws Exception {

		AvroItemReader<GenericRecord> itemReader = new AvroItemReader<>(dataResource, schemaResource);
		itemReader.setName(itemReader.getClass().getSimpleName());
		itemReader.setEmbeddedSchema(false);

		verify(itemReader, genericAvroGeneratedUsers());
	}

	@Test
	void readSpecificUsers() throws Exception {

		AvroItemReader<User> itemReader = new AvroItemReader<>(dataResource, User.class);
		itemReader.setEmbeddedSchema(false);
		itemReader.setName(itemReader.getClass().getSimpleName());

		verify(itemReader, avroGeneratedUsers());
	}

	@Test
	void readSpecificUsersWithEmbeddedSchema() throws Exception {

		AvroItemReader<User> itemReader = new AvroItemReader<>(dataResourceWithSchema, User.class);
		itemReader.setEmbeddedSchema(true);
		itemReader.setName(itemReader.getClass().getSimpleName());

		verify(itemReader, avroGeneratedUsers());
	}

	@Test
	void readPojosWithNoEmbeddedSchema() throws Exception {

		AvroItemReader<PlainOldUser> itemReader = new AvroItemReader<>(plainOldUserDataResource, PlainOldUser.class);
		itemReader.setEmbeddedSchema(false);
		itemReader.setName(itemReader.getClass().getSimpleName());

		verify(itemReader, plainOldUsers());
	}

	@Test
	void dataResourceDoesNotExist() {
		assertThrows(IllegalStateException.class,
				() -> new AvroItemReader<User>(new ClassPathResource("doesnotexist"), schemaResource));
	}

	@Test
	void schemaResourceDoesNotExist() {
		assertThrows(IllegalStateException.class,
				() -> new AvroItemReader<User>(dataResource, new ClassPathResource("doesnotexist")));
	}

}
