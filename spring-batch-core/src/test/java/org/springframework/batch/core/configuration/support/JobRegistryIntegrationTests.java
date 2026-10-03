/*
 * Copyright 2006-2026 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class JobRegistryIntegrationTests {

	@Test
	void testRegistry() throws DuplicateJobException {
		SimpleJob job = new SimpleJob("testJob");
		JobRegistry jobRegistry = new MapJobRegistry();

		jobRegistry.register(job);

		assertEquals(1, jobRegistry.getJobNames().size());
		assertEquals(job.getName(), jobRegistry.getJobNames().iterator().next());
	}

	@Test
	void testDuplicateJobRegistration() {
		assertThrows(IllegalStateException.class,
				() -> new AnnotationConfigApplicationContext(JobConfigurationWithDuplicateJobs.class));
	}

	@Configuration
	@EnableBatchProcessing
	static class JobConfigurationWithDuplicateJobs {

		@Bean
		Job job1() {
			return new JobSupport("sameJobNameOnPurpose");
		}

		@Bean
		Job job2() {
			return new JobSupport("sameJobNameOnPurpose");
		}

		@Bean
		public JobRegistry jobRegistry() {
			return new MapJobRegistry();
		}

	}

}
