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

package org.springframework.batch.sample.iosample;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * @since 2.0
 */
@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/jobs/iosample/multiRecordType.xml",
		"/job-runner-context.xml" })
class MultiRecordTypeFunctionalTests {

	private static final String OUTPUT_FILE = "target/test-outputs/multiRecordTypeOutput.txt";

	private static final String INPUT_FILE = "src/main/resources/data/iosample/input/multiRecordType.txt";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	/**
	 * Output should be the same as input
	 */
	@Test
	void testJob() throws Exception {
		// when
		jobLauncherTestUtils.launchJob();

		// then
		Path inputFile = new FileSystemResource(INPUT_FILE).getFile().toPath();
		Path outputFile = new FileSystemResource(OUTPUT_FILE).getFile().toPath();
		Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));
	}

}
