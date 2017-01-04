/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.test.infinite_loop_on_retry;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Yoann GENDRE
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml",
		"/META-INF/batch/infiniteLoopOnRetryJob.xml" })
public class InfiniteLoopOnRetryJobTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	private static final String outputFilePath = "build/output.txt";
	private static final String expectedFilePath = "src/test/resources/expectedOutputInfiniteLoopOnRetryJob.txt";

	@SuppressWarnings("unused")
	@Test
	public void testInfiniteLoop() throws Exception {
		// 7 items (item1,item2,item3,item4,item5,item6,item7)
		// commit-interval= 5
		// writer fail on 1rst chunk (writer.cpt=1)
		// processor fail on 1rst item during scan (processor.cpt=6) and on 3rd
		// item during scan (processor.cpt=8)
		// expected output =
		// Item:id=3,nbProcessed=2,nbWritten=2
		// Item:id=4,nbProcessed=2,nbWritten=2
		// Item:id=5,nbProcessed=2,nbWritten=2
		// Item:id=6,nbProcessed=1,nbWritten=1
		// Item:id=7,nbProcessed=1,nbWritten=1

		JobExecution execution = jobLauncher.run(
				job,
				new JobParametersBuilder().addString("outputFilePath",
						outputFilePath).toJobParameters());

		org.junit.Assert.assertEquals("BatchStatus", BatchStatus.COMPLETED,
				execution.getStatus());

		checkOutput(new FileSystemResource(expectedFilePath).getFile(), new FileSystemResource(outputFilePath).getFile());
	}

	private void checkOutput(File expected, File actual) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(
				expected));
		BufferedReader actualReader = new BufferedReader(new FileReader(actual));
		try {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader
					.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				assertEquals("Line number " + lineNum + " does not match.",
						expectedLine, actualLine);
			}

			String actualLine = actualReader.readLine();
			assertEquals(
					"More lines than expected.  There should not be a line number "
							+ lineNum + ".", null, actualLine);
		} finally {
			expectedReader.close();
			actualReader.close();
		}
	}

}
