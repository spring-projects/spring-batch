/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.test.football;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/footballJob.xml" })
public class FootballJobIntegrationTests {

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@Test
	void testLaunchJob() throws Exception {
		JobExecution execution = jobOperator.start(job,
				new JobParametersBuilder().addLong("commit.interval", 10L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		for (StepExecution stepExecution : execution.getStepExecutions()) {
			logger.info("Processed: " + stepExecution);
			if (stepExecution.getStepName().equals("playerload")) {
				// The effect of the retries
				assertEquals((int) Math.ceil(stepExecution.getReadCount() / 10. + 1), stepExecution.getCommitCount());
			}
		}
	}

}
