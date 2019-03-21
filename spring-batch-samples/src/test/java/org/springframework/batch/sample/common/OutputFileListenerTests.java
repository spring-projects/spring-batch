/*
 * Copyright 2009 the original author or authors.
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
package org.springframework.batch.sample.common;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import static org.junit.Assert.assertEquals;

public class OutputFileListenerTests {
	private OutputFileListener listener = new OutputFileListener();
	private StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 1L);
	
	@Test
	public void testCreateOutputNameFromInput() {
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{outputFile=file:./build/output/foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetPath() {
		listener.setPath("spam/");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{outputFile=spam/foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetOutputKeyName() {
		listener.setPath("");
		listener.setOutputKeyName("spam");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{spam=foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetInputKeyName() {
		listener.setPath("");
		listener.setInputKeyName("spam");
		stepExecution.getExecutionContext().putString("spam", "bar");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("bar.csv", stepExecution.getExecutionContext().getString("outputFile"));
	}
}
