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

package org.springframework.batch.samples.file.multiresource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dan Garrette
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@Disabled("Failing on the CI platform but not locally")
@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/file/multiresource/job/multiResource.xml",
		"/simple-job-launcher-context.xml" })
class MultiResourceFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFiles", "org/springframework/batch/samples/file/multiresource/data/delimited*.csv")
			.addString("outputFiles", "file:./target/test-outputs/multiResourceOutput.csv")
			.toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MultiResourceJobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFiles", "org/springframework/batch/samples/file/multiresource/data/delimited*.csv")
			.addString("outputFiles", "file:./target/test-outputs/multiResourceOutput.csv")
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

}
