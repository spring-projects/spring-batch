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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.batch.item.avro.builder.AvroItemReaderBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 */
public abstract class AvroItemWriterTestSupport extends AvroTestFixtures {

	/*
	 * This item reader configured for Specific Avro types.
	 */
	protected <T> void verifyRecords(byte[] bytes, List<T> actual, Class<T> clazz, boolean embeddedSchema)
			throws Exception {
		doVerify(bytes, clazz, actual, embeddedSchema);
	}

	protected <T> void verifyRecordsWithEmbeddedHeader(byte[] bytes, List<T> actual, Class<T> clazz) throws Exception {
		doVerify(bytes, clazz, actual, true);
	}

	private <T> void doVerify(byte[] bytes, Class<T> clazz, List<T> actual, boolean embeddedSchema) throws Exception {
		AvroItemReader<T> avroItemReader = new AvroItemReaderBuilder<T>().type(clazz)
				.resource(new ByteArrayResource(bytes)).embeddedSchema(embeddedSchema).build();

		avroItemReader.open(new ExecutionContext());

		List<T> records = new ArrayList<>();
		T record;
		while ((record = avroItemReader.read()) != null) {
			records.add(record);
		}
		assertThat(records).hasSize(4);
		assertThat(records).containsExactlyInAnyOrder(actual.get(0), actual.get(1), actual.get(2), actual.get(3));
	}

	protected static class OutputStreamResource implements WritableResource {

		final private OutputStream outputStream;

		public OutputStreamResource(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return this.outputStream;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public URL getURL() throws IOException {
			return null;
		}

		@Override
		public URI getURI() throws IOException {
			return null;
		}

		@Override
		public File getFile() throws IOException {
			return null;
		}

		@Override
		public long contentLength() throws IOException {
			return 0;
		}

		@Override
		public long lastModified() throws IOException {
			return 0;
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return null;
		}

		@Override
		public String getFilename() {
			return null;
		}

		@Override
		public String getDescription() {
			return "Output stream resource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

	}

}
