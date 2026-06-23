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
package org.springframework.batch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobInstance;
import org.springframework.util.SerializationUtils;

/**
 * @author dsyer
 *
 **/
class JobInstanceTests {

	private JobInstance instance = new JobInstance(11L, "job");

	@Test
	void testGetName() {
		instance = new JobInstance(1L, "foo");
		assertEquals("foo", instance.getJobName());
	}

	@Test
	void testGetJob() {
		assertEquals("job", instance.getJobName());
	}

	@Test
	void testSerialization() {
		instance = new JobInstance(1L, "jobName");
		assertEquals(instance, SerializationUtils.clone(instance));
	}

	@Test
	void testGetInstanceId() {
		assertEquals(11, instance.getId());
	}

}
