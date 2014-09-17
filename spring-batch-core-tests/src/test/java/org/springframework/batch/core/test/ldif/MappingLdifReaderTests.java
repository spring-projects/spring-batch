/*
 * Copyright 2005-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.ldif;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/applicationContext-test2.xml"})
public class MappingLdifReaderTests {
	private static Logger log = LoggerFactory.getLogger(MappingLdifReaderTests.class);

	private Resource expected;
	private Resource actual;

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private Job validJob;

	@Autowired
	private Job invalidJob;

	public MappingLdifReaderTests() {
		try {
			expected = new ClassPathResource("/expectedOutput.ldif");
			actual = new UrlResource("file:target/test-outputs/output.ldif");
		} catch (MalformedURLException e) {
			log.error("Unexpected error", e);
		}
	}

	@Before
	public void checkFiles() {
		Assert.isTrue(expected.exists(), "Expected does not exist.");
	}

	@Test
	public void testValidRun() throws Exception {
		JobExecution jobExecution = launcher.run(validJob, new JobParameters());

		//Ensure job completed successfully.
		Assert.isTrue(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED), "Step Execution did not complete normally: " + jobExecution.getExitStatus());

		//Check output.
		assertTrue("Actual does not exist.", actual.exists());
		assertFileEquals(expected.getFile(), actual.getFile());
	}

	@Test
	public void testResourceNotExists() throws Exception {
		JobExecution jobExecution = launcher.run(invalidJob, new JobParameters());

		assertEquals("The job exit status is not FAILED.", jobExecution.getStatus(), BatchStatus.FAILED);
		assertTrue("The job failed for the wrong reason.", jobExecution.getStepExecutions().iterator().next().getExitStatus().getExitDescription().contains("Failed to initialize the reader"));
	}

	public static void assertFileEquals(File expected, File actual) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(expected));
		BufferedReader actualReader = new BufferedReader(new FileReader(actual));
		try {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				assertEquals("Line number " + lineNum + " does not match.", expectedLine, actualLine);
			}

			String actualLine = actualReader.readLine();
			assertEquals("More lines than expected.  There should not be a line number " + lineNum + ".", null,
								actualLine);
		} finally {
			expectedReader.close();
			actualReader.close();
		}
	}
}
