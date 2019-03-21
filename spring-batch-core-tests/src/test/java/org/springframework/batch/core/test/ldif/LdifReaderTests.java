/*
 * Copyright 2005-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/applicationContext-test1.xml"})
public class LdifReaderTests {

	private Resource expected;
	private Resource actual;

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

	@Before
	public void checkFiles() {
		Assert.isTrue(expected.exists(), "Expected does not exist.");
	}

	@Test
	public void testValidRun() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job1, new JobParameters());

		//Ensure job completed successfully.
		Assert.isTrue(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED), "Step Execution did not complete normally: " + jobExecution.getExitStatus());

		//Check output.
		Assert.isTrue(actual.exists(), "Actual does not exist.");
		compareFiles(expected.getFile(), actual.getFile());
	}

	@Test
	public void testResourceNotExists() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job2, new JobParameters());

		Assert.isTrue(jobExecution.getExitStatus().getExitCode().equals("FAILED"), "The job exit status is not FAILED.");
		Assert.isTrue(jobExecution.getAllFailureExceptions().get(0).getMessage().contains("Failed to initialize the reader"), "The job failed for the wrong reason.");
	}

	private void compareFiles(File expected, File actual) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(expected));
		BufferedReader actualReader = new BufferedReader(new FileReader(actual));
		try {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				assertEquals("Line number " + lineNum + " does not match.", expectedLine, actualLine);
			}

			String actualLine = actualReader.readLine();
			assertEquals("More lines than expected.  There should not be a line number " + lineNum + ".", null, actualLine);
		}
		finally {
			expectedReader.close();
			actualReader.close();
		}
	}
}