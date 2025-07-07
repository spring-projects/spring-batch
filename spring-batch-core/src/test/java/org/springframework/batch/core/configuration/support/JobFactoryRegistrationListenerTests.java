/*
 * Copyright 2006-2025 the original author or authors.
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
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.test.repository.JobSupport;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class JobFactoryRegistrationListenerTests {

	private final JobFactoryRegistrationListener listener = new JobFactoryRegistrationListener();

	private final MapJobRegistry registry = new MapJobRegistry();

	@Test
	void testBind() throws Exception {
		listener.setJobRegistry(registry);
		listener.bind(new JobFactory() {
			@Override
			public Job createJob() {
				return new JobSupport("foo");
			}

			@Override
			public String getJobName() {
				return "foo";
			}
		}, null);
		assertEquals(1, registry.getJobNames().size());
	}

	@Test
	void testUnbind() throws Exception {
		testBind();
		listener.unbind(new JobFactory() {
			@Override
			public Job createJob() {
				return new JobSupport("foo");
			}

			@Override
			public String getJobName() {
				return "foo";
			}
		}, null);
		assertEquals(0, registry.getJobNames().size());
	}

}
