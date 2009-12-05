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
			assertTrue("Wrong message: "+message, message.matches("(?s).*You cannot use spring-batch-2.0.xsd.*"));
		} catch (BeanDefinitionStoreException e) {
			// Probably the internet is not available and the schema validation failed.
			fail("Wrong exception when schema didn't match: " + e.getMessage());
		}
	}

}
