/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support.state;

import static org.junit.Assert.assertEquals;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Test;
import org.springframework.batch.core.jsr.JsrTestUtils;

/**
 * Tests for the JSR-352 version of {@link EndState}
 *
 * @author Michael Minella
 */
public class EndStateTests {

	@Test
	public void test() throws Exception {
		JobExecution jobExecution = JsrTestUtils.runJob("jobWithEndTransition", null, 10000l);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
		assertEquals("SUCCESS", jobExecution.getExitStatus());
		assertEquals(1, BatchRuntime.getJobOperator().getStepExecutions(jobExecution.getExecutionId()).size());
	}

	public static class EndStateBatchlet extends AbstractBatchlet {

		@Override
		public String process() throws Exception {
			return "GOOD";
		}
	}
}
