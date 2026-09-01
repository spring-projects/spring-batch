/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.support.AbstractFileItemWriter;
import org.springframework.core.io.FileSystemResource;

/**
 * Tests for common methods from {@link AbstractFileItemWriter}.
 *
 * @author Elimelec Burghelea
 */
class AbstractFileItemWriterTests {

	@Test
	void testFailedFileDeletionThrowsException() throws Exception {

		File outputFile = new File("target/data/output.tmp");
		outputFile.getParentFile().mkdirs();

		TestFileItemWriter writer = new TestFileItemWriter();
		writer.setResource(new FileSystemResource(outputFile));
		writer.setShouldDeleteIfEmpty(true);
		writer.setName(writer.getClass().getSimpleName());

		writer.open(new ExecutionContext());

		// Keep the file open so Files.delete(...) cannot delete it (Windows)
		RandomAccessFile lock = new RandomAccessFile(outputFile, "rw");

		try {
			ItemStreamException exception = assertThrows(ItemStreamException.class, writer::close);

			assertEquals("Failed to delete empty file on close", exception.getMessage());

			assertNotNull(exception.getCause());
			assertTrue(exception.getCause() instanceof IOException);
		}
		finally {
			lock.close();
		}
	}

	private static class TestFileItemWriter extends AbstractFileItemWriter<String> {

		@Override
		protected String doWrite(Chunk<? extends String> items) {
			return String.join("\n", items);
		}

		@Override
		public void afterPropertiesSet() {

		}

	}

}