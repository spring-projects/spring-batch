/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.batch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.DefaultJobKeyGenerator;
import org.springframework.batch.core.job.JobKeyGenerator;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class DefaultJobKeyGeneratorTests {

	private final JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator();

	@Test
	void testNullParameters() {
		assertThrows(IllegalArgumentException.class, () -> jobKeyGenerator.generateKey(null));
	}

	@Test
	void testMixedParameters() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString("foo", "bar")
			.addString("bar", "foo")
			.toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString("foo", "bar", true)
			.addString("bar", "foo", true)
			.addString("ignoreMe", "irrelevant", false)
			.toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

	@Test
	void testCreateJobKey() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar")
			.addString("bar", "foo")
			.toJobParameters();
		String key = jobKeyGenerator.generateKey(jobParameters);
		assertEquals(32, key.length());
	}

	@Test
	void testCreateJobKeyOrdering() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString("foo", "bar")
			.addString("bar", "foo")
			.toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		JobParameters jobParameters2 = new JobParametersBuilder().addString("bar", "foo")
			.addString("foo", "bar")
			.toJobParameters();
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

	@Test
	public void testCreateJobKeyForEmptyParameters() {
		JobParameters jobParameters1 = new JobParameters();
		JobParameters jobParameters2 = new JobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

	@Test
	public void testCreateJobKeyForEmptyParametersAndNonIdentifying() {
		JobParameters jobParameters1 = new JobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString("name", "foo", false).toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

}
