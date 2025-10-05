/*
 * Copyright 2019-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.avro.support;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.avro.example.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 */
public abstract class AvroTestFixtures {

	//@formatter:off
	private final Chunk<User> avroGeneratedUsers = Chunk.of(
			new User("David", 20, "blue"),
			new User("Sue", 4, "red"),
			new User("Alana", 13, "yellow"),
			new User("Joe", 1, "pink"));

	private final Chunk<PlainOldUser> plainOldUsers = Chunk.of(
			new PlainOldUser("David", 20, "blue"),
			new PlainOldUser("Sue", 4, "red"),
			new PlainOldUser("Alana", 13, "yellow"),
			new PlainOldUser("Joe", 1, "pink"));
	//@formatter:on

	protected Resource schemaResource = new ClassPathResource(
			"org/springframework/batch/infrastructure/item/avro/user-schema.json");

	protected Resource plainOldUserSchemaResource = new ByteArrayResource(PlainOldUser.SCHEMA.toString().getBytes());

	// Serialized data only
	protected Resource dataResource = new ClassPathResource(
			"org/springframework/batch/infrastructure/item/avro/user-data-no-schema.avro");

	// Data written with DataFileWriter, includes embedded SCHEMA (more common)
	protected Resource dataResourceWithSchema = new ClassPathResource(
			"org/springframework/batch/infrastructure/item/avro/user-data.avro");

	protected Resource plainOldUserDataResource = new ClassPathResource(
			"org/springframework/batch/infrastructure/item/avro/plain-old-user-data-no-schema.avro");

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

	protected Chunk<User> avroGeneratedUsers() {
		return this.avroGeneratedUsers;
	}

	protected Chunk<GenericRecord> genericAvroGeneratedUsers() {
		return new Chunk<>(this.avroGeneratedUsers.getItems().stream().map(u -> {
			GenericData.Record avroRecord;
			avroRecord = new GenericData.Record(u.getSchema());
			avroRecord.put("name", u.getName());
			avroRecord.put("favorite_number", u.getFavoriteNumber());
			avroRecord.put("favorite_color", u.getFavoriteColor());
			return avroRecord;
		}).collect(Collectors.toList()));
	}

	protected Chunk<PlainOldUser> plainOldUsers() {
		return this.plainOldUsers;
	}

	protected Chunk<GenericRecord> genericPlainOldUsers() {
		return new Chunk<>(
				this.plainOldUsers.getItems().stream().map(PlainOldUser::toGenericRecord).collect(Collectors.toList()));
	}

	protected static class PlainOldUser {

		public static final Schema SCHEMA = ReflectData.get().getSchema(PlainOldUser.class);

		private CharSequence name;

		private int favoriteNumber;

		private CharSequence favoriteColor;

		public PlainOldUser() {

		}

		public PlainOldUser(CharSequence name, int favoriteNumber, CharSequence favoriteColor) {
			this.name = name;
			this.favoriteNumber = favoriteNumber;
			this.favoriteColor = favoriteColor;
		}

		public String getName() {
			return name.toString();
		}

		public int getFavoriteNumber() {
			return favoriteNumber;
		}

		public String getFavoriteColor() {
			return favoriteColor.toString();
		}

		public GenericRecord toGenericRecord() {
			GenericData.Record avroRecord = new GenericData.Record(SCHEMA);
			avroRecord.put("name", this.name);
			avroRecord.put("favoriteNumber", this.favoriteNumber);
			avroRecord.put("favoriteColor", this.favoriteColor);
			return avroRecord;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PlainOldUser that = (PlainOldUser) o;
			return favoriteNumber == that.favoriteNumber && Objects.equals(name, that.name)
					&& Objects.equals(favoriteColor, that.favoriteColor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, favoriteNumber, favoriteColor);
		}

	}

	public static void createPlainOldUsersWithNoEmbeddedSchema() throws Exception {

		DatumWriter<PlainOldUser> userDatumWriter = new ReflectDatumWriter<>(AvroTestFixtures.PlainOldUser.class);

		FileOutputStream fileOutputStream = new FileOutputStream("plain-old-user-data-no-schema.avro");

		Encoder encoder = EncoderFactory.get().binaryEncoder(fileOutputStream, null);
		userDatumWriter.write(new PlainOldUser("David", 20, "blue"), encoder);
		userDatumWriter.write(new PlainOldUser("Sue", 4, "red"), encoder);
		userDatumWriter.write(new PlainOldUser("Alana", 13, "yellow"), encoder);
		userDatumWriter.write(new PlainOldUser("Joe", 1, "pink"), encoder);

		encoder.flush();
		fileOutputStream.flush();
		fileOutputStream.close();
	}

}
