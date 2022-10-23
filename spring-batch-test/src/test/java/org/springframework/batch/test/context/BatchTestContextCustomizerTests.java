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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
class BatchTestContextCustomizerTests {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void testCustomizeContext(boolean autowireJob) {
		// given
		BatchTestContextCustomizer contextCustomizer = new BatchTestContextCustomizer(autowireJob);
		GenericApplicationContext context = new GenericApplicationContext();
		MergedContextConfiguration mergedConfig = Mockito.mock(MergedContextConfiguration.class);

		// when
		contextCustomizer.customizeContext(context, mergedConfig);

		// then
		assertTrue(context.containsBean("jobLauncherTestUtils"));
		assertTrue(context.containsBean("jobRepositoryTestUtils"));

		var beanDefinition = context.getBeanDefinition("jobLauncherTestUtils");
		assertEquals(autowireJob, beanDefinition.getPropertyValues().contains("job"));
	}

	@Test
	void testCustomizeContext_whenBeanFactoryIsNotAnInstanceOfBeanDefinitionRegistry() {
		// given
		BatchTestContextCustomizer contextCustomizer = new BatchTestContextCustomizer(false);
		ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
		MergedContextConfiguration mergedConfig = Mockito.mock(MergedContextConfiguration.class);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> contextCustomizer.customizeContext(context, mergedConfig));

		// then
		assertThat(expectedException.getMessage(),
				containsString("The bean factory must be an instance of BeanDefinitionRegistry"));
	}

	@Test
	void testEquals() {
		assertEquals(new BatchTestContextCustomizer(true), new BatchTestContextCustomizer(true));
		assertEquals(new BatchTestContextCustomizer(false), new BatchTestContextCustomizer(false));
		assertNotEquals(new BatchTestContextCustomizer(true), new BatchTestContextCustomizer(false));
	}

	@Test
	void testHashCode() {
		assertNotEquals(new BatchTestContextCustomizer(true).hashCode(),
				new BatchTestContextCustomizer(false).hashCode());
	}

}
