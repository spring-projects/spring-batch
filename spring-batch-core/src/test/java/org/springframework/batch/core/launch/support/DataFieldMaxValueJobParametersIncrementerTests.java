/*
 * Copyright 2020-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
class DataFieldMaxValueJobParametersIncrementerTests {

	private final DataFieldMaxValueIncrementer incrementer = mock();

	@Test
	void testInvalidKey() {
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer = new DataFieldMaxValueJobParametersIncrementer(
				this.incrementer);
		Exception exception = assertThrows(IllegalArgumentException.class, () -> jobParametersIncrementer.setKey(""));
		assertEquals("key must not be null or empty", exception.getMessage());
	}

	@Test
	void testInvalidDataFieldMaxValueIncrementer() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new DataFieldMaxValueJobParametersIncrementer(null));
		assertEquals("dataFieldMaxValueIncrementer must not be null", exception.getMessage());
	}

	@Test
	void testGetNext() {
		// given
		JobParameters jobParameters = new JobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer = new DataFieldMaxValueJobParametersIncrementer(
				this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		assertEquals(Long.valueOf(10L), runId);
	}

	@Test
	void testGetNextAppend() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer = new DataFieldMaxValueJobParametersIncrementer(
				this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		String foo = nextParameters.getString("foo");
		assertEquals(Long.valueOf(10L), runId);
		assertEquals("bar", foo);
	}

	@Test
	void testGetNextOverride() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addLong("run.id", 1L).toJobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer = new DataFieldMaxValueJobParametersIncrementer(
				this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		assertEquals(Long.valueOf(10L), runId);
	}

}