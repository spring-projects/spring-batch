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

package org.springframework.batch.item.avro.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ReflectData;
import org.springframework.batch.item.avro.example.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author David Turanski
 */
public abstract class AvroTestFixtures {


	//@formatter:off
	private final List<User> avroGeneratedUsers = Arrays.asList(
			new User("David", 20, "blue"),
			new User("Sue", 4, "red"),
			new User("Alana", 13, "yellow"),
			new User("Joe", 1, "pink"));

	List<PlainOldUser> plainOldUsers = Arrays.asList(
			new PlainOldUser("David", 20, "blue"),
			new PlainOldUser("Sue", 4, "red"),
			new PlainOldUser("Alana", 13, "yellow"),
			new PlainOldUser("Joe", 1, "pink"));
	//@formatter:on


	protected Resource schemaResource = new ClassPathResource("org/springframework/batch/item/avro/user-schema.json");


	protected Resource plainOldUserSchemaResource = new ByteArrayResource(PlainOldUser.SCHEMA.toString().getBytes());

	// Serialized data only
	protected Resource dataResource = new ClassPathResource(
			"org/springframework/batch/item/avro/user-data-no-schema.avro");

	// Data written with DataFileWriter, includes embedded SCHEMA (more common)
	protected Resource dataResourceWithSchema = new ClassPathResource(
			"org/springframework/batch/item/avro/user-data.avro");

	protected String schemaString(Resource resource) {
		{
			String content;
			try {
				content = new String(Files.readAllBytes(Paths.get(resource.getFile().getAbsolutePath())));
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			return content;
		}
	}

	protected List<User> avroGeneratedUsers() {
		return this.avroGeneratedUsers;
	}

	protected List<GenericRecord> genericAvroGeneratedUsers() {
		return this.avroGeneratedUsers.stream().map(u-> {
					GenericData.Record avroRecord;
					avroRecord = new GenericData.Record(u.getSchema());
					avroRecord.put("name", u.getName());
					avroRecord.put("favorite_number", u.getFavoriteNumber());
					avroRecord.put("favorite_color",u.getFavoriteColor());
					return avroRecord;
				}
				).collect(Collectors.toList());
	}

	protected List<PlainOldUser> plainOldUsers() {
		return this.plainOldUsers;
	}

	protected List<GenericRecord> genericPlainOldUsers() {
		return this.plainOldUsers.stream().map(PlainOldUser::toGenericRecord).collect(Collectors.toList());
	}

	protected static class PlainOldUser {
		public static final Schema SCHEMA = ReflectData.get().getSchema(PlainOldUser.class);
		private String name;

		private int favoriteNumber;

		private String favoriteColor;

		public PlainOldUser(){

		}

		public PlainOldUser(String name, int favoriteNumber, String favoriteColor) {
			this.name = name;
			this.favoriteNumber = favoriteNumber;
			this.favoriteColor = favoriteColor;
		}

		public String getName() {
			return name;
		}

		public int getFavoriteNumber() {
			return favoriteNumber;
		}

		public String getFavoriteColor() {
			return favoriteColor;
		}

		public GenericRecord toGenericRecord() {
			GenericData.Record avroRecord = new GenericData.Record(SCHEMA);
			avroRecord.put("name", this.name);
			avroRecord.put("favoriteNumber", this.favoriteNumber);
			avroRecord.put("favoriteColor",this.favoriteColor);
			return avroRecord;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			PlainOldUser that = (PlainOldUser) o;
			return favoriteNumber == that.favoriteNumber &&
					Objects.equals(name, that.name) &&
					Objects.equals(favoriteColor, that.favoriteColor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, favoriteNumber, favoriteColor);
		}
	}
}
