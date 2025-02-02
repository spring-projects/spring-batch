/*
 * Copyright 2018-2025 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
class BatchTestContextCustomizerFactoryTests {

	private final BatchTestContextCustomizerFactory factory = new BatchTestContextCustomizerFactory();

	@ParameterizedTest
	@ValueSource(classes = { MyJobTest.class, MyJobTest.MyNestedTest.class })
	void testCreateContextCustomizer_whenAnnotationIsPresent(Class<?> testClass) {
		// given
		List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		assertInstanceOf(BatchTestContextCustomizer.class, contextCustomizer);
	}

	@Test
	void testCreateContextCustomizer_whenAnnotationIsAbsent() {
		// given
		Class<?> testClass = MyOtherJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		assertNull(contextCustomizer);
	}

	@SpringBatchTest
	private static class MyJobTest {

		@Nested
		class MyNestedTest {

		}

	}

	private static class MyOtherJobTest {

	}

}
