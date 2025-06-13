/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.samples.file.json;

import java.io.File;
import java.io.FileInputStream;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.DigestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */
class JsonFunctionalTests {

	public static final String INPUT_FILE = "org/springframework/batch/samples/file/json/data/trades.json";

	public static final String OUTPUT_FILE = "target/test-outputs/trades.json";

	@Test
	void testJsonReadingAndWriting() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(JsonJobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder().addString("inputFile", INPUT_FILE)
			.addString("outputFile", "file:./" + OUTPUT_FILE)
			.toJobParameters();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		assertFileEquals(new File("src/main/resources/" + INPUT_FILE), new File(OUTPUT_FILE));
	}

	private void assertFileEquals(File expected, File actual) throws Exception {
		String expectedHash = DigestUtils.md5DigestAsHex(new FileInputStream(expected));
		String actualHash = DigestUtils.md5DigestAsHex(new FileInputStream(actual));
		assertEquals(expectedHash, actualHash);
	}

}
