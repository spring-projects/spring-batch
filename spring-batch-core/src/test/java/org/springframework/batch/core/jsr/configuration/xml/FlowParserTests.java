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
package org.springframework.batch.core.jsr.configuration.xml;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.batch.api.Batchlet;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Unit tests around {@link FlowParser}.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FlowParserTests {
	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Test
	public void testDuplicateTransitionPatternsAllowed() throws Exception {
		JobExecution stoppedExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(ExitStatus.STOPPED.getExitCode(), stoppedExecution.getExitStatus().getExitCode());

		JobExecution endedExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(ExitStatus.COMPLETED.getExitCode(), endedExecution.getExitStatus().getExitCode());

		JobExecution failedExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(ExitStatus.FAILED.getExitCode(), failedExecution.getExitStatus().getExitCode());
	}

	public static class TestBatchlet implements Batchlet {
		private static int CNT;

		@Inject
		private StepContext stepContext;

		@Override
		public String process() throws Exception {
			String exitCode = "DISTINCT";

			if("step3".equals(stepContext.getStepName())) {
				exitCode = CNT % 2 == 0 ? "DISTINCT" : "RESTART";
				CNT++;
			}

			return exitCode;
		}

		@Override
		public void stop() throws Exception { }
	}
}
