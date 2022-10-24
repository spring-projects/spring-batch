/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.sample;

import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.jupiter.api.Test;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/headerFooterSample.xml", "/job-runner-context.xml" })
class HeaderFooterSampleFunctionalTests {

	@Autowired
	@Qualifier("inputResource")
	private Resource input;

	@Autowired
	@Qualifier("outputResource")
	private Resource output;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testJob() throws Exception {
		this.jobLauncherTestUtils.launchJob();

		BufferedReader inputReader = new BufferedReader(new FileReader(input.getFile()));
		BufferedReader outputReader = new BufferedReader(new FileReader(output.getFile()));

		// skip initial comment from input file
		inputReader.readLine();

		String line;

		int lineCount = 0;
		while ((line = inputReader.readLine()) != null) {
			lineCount++;
			assertTrue("input line should correspond to output line", outputReader.readLine().contains(line));
		}

		// footer contains the item count
		int itemCount = lineCount - 1; // minus 1 due to header line
		assertTrue("OutputReader did not contain the values specified",
				outputReader.readLine().contains(String.valueOf(itemCount)));

		inputReader.close();
		outputReader.close();
	}

}
