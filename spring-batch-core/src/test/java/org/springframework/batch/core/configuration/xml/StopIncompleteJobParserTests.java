/*
 * Copyright 2006-2021 the original author or authors.
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
// FIXME this test fails when upgrading the batch xsd from 2.2 to 3.0: https://github.com/spring-projects/spring-batch/issues/1287
public class StopIncompleteJobParserTests extends AbstractJobParserTests {

	@Test
	public void testStopIncomplete() throws Exception {

		//
		// First Launch
		//
		JobExecution jobExecution = createJobExecution();
		job.execute(jobExecution);
		assertEquals(1, stepNamesList.size());
		assertEquals("Wrong steps executed: "+stepNamesList, "[fail]", stepNamesList.toString());

		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		assertEquals(ExitStatus.STOPPED.getExitCode(), jobExecution.getExitStatus().getExitCode());

		StepExecution stepExecution1 = getStepExecution(jobExecution, "fail");
		assertEquals(BatchStatus.ABANDONED, stepExecution1.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution1.getExitStatus().getExitCode());

		//
		// Second Launch
		//
		stepNamesList.clear();
		jobExecution = createJobExecution();
		job.execute(jobExecution);
		assertEquals(1, stepNamesList.size()); // step1 is not executed
		assertEquals("Wrong steps executed: "+stepNamesList, "[s2]", stepNamesList.toString());

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		StepExecution stepExecution2 = getStepExecution(jobExecution, "s2");
		assertEquals(BatchStatus.COMPLETED, stepExecution2.getStatus());
		assertEquals(ExitStatus.COMPLETED, stepExecution2.getExitStatus());

	}

}
