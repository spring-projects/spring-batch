/*
 * Copyright 2008-2022 the original author or authors.
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

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
class JobParameterTests {

	@Test
	void testStringParameter() {
		JobParameter<String> jobParameter = new JobParameter<>("param", "test", String.class, true);
		assertEquals("param", jobParameter.name());
		assertEquals("test", jobParameter.value());
		assertEquals(String.class, jobParameter.type());
		assertTrue(jobParameter.identifying());
	}

	@Test
	void testLongParameter() {
		JobParameter<Long> jobParameter = new JobParameter<>("param", 1L, Long.class, true);
		assertEquals("param", jobParameter.name());
		assertEquals(1L, jobParameter.value());
		assertEquals(Long.class, jobParameter.type());
		assertTrue(jobParameter.identifying());
	}

	@Test
	void testDoubleParameter() {
		JobParameter<Double> jobParameter = new JobParameter<>("param", 1.1, Double.class, true);
		assertEquals("param", jobParameter.name());
		assertEquals(1.1, jobParameter.value());
		assertEquals(Double.class, jobParameter.type());
		assertTrue(jobParameter.identifying());
	}

	@Test
	void testDateParameter() {
		Date epoch = new Date(0L);
		JobParameter<Date> jobParameter = new JobParameter<>("param", epoch, Date.class, true);
		assertEquals("param", jobParameter.name());
		assertEquals(new Date(0L), jobParameter.value());
		assertEquals(Date.class, jobParameter.type());
		assertTrue(jobParameter.identifying());
	}

	// Job parameters are equal if their names are equal

	@Test
	void testEquals() {
		JobParameter<String> jobParameter = new JobParameter<>("param", "test1", String.class, true);
		JobParameter<String> testParameter = new JobParameter<>("param", "test2", String.class, true);
		assertEquals(jobParameter, testParameter);
	}

	@Test
	void testHashcode() {
		JobParameter<String> jobParameter = new JobParameter<>("param", "test1", String.class, true);
		JobParameter<String> testParameter = new JobParameter<>("param", "test2", String.class, true);
		assertEquals(testParameter.hashCode(), jobParameter.hashCode());
	}

	@Test
	void testAddingNullJobParameters() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new JobParametersBuilder().addString("foo", null).toJobParameters());
		Assertions.assertEquals("Value for parameter 'foo' must not be null", exception.getMessage());
	}

}