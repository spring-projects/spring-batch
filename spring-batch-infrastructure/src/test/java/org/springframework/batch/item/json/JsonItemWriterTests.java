/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.json;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author Mahmoud Ben Hassine
 */
public class JsonItemWriterTests {

	private Resource resource;
	private LineAggregator<String> lineAggregator;

	@Before
	public void setUp() throws Exception {
		File file = Files.createTempFile("test", "json").toFile();
		this.resource = new FileSystemResource(file);
		this.lineAggregator = Mockito.mock(LineAggregator.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void resourceMustNotBeNull() {
		new JsonItemWriter<>(null, this.lineAggregator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void lineAggregatorMustNotBeNull() {
		new JsonItemWriter<>(this.resource, null);
	}

	@Test
	public void itemsShouldBeTransformedToJsonWithTheLineAggregator() throws Exception {
		// given
		JsonItemWriter<String> writer = new JsonItemWriter<>(this.resource, this.lineAggregator);

		// when
		writer.open(new ExecutionContext());
		writer.write(Arrays.asList("foo", "bar"));
		writer.close();

		// then
		Mockito.verify(this.lineAggregator).aggregate("foo");
		Mockito.verify(this.lineAggregator).aggregate("bar");
	}
}
