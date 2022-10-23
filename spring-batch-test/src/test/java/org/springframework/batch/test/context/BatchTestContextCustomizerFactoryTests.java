/*
 * Copyright 2018-2022 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mahmoud Ben Hassine
 */
class BatchTestContextCustomizerFactoryTests {

	private final BatchTestContextCustomizerFactory factory = new BatchTestContextCustomizerFactory();

	@Test
	void testCreateContextCustomizer_whenAnnotationIsPresent() {
		// given
		Class<MyJobTest> testClass = MyJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		assertEquals(new BatchTestContextCustomizer(false), contextCustomizer);
	}

	@Test
	void testCreateContextCustomizer_whenAnnotationIsPresentAndFlagSet() {
		// given
		Class<MyUniqueJobTest> testClass = MyUniqueJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		assertEquals(new BatchTestContextCustomizer(true), contextCustomizer);
	}

	@Test
	void testCreateContextCustomizer_whenAnnotationIsAbsent() {
		// given
		Class<MyOtherJobTest> testClass = MyOtherJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		assertNull(contextCustomizer);
	}

	@SpringBatchTest
	private static class MyJobTest {

	}

	@SpringBatchTest(autowireJob = true)
	private static class MyUniqueJobTest {

	}

	private static class MyOtherJobTest {

	}

}
