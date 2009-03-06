/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 * 
 */
public class JobParameterExecutionContextCopyListenerTests {

	private JobParameterExecutionContextCopyListener listener = new JobParameterExecutionContextCopyListener();

	private StepExecution stepExecution;

	@Before
	public void createExecution() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		stepExecution = new StepExecution("foo", new JobExecution(new JobInstance(123L, jobParameters, "job")));
	}

	@Test
	public void testBeforeStep() {
		listener.beforeStep(stepExecution);
		assertEquals("bar", stepExecution.getExecutionContext().get("foo"));
	}

	@Test
	public void testSetKeys() {
		listener.setKeys(new String[]{});
		listener.beforeStep(stepExecution);
		assertFalse(stepExecution.getExecutionContext().containsKey("foo"));
	}

}
