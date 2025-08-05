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

package org.springframework.batch.samples.file.fixed;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/file/fixed/job/fixedLength.xml",
		"/simple-job-launcher-context.xml" })
class FixedLengthFunctionalTests {

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFile", "org/springframework/batch/samples/file/fixed/data/fixedLength.txt")
			.addString("outputFile", "file:./target/test-outputs/fixedLengthOutput.txt")
			.toJobParameters();

		// when
		JobExecution jobExecution = this.jobOperatorTestUtils.startJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(FixedLengthJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFile", "org/springframework/batch/samples/file/fixed/data/fixedLength.txt")
			.addString("outputFile", "file:./target/test-outputs/fixedLengthOutput.txt")
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

}
