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
package org.springframework.batch.core.launch.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
class JobRegistryBackgroundJobRunnerTests {

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.JobRegistryBackgroundJobRunner#main(java.lang.String[])}.
	 */
	@Test
	void testMain() throws Exception {
		JobRegistryBackgroundJobRunner.main(
				ClassUtils.addResourcePathToPackagePath(getClass(), "test-environment-with-registry.xml"),
				ClassUtils.addResourcePathToPackagePath(getClass(), "job.xml"));
		assertEquals(0, JobRegistryBackgroundJobRunner.getErrors().size());
	}

	@Test
	void testMainWithAutoRegister() throws Exception {
		JobRegistryBackgroundJobRunner.main(
				ClassUtils.addResourcePathToPackagePath(getClass(),
						"test-environment-with-registry-and-auto-register.xml"),
				ClassUtils.addResourcePathToPackagePath(getClass(), "job.xml"));
		assertEquals(0, JobRegistryBackgroundJobRunner.getErrors().size());
	}

	@Test
	void testMainWithJobLoader() throws Exception {
		JobRegistryBackgroundJobRunner.main(
				ClassUtils.addResourcePathToPackagePath(getClass(), "test-environment-with-loader.xml"),
				ClassUtils.addResourcePathToPackagePath(getClass(), "job.xml"));
		assertEquals(0, JobRegistryBackgroundJobRunner.getErrors().size());
	}

	@BeforeEach
	void setUp() {
		JobRegistryBackgroundJobRunner.getErrors().clear();
		System.setProperty(JobRegistryBackgroundJobRunner.EMBEDDED, "");
	}

	@AfterEach
	void tearDown() {
		System.clearProperty(JobRegistryBackgroundJobRunner.EMBEDDED);
		JobRegistryBackgroundJobRunner.getErrors().clear();
		JobRegistryBackgroundJobRunner.stop();
	}

}
