/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Stefano Cordio
 */
@SpringJUnitConfig
@SpringBatchTest
class SpringBatchTestIntegrationTests {

	@Autowired
	ApplicationContext context;

	@Nested
	class InnerWithoutSpringBatchTest {

		@Autowired
		ApplicationContext context;

		@Test
		void test() {
			assertSame(SpringBatchTestIntegrationTests.this.context, context);
			assertNotNull(context.getBean(JobLauncherTestUtils.class));
			assertNotNull(context.getBean(JobRepositoryTestUtils.class));
		}

	}

	@Nested
	@SpringBatchTest
	class InnerWithSpringBatchTest {

		@Autowired
		ApplicationContext context;

		@Test
		void test() {
			assertSame(SpringBatchTestIntegrationTests.this.context, context);
			assertNotNull(context.getBean(JobLauncherTestUtils.class));
			assertNotNull(context.getBean(JobRepositoryTestUtils.class));
		}

	}

}
