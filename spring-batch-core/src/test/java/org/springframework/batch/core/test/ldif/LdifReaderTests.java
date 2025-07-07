/*
 * Copyright 2005-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/applicationContext-test1.xml" })
public class LdifReaderTests {

	private final Resource expected;

	private final Resource actual;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	public LdifReaderTests() throws MalformedURLException {
		expected = new ClassPathResource("/expectedOutput.ldif");
		actual = new UrlResource("file:target/test-outputs/output.ldif");
	}

	@BeforeEach
	void checkFiles() {
		Assert.isTrue(expected.exists(), "Expected does not exist.");
	}

	@Test
	void testValidRun() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job1, new JobParameters());

		// Ensure job completed successfully.
		Assert.isTrue(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED),
				"Step Execution did not complete normally: " + jobExecution.getExitStatus());

		// Check output.
		Assert.isTrue(actual.exists(), "Actual does not exist.");
		compareFiles(expected.getFile(), actual.getFile());
	}

	@Test
	void testResourceNotExists() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job2, new JobParameters());

		Assert.isTrue(jobExecution.getExitStatus().getExitCode().equals("FAILED"),
				"The job exit status is not FAILED.");
		Assert.isTrue(
				jobExecution.getAllFailureExceptions().get(0).getMessage().contains("Failed to initialize the reader"),
				"The job failed for the wrong reason.");
	}

	private void compareFiles(File expected, File actual) throws Exception {
		try (BufferedReader expectedReader = new BufferedReader(new FileReader(expected));
				BufferedReader actualReader = new BufferedReader(new FileReader(actual))) {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				assertEquals(expectedLine, actualLine, "Line number " + lineNum + " does not match.");
			}

			String actualLine = actualReader.readLine();
			assertNull(actualLine, "More lines than expected.  There should not be a line number " + lineNum + ".");
		}
	}

}