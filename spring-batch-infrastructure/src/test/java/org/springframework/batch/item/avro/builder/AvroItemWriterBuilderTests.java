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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.batch.item.avro.AvroItemWriter;
import org.springframework.batch.item.avro.example.PlainOldUser;
import org.springframework.batch.item.avro.example.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class AvroItemWriterBuilderTests {

    private Resource schemaResource = new ClassPathResource("org/springframework/batch/item/avro/user-schema.json");

    @Test
    public void itemWriterForAvroGeneratedClass() throws Exception {
        List<User> users = Arrays.asList(new User("David", 20, "blue"),
                new User("Sue", 4, "red"),
                new User("Alana", 13, "yellow"),
                new User("Joe", 1, "pink"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

        AvroItemWriter<User> avroItemWriter = new AvroItemWriterBuilder<User>()
                .schema(schemaResource)
                .outputStream(outputStream)
                .type(User.class)
                .build();

        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(users);
        avroItemWriter.close();

        verifyAvroRecords(outputStream.toByteArray(), users, User.class, new Schema.Parser().parse(schemaResource.getInputStream()));
    }


	@Test
	public void itemWriterForGenericRecords() throws Exception {

		List<GenericRecord> users = Arrays.asList(new PlainOldUser("David", 20, "blue").toGenericRecord(),
				new PlainOldUser("Sue", 4, "red").toGenericRecord(),
				new PlainOldUser("Alana", 13, "yellow").toGenericRecord(),
				new PlainOldUser("Joe", 1, "pink").toGenericRecord());

		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(PlainOldUser.schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

		AvroItemWriter<GenericRecord> avroItemWriter = new AvroItemWriterBuilder<GenericRecord>()
				.dataFileWriter(dataFileWriter).outputStream(outputStream).schema(PlainOldUser.schema).build();

		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(users);
		avroItemWriter.close();

		byte[] bytes = outputStream.toByteArray();
        verifyAvroRecords(bytes, users, GenericRecord.class, PlainOldUser.schema);

	}

	@Test
	public void itemWriterForPojos() throws Exception {
		List<PlainOldUser> users = Arrays.asList(new PlainOldUser("David", 20, "blue"),
				new PlainOldUser("Sue", 4, "red"), new PlainOldUser("Alana", 13, "yellow"),
				new PlainOldUser("Joe", 1, "pink"));

		final DatumWriter<PlainOldUser> writer = new ReflectDatumWriter<>(PlainOldUser.class);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

		AvroItemWriter<PlainOldUser> avroItemWriter = new AvroItemWriterBuilder<PlainOldUser>()
                .outputStream(outputStream)
                .datumWriter(writer)
                .build();


		avroItemWriter.open(new ExecutionContext());
		avroItemWriter.write(users);
		avroItemWriter.close();

		byte[] bytes = outputStream.toByteArray();

		verifyPojos(bytes, users.stream().map(PlainOldUser::toGenericRecord).collect(Collectors.toList()),
                GenericRecord.class, PlainOldUser.schema);

	}


	/*
	 * This item reader configured for Specific Avro types.
	 */
    private <T> void verifyAvroRecords(byte[] bytes, List<T> actual, Class<T> clazz, Schema schema) throws Exception {
        doVerify(bytes, clazz, actual);
    }


    private <T> void doVerify(byte[] bytes, Class<T> clazz, List<T> actual) throws Exception {
        AvroItemReader<T> avroItemReader = new AvroItemReaderBuilder<T>()
                .type(clazz)
                .inputStream(new ByteArrayInputStream(bytes))
                .embeddedHeader(true)
                .build();

        avroItemReader.afterPropertiesSet();

        List<T> records = new ArrayList<>();
        T record;
        while ((record = avroItemReader.read()) != null) {
            records.add(record);
        }
        assertThat(records).hasSize(4);
        assertThat(records).containsExactlyInAnyOrder(actual.get(0), actual.get(1), actual.get(2), actual.get(3));
    }

    /*
     * This item reader configured for no embedded schema header.
     */
	private <T> void verifyPojos(byte[] bytes, List<T> actual, Class<T> clazz, Schema schema) throws Exception {
		AvroItemReader<T> avroItemReader = new AvroItemReaderBuilder<T>()
                .inputStream(new ByteArrayInputStream(bytes))
				.schema(schema)
                .build();
        avroItemReader.afterPropertiesSet();


        List<T> records = new ArrayList<>();
		T record;
		while ((record = avroItemReader.read()) != null) {
 			records.add(record);
		}
		assertThat(records).hasSize(4);
		assertThat(records).containsExactlyInAnyOrder(actual.get(0), actual.get(1), actual.get(2), actual.get(3));

	}

}
