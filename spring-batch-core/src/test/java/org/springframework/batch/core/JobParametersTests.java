/*
 * Copyright 2008-2023 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.util.SerializationUtils;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 *
 */
class JobParametersTests {

	JobParameters parameters;

	Date date1 = new Date(4321431242L);

	Date date2 = new Date(7809089900L);

	@BeforeEach
	void setUp() throws Exception {
		parameters = getNewParameters();
	}

	private JobParameters getNewParameters() {

		Set<JobParameter<?>> jobParameters = new HashSet<>();
		jobParameters.add(new JobParameter<>("string.key1", "value1", String.class, true));
		jobParameters.add(new JobParameter<>("string.key2", "value2", String.class, true));
		jobParameters.add(new JobParameter<>("long.key1", 1L, Long.class, true));
		jobParameters.add(new JobParameter<>("long.key2", 2L, Long.class, true));
		jobParameters.add(new JobParameter<>("double.key1", 1.1, Double.class, true));
		jobParameters.add(new JobParameter<>("double.key2", 2.2, Double.class, true));
		jobParameters.add(new JobParameter<>("date.key1", date1, Date.class, true));
		jobParameters.add(new JobParameter<>("date.key2", date2, Date.class, true));

		return new JobParameters(jobParameters);
	}

	@Test
	void testGetString() {
		assertEquals("value1", parameters.getString("string.key1"));
		assertEquals("value2", parameters.getString("string.key2"));
	}

	@Test
	void testGetLong() {
		assertEquals(1L, parameters.getLong("long.key1"));
		assertEquals(2L, parameters.getLong("long.key2"));
	}

	@Test
	void testGetDouble() {
		assertEquals(Double.valueOf(1.1d), parameters.getDouble("double.key1"));
		assertEquals(Double.valueOf(2.2d), parameters.getDouble("double.key2"));
	}

	@Test
	void testGetDate() {
		assertEquals(date1, parameters.getDate("date.key1"));
		assertEquals(date2, parameters.getDate("date.key2"));
	}

	@Test
	void testGetMissingLong() {
		assertNull(parameters.getLong("missing.long1"));
	}

	@Test
	void testGetMissingDouble() {
		assertNull(parameters.getDouble("missing.double1"));
	}

	@Test
	void testIsEmptyWhenEmpty() {
		assertTrue(new JobParameters().isEmpty());
	}

	@Test
	void testIsEmptyWhenNotEmpty() {
		assertFalse(parameters.isEmpty());
	}

	@Test
	void testEquals() {
		JobParameters testParameters = getNewParameters();
		assertEquals(testParameters, parameters);
	}

	@Test
	void testEqualsSelf() {
		assertEquals(parameters, parameters);
	}

	@Test
	void testEqualsDifferent() {
		assertNotEquals(parameters, new JobParameters());
	}

	@Test
	void testEqualsWrongType() {
		assertNotEquals("foo", parameters);
	}

	@Test
	void testEqualsNull() {
		assertNotEquals(null, parameters);
	}

	@Test
	void testHashCodeEqualWhenEmpty() {
		int code = new JobParameters().hashCode();
		assertEquals(code, new JobParameters().hashCode());
	}

	@Test
	void testHashCodeEqualWhenNotEmpty() {
		int code = getNewParameters().hashCode();
		assertEquals(code, parameters.hashCode());
	}

	@Test
	void testSerialization() {
		JobParameters params = getNewParameters();
		assertEquals(params, SerializationUtils.clone(params));
	}

	@Test
	void testGetIdentifyingParameters() {
		// given
		JobParameter<String> jobParameter1 = new JobParameter<>("key1", "value1", String.class, true);
		JobParameter<String> jobParameter2 = new JobParameter<>("key2", "value2", String.class, false);
		JobParameters parameters = new JobParameters(Set.of(jobParameter1, jobParameter2));

		// when
		Set<JobParameter<?>> identifyingParameters = parameters.getIdentifyingParameters();

		// then
		assertEquals(1, identifyingParameters.size());
		JobParameter<?> jobParameter = identifyingParameters.iterator().next();
		assertEquals(jobParameter1, jobParameter);
	}

	@Test
	void testLongReturnsNullWhenKeyDoesntExit() {
		assertNull(new JobParameters().getLong("keythatdoesntexist"));
	}

	@Test
	void testStringReturnsNullWhenKeyDoesntExit() {
		assertNull(new JobParameters().getString("keythatdoesntexist"));
	}

	@Test
	void testDoubleReturnsNullWhenKeyDoesntExit() {
		assertNull(new JobParameters().getDouble("keythatdoesntexist"));
	}

	@Test
	void testDateReturnsNullWhenKeyDoesntExit() {
		assertNull(new JobParameters().getDate("keythatdoesntexist"));
	}

}
