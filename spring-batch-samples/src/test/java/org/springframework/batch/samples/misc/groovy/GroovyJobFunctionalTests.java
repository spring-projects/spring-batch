/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.samples.misc.groovy;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/misc/groovy/job/groovyJob.xml",
		"/simple-job-launcher-context.xml" })
public class GroovyJobFunctionalTests {

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@BeforeEach
	void removeOldData() throws IOException {
		FileUtils.deleteDirectory(new File("target/groovyJob"));
	}

	@Test
	void testLaunchJob() throws Exception {
		assertFalse(new File("target/groovyJob/output/files.zip").exists());
		jobOperatorTestUtils.startJob();
		assertTrue(new File("target/groovyJob/output/files.zip").exists());
	}

}
