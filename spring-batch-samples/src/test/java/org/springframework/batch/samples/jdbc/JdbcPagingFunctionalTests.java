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

package org.springframework.batch.samples.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.samples.jdbc.paging.JdbcPagingReaderBatchWriterSampleJob;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml",
		"/org/springframework/batch/samples/jdbc/job/jdbcPaging.xml" })
class JdbcPagingFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addDouble("credit", 0.)
			.toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JdbcPagingReaderBatchWriterSampleJob.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addDouble("credit", 0.).toJobParameters();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

}
