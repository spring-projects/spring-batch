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

package org.springframework.batch.samples.file.multirecordtype;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * @since 2.0
 */
@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/file/multirecordtype/job/multiRecordType.xml",
		"/simple-job-launcher-context.xml" })
class MultiRecordTypeFunctionalTests {

	private static final String OUTPUT_FILE = "target/test-outputs/multiRecordTypeOutput.txt";

	private static final String INPUT_FILE = "org/springframework/batch/samples/file/multirecordtype/data/multiRecordType.txt";

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Test
	void testLaunchJobWithXmlConfig() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("inputFile", INPUT_FILE)
			.addString("outputFile", "file:./" + OUTPUT_FILE)
			.toJobParameters();

		// when
		JobExecution jobExecution = this.jobOperatorTestUtils.startJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Path inputFile = new ClassPathResource(INPUT_FILE).getFile().toPath();
		Path outputFile = new FileSystemResource(OUTPUT_FILE).getFile().toPath();
		Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));
	}

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MultiRecordTypeJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder().addString("inputFile", INPUT_FILE)
			.addString("outputFile", "file:./" + OUTPUT_FILE)
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Path inputFile = new ClassPathResource(INPUT_FILE).getFile().toPath();
		Path outputFile = new FileSystemResource(OUTPUT_FILE).getFile().toPath();
		Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));
	}

}
