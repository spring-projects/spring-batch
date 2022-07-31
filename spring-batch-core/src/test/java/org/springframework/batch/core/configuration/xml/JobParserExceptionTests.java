/*
 * Copyright 2009-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;

class JobParserExceptionTests {

	@Test
	void testUnreachableStep() {
		Exception exception = assertThrows(BeanDefinitionParsingException.class,
				() -> new ClassPathXmlApplicationContext(
						"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepTests-context.xml"));
		assertTrue(exception.getMessage().contains("The element [s2] is unreachable"));
	}

	@Test
	void testUnreachableStepInFlow() {
		Exception exception = assertThrows(BeanDefinitionParsingException.class,
				() -> new ClassPathXmlApplicationContext(
						"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepInFlowTests-context.xml"));
		assertTrue(exception.getMessage().contains("The element [s4] is unreachable"));
	}

	@Test
	void testNextOutOfScope() {
		NestedRuntimeException exception = assertThrows(BeanCreationException.class,
				() -> new ClassPathXmlApplicationContext(
						"org/springframework/batch/core/configuration/xml/JobParserNextOutOfScopeTests-context.xml"));
		String message = exception.getRootCause().getMessage();
		assertTrue(
				message.matches(".*Missing state for \\[StateTransition: \\[state=.*s2, pattern=\\*, next=.*s3\\]\\]"),
				"Wrong message: " + message);
	}

	@Test
	void testWrongSchemaInRoot() {
		Exception exception = assertThrows(BeanDefinitionParsingException.class,
				() -> new ClassPathXmlApplicationContext(
						"org/springframework/batch/core/configuration/xml/JobParserWrongSchemaInRootTests-context.xml"));
		String message = exception.getMessage();
		assertTrue(message.startsWith("Configuration problem: You are using a version of the spring-batch XSD"),
				"Wrong message: " + message);
	}

}
