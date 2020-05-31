/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.springframework.batch.support.transaction.TransactionAwareBufferedWriter;

/**
 * Tests for {@link DefaultBufferedWriterFactory}.
 *
 * @author Parikshit Dutta
 *
 */
public class DefaultBufferedWriterFactoryTests {

	private static final String TEST_STRING = "DefaultBufferedWriterFactoryTests-OutputData";

	private File file;
	private FileOutputStream fileOutputStream;
	private FileChannel fileChannel;
	private Writer writer;

	@Before
	public void setUp() throws IOException {
		file = File.createTempFile("bufferedwriterfactory-test-output", ".tmp");
		fileOutputStream = new FileOutputStream(file.getAbsolutePath(), true);
		fileChannel = fileOutputStream.getChannel();
	}

	@After
	public void release() throws IOException {
		if (writer != null) {
			writer.close();
		}
		fileChannel.close();
		fileOutputStream.close();
		file.delete();
	}

	@Test
	public void testCreateBufferedWriter() throws IOException {
		BufferedWriter mockBufferedWriter = mock(BufferedWriter.class);

		DefaultBufferedWriterFactory factory = mock(DefaultBufferedWriterFactory.class);
		when(factory.createBufferedWriter(any(FileChannel.class), anyString(), anyBoolean()))
				.thenReturn(mockBufferedWriter);

		writer = factory.createBufferedWriter(fileChannel, "UTF-8", false);
		writer.write(TEST_STRING);

		verify(mockBufferedWriter).write(TEST_STRING);
	}

	@Test
	public void testCreateTransactionAwareBufferedWriter() throws IOException {
		TransactionAwareBufferedWriter mockTransactionAwareBufferedWriter = mock(TransactionAwareBufferedWriter.class);

		DefaultBufferedWriterFactory factory = mock(DefaultBufferedWriterFactory.class);
		when(factory.createTransactionAwareBufferedWriter(any(FileChannel.class), anyString(), anyBoolean(),
				any(Runnable.class))).thenReturn(mockTransactionAwareBufferedWriter);

		Runnable closeCallback = mock(Runnable.class);

		writer = factory.createTransactionAwareBufferedWriter(fileChannel, "UTF-8", false, closeCallback);
		writer.write(TEST_STRING);

		verify(mockTransactionAwareBufferedWriter).write(TEST_STRING);
	}
}
