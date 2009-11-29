/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.sample;

import static org.springframework.batch.test.AssertFile.assertFileEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/multilineOrderJob.xml",
		"/job-runner-context.xml" })
public class MultilineOrderJobFunctionalTests {

	private static final String ACTUAL = "target/test-outputs/multilineOrderOutput.txt";
	private static final String EXPECTED = "data/multilineOrderJob/result/multilineOrderOutput.txt";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	
	@Test
	public void testJobLaunch() throws Exception {
		jobLauncherTestUtils.launchJob();
		assertFileEquals(new ClassPathResource(EXPECTED), new FileSystemResource(ACTUAL));
	}

}
