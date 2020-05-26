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
package org.springframework.batch.item.support;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Common parent class for {@link SynchronizedItemStreamWriterTests} and
 * {@link org.springframework.batch.item.support.builder.SynchronizedItemStreamWriterBuilderTests}
 *
 * @author Dimitrios Liapis
 *
 */
public abstract class AbstractSynchronizedItemStreamWriterTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	protected ItemStreamWriter<Object> delegate;

	private SynchronizedItemStreamWriter<Object> synchronizedItemStreamWriter;
	private final List<Object> testList = Collections.unmodifiableList(new ArrayList<>());
	private final ExecutionContext testExecutionContext = new ExecutionContext();

	abstract protected SynchronizedItemStreamWriter<Object> createNewSynchronizedItemStreamWriter();

	@Before
	public void init() {
		initMocks(this);
		synchronizedItemStreamWriter = createNewSynchronizedItemStreamWriter();
	}

	@Test
	public void testDelegateWriteIsCalled() throws Exception {
		synchronizedItemStreamWriter.write(testList);
		verify(delegate).write(testList);
	}

	@Test
	public void testDelegateOpenIsCalled() {
		synchronizedItemStreamWriter.open(testExecutionContext);
		verify(delegate).open(testExecutionContext);
	}

	@Test
	public void testDelegateUpdateIsCalled() {
		synchronizedItemStreamWriter.update(testExecutionContext);
		verify(delegate).update(testExecutionContext);
	}

	@Test
	public void testDelegateCloseIsClosed() {
		synchronizedItemStreamWriter.close();
		verify(delegate).close();
	}

}
