/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.step.batchlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.batch.api.Batchlet;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class BatchletAdapterTests {

	private BatchletAdapter adapter;
	@Mock
	private Batchlet delegate;
	@Mock
	private StepContribution contribution;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new BatchletAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new BatchletAdapter(null);
	}

	@Test
	public void testExecuteNoExitStatus() throws Exception {
		assertEquals(RepeatStatus.FINISHED, adapter.execute(contribution, new ChunkContext(null)));

		verify(delegate).process();
	}

	@Test
	public void testExecuteWithExitStatus() throws Exception {
		when(delegate.process()).thenReturn("my exit status");

		assertEquals(RepeatStatus.FINISHED, adapter.execute(contribution, new ChunkContext(null)));

		verify(delegate).process();
		verify(contribution).setExitStatus(new ExitStatus("my exit status"));
	}

	@Test
	public void testStop() throws Exception{
		adapter.stop();
		verify(delegate).stop();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testStopException() throws Exception{
		doThrow(new Exception("expected")).when(delegate).stop();
		adapter.stop();
	}
}
