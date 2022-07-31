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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * @author Lucas Ward
 *
 */
class JobParameterTests {

	JobParameter jobParameter;

	@Test
	void testStringParameter() {
		jobParameter = new JobParameter("test", true);
		assertEquals("test", jobParameter.getValue());
	}

	@Test
	void testNullStringParameter() {
		assertThrows(IllegalArgumentException.class, () -> new JobParameter((String) null, true));
	}

	@Test
	void testLongParameter() {
		jobParameter = new JobParameter(1L, true);
		assertEquals(1L, jobParameter.getValue());
	}

	@Test
	void testDoubleParameter() {
		jobParameter = new JobParameter(1.1, true);
		assertEquals(1.1, jobParameter.getValue());
	}

	@Test
	void testDateParameter() {
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch, true);
		assertEquals(new Date(0L), jobParameter.getValue());
	}

	@Test
	void testNullDateParameter() {
		assertThrows(IllegalArgumentException.class, () -> new JobParameter((Date) null, true));
	}

	@Test
	void testDateParameterToString() {
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch, true);
		assertEquals("0", jobParameter.toString());
	}

	@Test
	void testEquals() {
		jobParameter = new JobParameter("test", true);
		JobParameter testParameter = new JobParameter("test", true);
		assertEquals(jobParameter, testParameter);
	}

	@Test
	void testHashcode() {
		jobParameter = new JobParameter("test", true);
		JobParameter testParameter = new JobParameter("test", true);
		assertEquals(testParameter.hashCode(), jobParameter.hashCode());
	}

}