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
package org.springframework.batch.core.configuration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.JobSupport;

/**
 * @author Dave Syer
 *
 */
class GroupAwareJobTests {

	private final Job job = new JobSupport("foo");

	@Test
	void testCreateJob() {
		GroupAwareJob result = new GroupAwareJob(job);
		assertEquals("foo", result.getName());
	}

	@Test
	void testGetJobName() {
		GroupAwareJob result = new GroupAwareJob("jobs", job);
		assertEquals("jobs.foo", result.getName());
	}

	@Test
	void testToString() {
		GroupAwareJob result = new GroupAwareJob("jobs", job);
		assertEquals("JobSupport: [name=jobs.foo]", result.toString());
	}

}
