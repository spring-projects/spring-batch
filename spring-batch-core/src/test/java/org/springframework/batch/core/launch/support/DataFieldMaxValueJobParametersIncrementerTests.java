/*
 * Copyright 2020 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
public class DataFieldMaxValueJobParametersIncrementerTests {

	private final DataFieldMaxValueIncrementer incrementer = mock(DataFieldMaxValueIncrementer.class);

	@Test
	public void testInvalidKey() {
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer
				= new DataFieldMaxValueJobParametersIncrementer(this.incrementer);
		try {
			jobParametersIncrementer.setKey("");
			fail("Must fail if the key is empty");
		}
		catch (IllegalArgumentException exception) {
			Assert.assertEquals("key must not be null or empty", exception.getMessage());
		}
	}

	@Test
	public void testInvalidDataFieldMaxValueIncrementer() {
		try {
			new DataFieldMaxValueJobParametersIncrementer(null);
			fail("Must fail if the incrementer is null");
		}
		catch (IllegalArgumentException exception) {
			Assert.assertEquals("dataFieldMaxValueIncrementer must not be null", exception.getMessage());
		}
	}

	@Test
	public void testGetNext() {
		// given
		JobParameters jobParameters = new JobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer
				= new DataFieldMaxValueJobParametersIncrementer(this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		Assert.assertEquals(new Long(10) ,runId);
	}

	@Test
	public void testGetNextAppend() {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("foo", "bar")
				.toJobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer
				= new DataFieldMaxValueJobParametersIncrementer(this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		String foo = nextParameters.getString("foo");
		Assert.assertEquals(new Long(10) ,runId);
		Assert.assertEquals("bar" ,foo);
	}

	@Test
	public void testGetNextOverride() {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("run.id", 1L)
				.toJobParameters();
		when(this.incrementer.nextLongValue()).thenReturn(10L);
		DataFieldMaxValueJobParametersIncrementer jobParametersIncrementer
				= new DataFieldMaxValueJobParametersIncrementer(this.incrementer);

		// when
		JobParameters nextParameters = jobParametersIncrementer.getNext(jobParameters);

		// then
		Long runId = nextParameters.getLong("run.id");
		Assert.assertEquals(new Long(10) ,runId);
	}
}