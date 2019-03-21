/*
 * Copyright 2018 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;

/**
 * @author Mahmoud Ben Hassine
 */
public class BatchTestContextCustomizerFactoryTest {

	private BatchTestContextCustomizerFactory factory = new BatchTestContextCustomizerFactory();

	@Test
	public void testCreateContextCustomizer_whenAnnotationIsPresent() {
		// given
		Class<MyJobTest> testClass = MyJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		Assert.assertNotNull(contextCustomizer);
	}

	@Test
	public void testCreateContextCustomizer_whenAnnotationIsAbsent() {
		// given
		Class<MyOtherJobTest> testClass = MyOtherJobTest.class;
		List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

		// when
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, configAttributes);

		// then
		Assert.assertNull(contextCustomizer);
	}

	@SpringBatchTest
	private static class MyJobTest {

	}

	private static class MyOtherJobTest {

	}
}
