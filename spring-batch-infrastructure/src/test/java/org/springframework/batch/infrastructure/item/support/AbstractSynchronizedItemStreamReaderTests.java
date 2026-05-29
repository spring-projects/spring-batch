/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilderTests;

import static org.mockito.Mockito.verify;

/**
 * Common parent class for {@link SynchronizedItemStreamReaderTests} and
 * {@link SynchronizedItemStreamReaderBuilderTests}
 *
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 *
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractSynchronizedItemStreamReaderTests {

	@Mock
	protected ItemStreamReader<Object> delegate;

	protected SynchronizedItemStreamReader<Object> synchronizedItemStreamReader;

	protected final ExecutionContext testExecutionContext = new ExecutionContext();

	abstract protected SynchronizedItemStreamReader<Object> createNewSynchronizedItemStreamReader();

	@BeforeEach
	void init() {
		this.synchronizedItemStreamReader = createNewSynchronizedItemStreamReader();
	}

	@Test
	void testDelegateReadIsCalled() throws Exception {
		this.synchronizedItemStreamReader.read();
		verify(this.delegate).read();
	}

	@Test
	void testDelegateOpenIsCalled() {
		this.synchronizedItemStreamReader.open(this.testExecutionContext);
		verify(this.delegate).open(this.testExecutionContext);
	}

	@Test
	void testDelegateUpdateIsCalled() {
		this.synchronizedItemStreamReader.update(this.testExecutionContext);
		verify(this.delegate).update(this.testExecutionContext);
	}

	@Test
	void testDelegateCloseIsClosed() {
		this.synchronizedItemStreamReader.close();
		verify(this.delegate).close();
	}

}
