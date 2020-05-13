/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.support.builder;

import org.junit.Test;
import org.springframework.batch.item.support.AbstractSynchronizedItemStreamWriterTests;
import org.springframework.batch.item.support.SynchronizedItemStreamWriter;

/**
 *
 * @author Dimitrios Liapis
 *
 */
public class SynchronizedItemStreamWriterBuilderTests extends AbstractSynchronizedItemStreamWriterTests {


	@Override
	protected SynchronizedItemStreamWriter<Object> createNewSynchronizedItemStreamWriter() {
		return new SynchronizedItemStreamWriterBuilder<>()
				.delegate(delegate)
				.build();
	}

	@Test
	public void testBuilderDelegateIsNotNull() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("A delegate item writer is required");
		new SynchronizedItemStreamWriterBuilder<>().build();
	}
}
