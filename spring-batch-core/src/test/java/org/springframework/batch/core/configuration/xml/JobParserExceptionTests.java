/*
 * Copyright 2009-2014 the original author or authors.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class JobParserExceptionTests {

	@Test
	public void testUnreachableStep() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepTests-context.xml");
			fail("Error expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("The element [s2] is unreachable"));
		}
	}

	@Test
	public void testUnreachableStepInFlow() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserUnreachableStepInFlowTests-context.xml");
			fail("Error expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("The element [s4] is unreachable"));
		}
	}

	@Test
	public void testNextOutOfScope() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserNextOutOfScopeTests-context.xml");
			fail("Error expected");
		}
		catch (BeanCreationException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: "+message, message.matches(".*Missing state for \\[StateTransition: \\[state=.*s2, pattern=\\*, next=.*s3\\]\\]"));
		}
	}

	@Test
	public void testWrongSchemaInRoot() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/batch/core/configuration/xml/JobParserWrongSchemaInRootTests-context.xml");
			fail("Error expected");
		}
		catch (BeanDefinitionParsingException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: "+message, message.startsWith("Configuration problem: You are using a version of the spring-batch XSD"));
		} catch (BeanDefinitionStoreException e) {
			// Probably the internet is not available and the schema validation failed.
			fail("Wrong exception when schema didn't match: " + e.getMessage());
		}
	}

}
