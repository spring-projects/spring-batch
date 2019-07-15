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

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

import org.springframework.batch.item.avro.example.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class AvroItemReaderTests {

	@Test
	public void readGenericRecords() throws Exception {

		Resource schemaResource = new ClassPathResource("org/springframework/batch/item/avro/user-schema.json");
		Resource dataResource = new ClassPathResource("org/springframework/batch/item/avro/user-data-no-schema.avro");
		AvroItemReader<GenericRecord> itemReader = new AvroItemReader<>(dataResource, schemaResource);
		itemReader.afterPropertiesSet();

		List<GenericRecord> users = new ArrayList<>();

		GenericRecord user;
		while ((user = itemReader.read()) != null) {
			users.add(user);
		}

		assertThat(users).hasSize(4);

	}

	@Test
	public void readSpecificUsers() throws Exception {

		Resource dataResource = new ClassPathResource("org/springframework/batch/item/avro/user-data-no-schema.avro");

		AvroItemReader<User> itemReader = new AvroItemReader<>(dataResource, User.class);
		itemReader.afterPropertiesSet();

		List<User> users = new ArrayList<>();

		User user;
		while ((user = itemReader.read()) != null) {
			users.add(user);
		}

		assertThat(users).hasSize(4);
	}

	@Test
	public void readSpecificUsersWithEmbeddedSchema() throws Exception {
		Resource dataResource = new ClassPathResource("org/springframework/batch/item/avro/user-data.avro");

		AvroItemReader<User> itemReader = new AvroItemReader<>(dataResource.getFile(), User.class);
		itemReader.setEmbeddedHeader(true);
		itemReader.afterPropertiesSet();

		List<User> users = new ArrayList<>();

		User user;
		while ((user = itemReader.read()) != null) {
			users.add(user);
		}

		assertThat(users).hasSize(4);
	}

}
