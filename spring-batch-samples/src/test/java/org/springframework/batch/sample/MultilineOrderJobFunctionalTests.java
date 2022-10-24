/*
 * Copyright 2006-2022 the original author or authors.
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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/multilineOrderJob.xml", "/job-runner-context.xml" })
class MultilineOrderJobFunctionalTests {

	private static final String ACTUAL = "target/test-outputs/multilineOrderOutput.txt";

	private static final String EXPECTED = "data/multilineOrderJob/result/multilineOrderOutput.txt";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testJobLaunch() throws Exception {
		this.jobLauncherTestUtils.launchJob();
		Path expectedFile = new ClassPathResource(EXPECTED).getFile().toPath();
		Path actualFile = new FileSystemResource(ACTUAL).getFile().toPath();
		Assertions.assertLinesMatch(Files.lines(expectedFile), Files.lines(actualFile));
	}

}
