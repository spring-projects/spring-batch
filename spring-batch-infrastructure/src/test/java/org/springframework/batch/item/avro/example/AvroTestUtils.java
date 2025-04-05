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

package org.springframework.batch.item.avro.example;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Used to create test data. See
 * <a href="http://avro.apache.org/docs/1.9.0/gettingstartedjava.html">...</a>
 *
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 */
class AvroTestUtils {

	public static void main(String... args) {
		try {
			createTestDataWithNoEmbeddedSchema();
			createTestData();
		}
		catch (Exception e) {
			// ignored
		}
	}

	static void createTestDataWithNoEmbeddedSchema() throws Exception {

		DatumWriter<User> userDatumWriter = new SpecificDatumWriter<>(User.class);

		FileOutputStream fileOutputStream = new FileOutputStream("user-data-no-schema.avro");

		Encoder encoder = EncoderFactory.get().binaryEncoder(fileOutputStream, null);
		userDatumWriter.write(new User("David", 20, "blue"), encoder);
		userDatumWriter.write(new User("Sue", 4, "red"), encoder);
		userDatumWriter.write(new User("Alana", 13, "yellow"), encoder);
		userDatumWriter.write(new User("Joe", 1, "pink"), encoder);

		encoder.flush();
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	static void createTestData() throws Exception {

		Resource schemaResource = new ClassPathResource("org/springframework/batch/item/avro/user-schema.json");

		DatumWriter<User> userDatumWriter = new SpecificDatumWriter<>(User.class);
		DataFileWriter<User> dataFileWriter = new DataFileWriter<>(userDatumWriter);
		dataFileWriter.create(new Schema.Parser().parse(schemaResource.getInputStream()), new File("users.avro"));
		dataFileWriter.append(new User("David", 20, "blue"));
		dataFileWriter.append(new User("Sue", 4, "red"));
		dataFileWriter.append(new User("Alana", 13, "yellow"));
		dataFileWriter.append(new User("Joe", 1, "pink"));
		dataFileWriter.close();
	}

}
