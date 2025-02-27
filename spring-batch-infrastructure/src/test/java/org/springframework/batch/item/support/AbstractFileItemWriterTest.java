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

package org.springframework.batch.item.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.FileSystemResource;

/**
 * Tests for common methods from {@link AbstractFileItemWriter}.
 *
 * @author Elimelec Burghelea
 */
class AbstractFileItemWriterTests {

	@Test
	void testFailedFileDeletionThrowsException() {
		File outputFile = new File("target/data/output.tmp");
		File mocked = Mockito.spy(outputFile);

		TestFileItemWriter writer = new TestFileItemWriter();

		writer.setResource(new FileSystemResource(mocked));
		writer.setShouldDeleteIfEmpty(true);
		writer.setName(writer.getClass().getSimpleName());
		writer.open(new ExecutionContext());

		when(mocked.delete()).thenReturn(false);

		ItemStreamException exception = assertThrows(ItemStreamException.class, writer::close,
				"Expected exception when file deletion fails");

		assertEquals("Failed to delete empty file on close", exception.getMessage(), "Wrong exception message");
		assertNotNull(exception.getCause(), "Exception should have a cause");
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