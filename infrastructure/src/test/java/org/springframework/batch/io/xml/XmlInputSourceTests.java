/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.io.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.xml.ObjectInput;
import org.springframework.batch.io.xml.ObjectInputFactory;
import org.springframework.batch.io.xml.XmlInputSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.ClassUtils;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Additional unit tests, which test feuatores not tested with
 * XmlInputTemplateIntegrationTest
 * @author peter.zozom
 */
public class XmlInputSourceTests extends TestCase {

	private MockControl oifControl;

	private MockControl oiControl;

	private ObjectInputFactory objectInputFactory;

	private ObjectInput objectInput;

	private XmlInputSource input;

	/**
	 * Set up XmlInputTemplate: create mock for FileLocator,
	 * ObjectInputFactory and ObjectInput
	 */
	public void setUp() {

		Resource resource = new ClassPathResource(ClassUtils.addResourcePathToPackagePath(getClass(), "20070125.testStream.xmlFileStep.xml"));

		// create mock for ObjectInput
		oiControl = MockControl.createControl(ObjectInput.class);
		objectInput = (ObjectInput) oiControl.getMock();

		// create mock for ObjectInputFactory
		oifControl = MockControl.createControl(ObjectInputFactory.class);
		objectInputFactory = (ObjectInputFactory) oifControl.getMock();
		objectInputFactory.createObjectInput(resource, "UTF-8");
		oifControl.setReturnValue(objectInput, 1);
		oifControl.replay();

		// create input template
		input = new XmlInputSource() {
			protected void registerSynchronization() {
			}

			protected SAXParserFactory getSaxFactory() throws FactoryConfigurationError {
				return new MockSAXFactory();
			}
		};

		// set up input template
		input.setValidating(false);
		input.setName("test_name");
		input.setInputFactory(objectInputFactory);
		
		input.setResource(resource);
	}

	/**
	 * Test init called twice (2nd call should do nothing)
	 */
	public void testDoubleInit() {

		// set up objectInput mock
		objectInput.position();
		oiControl.setReturnValue(3, 1);
		oiControl.replay();
		
		// call init
		input.open();

		// call init again - nothing should happen
		input.open();

		// verify method calls for each mock object
		oifControl.verify();
		oiControl.verify();
	}

	/**
	 * Test exception handling in validateInputFile() method
	 */
	public void testExceptionsInValidationMethod() {

		oifControl.reset();
		oifControl.replay();
		// set up objectInput mock
		oiControl.replay();
		input.setValidating(true);

		try {
			// call init again - nothing should happen
			input.open();
			fail("ParserConfigurationException was expected");
		}
		catch (BatchEnvironmentException bee) {
			// ParserConfigurationException is expected
			assertTrue(bee.getCause() instanceof ParserConfigurationException);
		}

		FileSystemResource resource = new FileSystemResource("FooDummy.xml");
		assertTrue(!resource.exists());
		input.setResource(resource);

		try {
			// call init again - nothing should happen
			input.open();
			fail("BatchCriticalException was expected");
		}
		catch (BatchCriticalException bee) {
			// IOException is expected
			assertTrue(bee.getCause() instanceof IOException);
		}

		// verify method calls for each mock object
		oifControl.verify();
		oiControl.verify();

	}

	/**
	 * Test exception handling in read() method
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void testExceptionsInReadMethod() throws ClassNotFoundException, IOException {

		// set up objectInput mock
		objectInput.position();
		oiControl.setReturnValue(3, 1);
		objectInput.readObject();
		oiControl.setThrowable(new IOException());
		objectInput.readObject();
		oiControl.setThrowable(new ClassNotFoundException());
		oiControl.replay();

		// call init
		input.open();

		try {
			input.read();
			fail("BatchCriticalException caused by IOException was expected");
		}
		catch (BatchCriticalException bce) {
			assertTrue(bce.getCause() instanceof IOException);
		}

		try {
			input.read();
			fail("BatchCriticalException caused by ClassNotFoundException was expected");
		}
		catch (BatchCriticalException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}

		// verify method calls for each mock object
		oifControl.verify();
		oiControl.verify();
	}

	/**
	 * Test afterCompletition() method with transaction status "UNKNOWN"
	 */
	public void testTransactionUnknownStatus() {

		// set up ObjectInput mock
		objectInput.position();
		oiControl.setReturnValue(3, 1);
		oiControl.replay();

		input.open();

		// call afterCompletition method with unknown status - nothing should
		// happen
		input.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

		// verify method calls for each mock object
		oifControl.verify();
		oiControl.verify();
	}

	/*
	 * Mock for SAXParserFactory, which either throws
	 * ParserConfigurationException or returns MockSAXParser
	 */
	private static class MockSAXFactory extends SAXParserFactory {

		private static int counter = 0;

		public boolean getFeature(String name) throws ParserConfigurationException, SAXNotRecognizedException,
				SAXNotSupportedException {
			return false;
		}

		public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
			counter++;
			if (counter % 2 != 0) {
				throw new ParserConfigurationException();
			}
			return new MockSAXParser();
		}

		public void setFeature(String name, boolean value) throws ParserConfigurationException,
				SAXNotRecognizedException, SAXNotSupportedException {
		}
	}

	/*
	 * Mock for SAXParser, which throws IOException in parse(java.io.File,
	 * org.xml.sax.helpers.DefaultHandler) method
	 */
	private static class MockSAXParser extends SAXParser {

		public void parse(File f, DefaultHandler dh) throws SAXException, IOException {
			throw new IOException();
		}

		public Parser getParser() throws SAXException {
			return null;
		}

		public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
			return null;
		}

		public XMLReader getXMLReader() throws SAXException {
			return null;
		}

		public boolean isNamespaceAware() {
			return false;
		}

		public boolean isValidating() {
			return false;
		}

		public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		}

	}
}
