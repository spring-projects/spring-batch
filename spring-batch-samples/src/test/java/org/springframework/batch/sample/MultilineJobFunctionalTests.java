/*
 * Copyright 2006-2007 the original author or authors.
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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
		locations = { "/simple-job-launcher-context.xml", "/jobs/multilineJob.xml", "/job-runner-context.xml" })
public class MultilineJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	// The output is grouped together in two lines, instead of all the
	// trades coming out on a single line.
	private static final String EXPECTED_RESULT = "[Trade: [isin=UK21341EAH45,quantity=978,price=98.34,customer=customer1], Trade: [isin=UK21341EAH46,quantity=112,price=18.12,customer=customer2]]"
			+ "[Trade: [isin=UK21341EAH47,quantity=245,price=12.78,customer=customer2], Trade: [isin=UK21341EAH48,quantity=108,price=9.25,customer=customer3], Trade: [isin=UK21341EAH49,quantity=854,price=23.39,customer=customer4]]";

	private Resource output = new FileSystemResource("target/test-outputs/20070122.testStream.multilineStep.txt");

	@Test
	public void testJobLaunch() throws Exception {
		jobLauncherTestUtils.launchJob();
		assertEquals(EXPECTED_RESULT, StringUtils.replace(IOUtils.toString(output.getInputStream(), "UTF-8"),
				System.getProperty("line.separator"), ""));
	}

}
