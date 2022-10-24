/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.sample;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * @since 2.0
 */
@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/restartFileSampleJob.xml", "/job-runner-context.xml" })
class RestartFileSampleFunctionalTests {

	@Autowired
	private Resource outputResource;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void runTest() throws Exception {
		JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParameters();

		JobExecution je1 = jobLauncherTestUtils.launchJob(jobParameters);
		assertEquals(BatchStatus.FAILED, je1.getStatus());
		Path outputResourceFile = outputResource.getFile().toPath();
		Assertions.assertEquals(10, Files.lines(outputResourceFile).count());

		JobExecution je2 = jobLauncherTestUtils.launchJob(jobParameters);
		assertEquals(BatchStatus.COMPLETED, je2.getStatus());
		outputResourceFile = outputResource.getFile().toPath();
		Assertions.assertEquals(20, Files.lines(outputResourceFile).count());
	}

	static class CustomerCreditFlatFileItemWriter extends FlatFileItemWriter<CustomerCredit> {

		private boolean failed = false;

		@Override
		public void write(Chunk<? extends CustomerCredit> arg0) throws Exception {
			for (CustomerCredit cc : arg0) {
				if (!failed && cc.getName().equals("customer13")) {
					failed = true;
					throw new RuntimeException();
				}
			}
			super.write(arg0);
		}

	}

}