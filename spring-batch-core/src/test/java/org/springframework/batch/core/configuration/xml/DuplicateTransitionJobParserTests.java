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
package org.springframework.batch.core.configuration.xml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0
 */
class DuplicateTransitionJobParserTests {

	@Test
	void testNextAttributeWithNestedElement() {
		assertThrows(BeanDefinitionStoreException.class, () -> new ClassPathXmlApplicationContext(ClassUtils
				.addResourcePathToPackagePath(getClass(), "NextAttributeMultipleFinalJobParserTests-context.xml")));
	}

	@Test
	void testDuplicateTransition() {
		assertThrows(BeanDefinitionStoreException.class, () -> new ClassPathXmlApplicationContext(
				ClassUtils.addResourcePathToPackagePath(getClass(), "DuplicateTransitionJobParserTests-context.xml")));
	}

}
