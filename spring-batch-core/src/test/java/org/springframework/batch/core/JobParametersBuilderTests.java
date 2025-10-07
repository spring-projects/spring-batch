/*
 * Copyright 2008-2025 the original author or authors.
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

import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lucas Ward
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 *
 */
class JobParametersBuilderTests {

	private JobParametersBuilder parametersBuilder;

	private final Date date = new Date(System.currentTimeMillis());

	@BeforeEach
	void initialize() {
		this.parametersBuilder = new JobParametersBuilder();
	}

	@Test
	void testAddingNullJobParameters() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new JobParametersBuilder().addString("foo", null).toJobParameters());
		Assertions.assertEquals("Value for parameter 'foo' must not be null", exception.getMessage());
	}

	@Test
	void testNonIdentifyingParameters() {
		this.parametersBuilder.addDate("SCHEDULE_DATE", date, false);
		this.parametersBuilder.addLong("LONG", 1L, false);
		this.parametersBuilder.addString("STRING", "string value", false);
		this.parametersBuilder.addDouble("DOUBLE", 1.0d, false);

		JobParameters parameters = this.parametersBuilder.toJobParameters();
		assertEquals(date, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(1L, parameters.getLong("LONG").longValue());
		assertEquals("string value", parameters.getString("STRING"));
		assertEquals(1, parameters.getDouble("DOUBLE"), 1e-15);
		assertFalse(parameters.getParameter("SCHEDULE_DATE").identifying());
		assertFalse(parameters.getParameter("LONG").identifying());
		assertFalse(parameters.getParameter("STRING").identifying());
		assertFalse(parameters.getParameter("DOUBLE").identifying());
	}

	@Test
	void testToJobRuntimeParameters() {
		this.parametersBuilder.addDate("SCHEDULE_DATE", date);
		this.parametersBuilder.addLong("LONG", 1L);
		this.parametersBuilder.addString("STRING", "string value");
		this.parametersBuilder.addDouble("DOUBLE", 1.0d);
		JobParameters parameters = this.parametersBuilder.toJobParameters();
		assertEquals(date, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(1L, parameters.getLong("LONG").longValue());
		assertEquals(1, parameters.getDouble("DOUBLE"), 1e-15);
		assertEquals("string value", parameters.getString("STRING"));
	}

	@Test
	void testCopy() {
		this.parametersBuilder.addString("STRING", "string value");
		this.parametersBuilder = new JobParametersBuilder(this.parametersBuilder.toJobParameters());
		Iterator<String> parameters = this.parametersBuilder.toJobParameters()
			.parameters()
			.stream()
			.map(JobParameter::name)
			.iterator();
		assertEquals("STRING", parameters.next());
	}

	@Test
	void testAddJobParameter() {
		JobParameter<String> jobParameter = new JobParameter<>("name", "bar", String.class);
		this.parametersBuilder.addJobParameter(jobParameter);
		Set<JobParameter<?>> parameters = this.parametersBuilder.toJobParameters().parameters();
		assertEquals(1, parameters.size());
		JobParameter<?> parameter = parameters.iterator().next();
		assertEquals("name", parameter.name());
		assertEquals("bar", parameter.value());
		assertTrue(parameter.identifying());
	}

}
