/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Minkuk Jo
 */
class JobXmlDescriptionTests {

	@Test
	void testJobDescriptionFromXml() {
		// given
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/core/job/builder/JobXmlDescriptionTests-context.xml");

		// when
		Job job = context.getBean("job", Job.class);

		// then
		assertThat(job).isInstanceOf(SimpleJob.class);
		assertThat(((SimpleJob) job).getDescription()).isEqualTo("A job that processes football data");
	}

}