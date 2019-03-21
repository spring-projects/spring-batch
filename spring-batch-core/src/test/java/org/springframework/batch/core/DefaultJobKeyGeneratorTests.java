/*
 * Copyright 2013-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DefaultJobKeyGeneratorTests {

	private JobKeyGenerator<JobParameters> jobKeyGenerator;

	@Before
	public void setUp() throws Exception {
		jobKeyGenerator = new DefaultJobKeyGenerator();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullParameters() {
		jobKeyGenerator.generateKey(null);
	}

	@Test
	public void testMixedParameters() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString(
				"foo", "bar").addString("bar", "foo").toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString(
				"foo", "bar", true).addString("bar", "foo", true)
				.addString("ignoreMe", "irrelevant", false).toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

	@Test
	public void testCreateJobKey() {
		JobParameters jobParameters = new JobParametersBuilder().addString(
				"foo", "bar").addString("bar", "foo").toJobParameters();
		String key = jobKeyGenerator.generateKey(jobParameters);
		assertEquals(32, key.length());
	}

	@Test
	public void testCreateJobKeyWithNullParameter() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString(
				"foo", "bar").addString("bar", null).toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addString(
				"foo", "bar").addString("bar", "").toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}

	@Test
	public void testCreateJobKeyOrdering() {
		JobParameters jobParameters1 = new JobParametersBuilder().addString(
				"foo", "bar").addString("bar", "foo").toJobParameters();
		String key1 = jobKeyGenerator.generateKey(jobParameters1);
		JobParameters jobParameters2 = new JobParametersBuilder().addString(
				"bar", "foo").addString("foo", "bar").toJobParameters();
		String key2 = jobKeyGenerator.generateKey(jobParameters2);
		assertEquals(key1, key2);
	}
}
