/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.file.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriterFlatFileTests;
import org.springframework.batch.item.file.ResourceSuffixCreator;
import org.springframework.batch.item.file.SimpleResourceSuffixCreator;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
class MultiResourceItemWriterBuilderTests {

	private MultiResourceItemWriter<String> writer;

	private File file;

	private final ResourceSuffixCreator suffixCreator = new ResourceSuffixCreator() {
		@Override
		public String getSuffix(int index) {
			return "A" + index;
		}
	};

	private final ExecutionContext executionContext = new ExecutionContext();

	private FlatFileItemWriter<String> delegate;

	@BeforeEach
	void setUp() throws Exception {
		this.delegate = new FlatFileItemWriter<>();
		this.delegate.setLineAggregator(new PassThroughLineAggregator<>());
		this.file = File.createTempFile(MultiResourceItemWriterFlatFileTests.class.getSimpleName(), null);
		this.writer = null;
	}

	@AfterEach
	void tearDown() {
		if (this.writer != null) {
			this.writer.close();
		}
	}

	@Test
	void testBasicMultiResourceWriteScenario() throws Exception {

		this.writer = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).resourceSuffixCreator(this.suffixCreator)
				.itemCountLimitPerResource(2).saveState(true).name("foo").build();

		this.writer.open(this.executionContext);

		this.writer.write(Chunk.of("1", "2", "3"));

		File part1 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		this.writer.write(Chunk.of("4"));
		File part2 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		this.writer.write(Chunk.of("5"));
		assertEquals("45", readFile(part2));

		this.writer.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	@Test
	void testBasicDefaultSuffixCreator() throws Exception {

		SimpleResourceSuffixCreator simpleResourceSuffixCreator = new SimpleResourceSuffixCreator();
		this.writer = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).itemCountLimitPerResource(2).saveState(true).name("foo")
				.build();

		this.writer.open(this.executionContext);

		this.writer.write(Chunk.of("1", "2", "3"));

		File part1 = new File(this.file.getAbsolutePath() + simpleResourceSuffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		this.writer.write(Chunk.of("4"));
		File part2 = new File(this.file.getAbsolutePath() + simpleResourceSuffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));
	}

	@Test
	void testUpdateAfterDelegateClose() throws Exception {

		this.writer = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).resourceSuffixCreator(this.suffixCreator)
				.itemCountLimitPerResource(2).saveState(true).name("foo").build();

		this.writer.update(this.executionContext);
		assertEquals(0, this.executionContext.getInt(this.writer.getExecutionContextKey("resource.item.count")));
		assertEquals(1, this.executionContext.getInt(this.writer.getExecutionContextKey("resource.index")));
		this.writer.write(Chunk.of("1", "2", "3"));
		this.writer.update(this.executionContext);
		assertEquals(0, this.executionContext.getInt(this.writer.getExecutionContextKey("resource.item.count")));
		assertEquals(2, this.executionContext.getInt(this.writer.getExecutionContextKey("resource.index")));
	}

	@Test
	void testRestart() throws Exception {

		this.writer = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).resourceSuffixCreator(this.suffixCreator)
				.itemCountLimitPerResource(2).saveState(true).name("foo").build();

		this.writer.write(Chunk.of("1", "2", "3"));

		File part1 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		this.writer.write(Chunk.of("4"));
		File part2 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		this.writer.update(this.executionContext);
		this.writer.close();
		this.writer.open(this.executionContext);

		this.writer.write(Chunk.of("5"));
		assertEquals("45", readFile(part2));

		this.writer.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	@Test
	void testRestartNoSaveState() throws Exception {

		this.writer = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).resourceSuffixCreator(this.suffixCreator)
				.itemCountLimitPerResource(2).saveState(false).name("foo").build();

		this.writer.write(Chunk.of("1", "2", "3"));

		File part1 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		this.writer.write(Chunk.of("4"));
		File part2 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		this.writer.update(this.executionContext);
		this.writer.close();
		this.writer.open(this.executionContext);

		this.writer.write(Chunk.of("5"));
		assertEquals("4", readFile(part2));

		this.writer.write(Chunk.of("6", "7", "8", "9"));
		File part3 = new File(this.file.getAbsolutePath() + this.suffixCreator.getSuffix(1));
		assertTrue(part3.exists());
		assertEquals("56789", readFile(part3));
	}

	@Test
	void testSaveStateNoName() {
		var builder = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resource(new FileSystemResource(this.file)).resourceSuffixCreator(this.suffixCreator)
				.itemCountLimitPerResource(2).saveState(true);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("A name is required when saveState is true.", exception.getMessage());
	}

	@Test
	void testNoResource() {
		var builder = new MultiResourceItemWriterBuilder<String>().delegate(this.delegate)
				.resourceSuffixCreator(this.suffixCreator).itemCountLimitPerResource(2);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("resource is required.", exception.getMessage());
	}

	@Test
	void testNoDelegateNoName() {
		var builder = new MultiResourceItemWriterBuilder<String>().resource(new FileSystemResource(this.file))
				.resourceSuffixCreator(this.suffixCreator).itemCountLimitPerResource(2).saveState(false);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("delegate is required.", exception.getMessage());
	}

	private String readFile(File f) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuilder result = new StringBuilder();
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				result.append(line);
			}
		}
		finally {
			reader.close();
		}
		return result.toString();
	}

}
