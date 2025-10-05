/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file;

import java.io.File;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MultiResourceItemWriter} delegating to {@link FlatFileItemWriter}.
 */
public class MultiResourceItemWriterFlatFileTests extends AbstractMultiResourceItemWriterTests {

	/**
	 * @author dsyer
	 *
	 */
	private final class WriterCallback implements TransactionCallback<Void> {

		private final Chunk<? extends String> list;

		public WriterCallback(Chunk<? extends String> list) {
			super();
			this.list = list;
		}

		@Override
		public @Nullable Void doInTransaction(TransactionStatus status) {
			try {
				tested.write(list);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected");
			}
			return null;
		}

	}

	private FlatFileItemWriter<String> delegate;

	@BeforeEach
	void setUp() throws Exception {
		super.createFile();
		delegate = new FlatFileItemWriter<>(new PassThroughLineAggregator<>());
	}

	@Test
	void testBasicMultiResourceWriteScenario() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		assertFileExistsAndContains(1, "12");
		assertFileExistsAndContains(2, "3");

		tested.write(Chunk.of("4"));

		assertFileExistsAndContains(2, "34");

		tested.write(Chunk.of("5"));

		assertFileExistsAndContains(3, "5");

		tested.write(Chunk.of("6", "7", "8", "9"));

		assertFileExistsAndContains(3, "56");
		assertFileExistsAndContains(4, "78");
		assertFileExistsAndContains(5, "9");
	}

	@Test
	void testUpdateAfterDelegateClose() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.update(executionContext);
		assertEquals(0, executionContext.getInt(tested.getExecutionContextKey("resource.item.count")));
		assertEquals(1, executionContext.getInt(tested.getExecutionContextKey("resource.index")));
		tested.write(Chunk.of("1", "2", "3"));
		tested.update(executionContext);
		assertEquals(1, executionContext.getInt(tested.getExecutionContextKey("resource.item.count")));
		assertEquals(2, executionContext.getInt(tested.getExecutionContextKey("resource.index")));

	}

	@Test
	void testMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		assertFileExistsAndContains(1, "12f");
		assertFileExistsAndContains(2, "3");

		tested.write(Chunk.of("4"));

		assertFileExistsAndContains(2, "34f");

		tested.write(Chunk.of("5"));

		assertFileExistsAndContains(3, "5");

		tested.close();

		assertFileExistsAndContains(1, "12f");
		assertFileExistsAndContains(2, "34f");
		assertFileExistsAndContains(3, "5f");

	}

	@Test
	void testTransactionalMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("1", "2")));

		assertFileExistsAndContains(1, "12f");

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("3")));

		assertFileExistsAndContains(2, "3");

		tested.close();

		assertFileExistsAndContains(1, "12f");
		assertFileExistsAndContains(2, "3f");

	}

	@Test
	void testRestart() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		assertFileExistsAndContains(1, "12");
		assertFileExistsAndContains(2, "3");

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		tested.write(Chunk.of("4"));

		assertFileExistsAndContains(2, "34");

		tested.write(Chunk.of("5", "6", "7", "8", "9"));

		assertFileExistsAndContains(3, "56");
		assertFileExistsAndContains(4, "78");
		assertFileExistsAndContains(5, "9");
	}

	@Test
	void testRestartWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		assertFileExistsAndContains(1, "12f");
		assertFileExistsAndContains(2, "3");

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		tested.write(Chunk.of("4"));

		assertFileExistsAndContains(2, "34f");

		tested.write(Chunk.of("5", "6", "7", "8", "9"));
		tested.close();

		assertFileExistsAndContains(3, "56f");
		assertFileExistsAndContains(4, "78f");
		assertFileExistsAndContains(5, "9f");
	}

	@Test
	void testTransactionalRestartWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("1", "2")));

		assertFileExistsAndContains(1, "12f");

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("3")));

		assertFileExistsAndContains(2, "3");

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("4")));

		assertFileExistsAndContains(2, "34f");
	}

	private void assertFileExistsAndContains(int index, String expected) throws Exception {
		File part = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(index));
		assertTrue(part.exists());
		assertEquals(expected, readFile(part));
	}

}
