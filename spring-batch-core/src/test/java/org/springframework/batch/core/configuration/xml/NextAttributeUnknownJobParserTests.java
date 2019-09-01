/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @since 2.1.9
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NextAttributeUnknownJobParserTests extends AbstractJobParserTests {

	@Test
	public void testDefaultUnknown() throws Exception {

		JobExecution jobExecution = createJobExecution();
		job.execute(jobExecution);
		assertEquals(3, stepNamesList.size());
		assertEquals("[s1, unknown, s2]", stepNamesList.toString());

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		StepExecution stepExecution1 = getStepExecution(jobExecution, "s1");
		assertEquals(BatchStatus.COMPLETED, stepExecution1.getStatus());
		assertEquals(ExitStatus.COMPLETED, stepExecution1.getExitStatus());

		StepExecution stepExecution2 = getStepExecution(jobExecution, "unknown");
		assertEquals(BatchStatus.UNKNOWN, stepExecution2.getStatus());
		assertEquals(ExitStatus.UNKNOWN, stepExecution2.getExitStatus());

	}
	
	public static class UnknownListener extends StepExecutionListenerSupport {
		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			stepExecution.setStatus(BatchStatus.UNKNOWN);
			return ExitStatus.UNKNOWN;
		}
	}

}
