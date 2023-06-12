/*
 * Copyright 2008-2023 the original author or authors.
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

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
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

		private Chunk<? extends String> list;

		public WriterCallback(Chunk<? extends String> list) {
			super();
			this.list = list;
		}

		@Override
		public Void doInTransaction(TransactionStatus status) {
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
		delegate = new FlatFileItemWriter<>();
		delegate.setLineAggregator(new PassThroughLineAggregator<>());
	}

	@Test
	void testBasicMultiResourceWriteScenario() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		tested.write(Chunk.of("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.write(Chunk.of("5"));
		assertEquals("45", readFile(part2));

		tested.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
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
		assertEquals(0, executionContext.getInt(tested.getExecutionContextKey("resource.item.count")));
		assertEquals(2, executionContext.getInt(tested.getExecutionContextKey("resource.index")));

	}

	@Test
	void testMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		tested.write(Chunk.of("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());

		tested.close();

		assertEquals("123f", readFile(part1));
		assertEquals("4f", readFile(part2));

	}

	@Test
	void testTransactionalMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("1", "2", "3")));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("4")));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());

		tested.close();

		assertEquals("123f", readFile(part1));
		assertEquals("4f", readFile(part2));

	}

	@Test
	void testRestart() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		tested.write(Chunk.of("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		tested.write(Chunk.of("5"));
		assertEquals("45", readFile(part2));

		tested.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	@Test
	void testRestartWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Chunk.of("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123f", readFile(part1));

		tested.write(Chunk.of("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		tested.write(Chunk.of("5"));
		assertEquals("45f", readFile(part2));

		tested.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789f", readFile(part3));
	}

	@Test
	void testTransactionalRestartWithFooter() throws Exception {

		delegate.setFooterCallback(writer -> writer.write("f"));
		super.setUp(delegate);
		tested.open(executionContext);

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("1", "2", "3")));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123f", readFile(part1));

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("4")));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();

		tested.open(executionContext);

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Chunk.of("5")));
		assertEquals("45f", readFile(part2));
	}

}
