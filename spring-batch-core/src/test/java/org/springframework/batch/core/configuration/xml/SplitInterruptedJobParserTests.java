/*
 * Copyright 2006-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@SpringJUnitConfig
class SplitInterruptedJobParserTests extends AbstractJobParserTests {

	@Test
	void testSplitInterrupted() throws Exception {

		final JobExecution jobExecution = createJobExecution();
		new Thread(() -> {
			try {
				job.execute(jobExecution);
			}
			catch (JobInterruptedException e) {
				throw new RuntimeException(e);
			}
		}).start();

		Thread.sleep(100L);
		jobExecution.setStatus(BatchStatus.STOPPING);
		Thread.sleep(200L);
		int count = 0;
		while (jobExecution.getStatus() == BatchStatus.STOPPING && count++ < 10) {
			Thread.sleep(200L);
		}
		assertTrue(count < 10, "Timed out waiting for job to stop: " + jobExecution);

		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		assertEquals(ExitStatus.STOPPED.getExitCode(), jobExecution.getExitStatus().getExitCode());

		assertTrue(stepNamesList.contains("stop"), "Wrong step names: " + stepNamesList);

		StepExecution stepExecution = getStepExecution(jobExecution, "stop");
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
		assertEquals(ExitStatus.STOPPED.getExitCode(), stepExecution.getExitStatus().getExitCode());

		assertEquals(1, stepNamesList.size());

	}

}
