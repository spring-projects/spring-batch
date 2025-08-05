/*
 * Copyright 2005-2025 the original author or authors.
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
package org.springframework.batch.core.test.ldif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/applicationContext-test2.xml" })
public class MappingLdifReaderTests {

	private static final Logger log = LoggerFactory.getLogger(MappingLdifReaderTests.class);

	private final Resource expected;

	private final Resource actual;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	public MappingLdifReaderTests() throws MalformedURLException {
		expected = new ClassPathResource("/expectedOutput.ldif");
		actual = new UrlResource("file:target/test-outputs/output.ldif");
	}

	@BeforeEach
	void checkFiles() {
		Assert.isTrue(expected.exists(), "Expected does not exist.");
	}

	@Test
	void testValidRun() throws Exception {
		JobExecution jobExecution = jobOperator.start(job1, new JobParameters());

		// Ensure job completed successfully.
		Assert.isTrue(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED),
				"Step Execution did not complete normally: " + jobExecution.getExitStatus());

		// Check output.
		Assert.isTrue(actual.exists(), "Actual does not exist.");
		Assert.isTrue(compareFiles(expected.getFile(), actual.getFile()), "Files were not equal");
	}

	@Test
	void testResourceNotExists() throws Exception {
		JobExecution jobExecution = jobOperator.start(job2, new JobParameters());

		Assert.isTrue(jobExecution.getExitStatus().getExitCode().equals("FAILED"),
				"The job exit status is not FAILED.");
		Assert.isTrue(
				jobExecution.getAllFailureExceptions().get(0).getMessage().contains("Failed to initialize the reader"),
				"The job failed for the wrong reason.");
	}

	private boolean compareFiles(File expected, File actual) throws Exception {
		boolean equal = true;

		FileInputStream expectedStream = new FileInputStream(expected);
		FileInputStream actualStream = new FileInputStream(actual);

		// Construct BufferedReader from InputStreamReader
		BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));
		BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualStream));

		String line = null;
		while ((line = expectedReader.readLine()) != null) {
			if (!line.equals(actualReader.readLine())) {
				equal = false;
				break;
			}
		}

		if (actualReader.readLine() != null) {
			equal = false;
		}

		expectedReader.close();

		return equal;
	}

}