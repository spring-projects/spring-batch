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

package org.springframework.batch.io.file.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.exception.ValidationException;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.io.file.support.transform.LineTokenizer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link SimpleFlatFileInputSourceTests}
 * 
 * @author Dave Syer
 * 
 */
public class SimpleFlatFileInputSourceTests extends TestCase {

	// object under test
	private SimpleFlatFileInputSource template = new SimpleFlatFileInputSource();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet tokenize(String line) {
			return new FieldSet(new String[] { line });
		}
	};

	/**
	 * Create inputFile, inject mock/stub dependencies for tested object,
	 * initialize the tested object
	 */
	protected void setUp() throws Exception {

		template.setResource(getInputResource(TEST_STRING));
		template.setTokenizer(tokenizer);
		template.afterPropertiesSet();

		// context argument is necessary only for the FileLocator, which
		// is mocked
		template.open();
	}

	/**
	 * Release resources.
	 */
	protected void tearDown() throws Exception {
		template.close();
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testReadFieldSet() throws IOException {
		assertEquals("[FlatFileInputTemplate-TestData]", template.readFieldSet().toString());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testRead() throws IOException {
		assertEquals("[FlatFileInputTemplate-TestData]", template.read().toString());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testReadExhausted() throws IOException {
		assertEquals("[FlatFileInputTemplate-TestData]", template.read().toString());
		assertEquals(null, template.read());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testReadWithError() throws IOException {
		template.setTokenizer(new LineTokenizer() {
			public FieldSet tokenize(String line) {
				throw new RuntimeException("foo");
			}
		});
		try {
			template.read();
			fail("Expected ValidationException");
		} catch (ValidationException e) {
			assertTrue(e.getMessage().indexOf("at line")>=0);
			assertTrue(e.getMessage().indexOf("at line 1")>=0);
		}
	}

	public void testReadBeforeOpen() throws Exception {
		template = new SimpleFlatFileInputSource();
		template.setResource(getInputResource(TEST_STRING));
		assertEquals("[FlatFileInputTemplate-TestData]", template.readFieldSet().toString());
	}

	public void testCloseBeforeOpen() throws Exception {
		template = new SimpleFlatFileInputSource();
		template.setResource(getInputResource(TEST_STRING));
		template.close();
		// The open still happens automatically on a read...
		assertEquals("[FlatFileInputTemplate-TestData]", template.readFieldSet().toString());
	}

	public void testCloseOnDestroy() throws Exception {
		final List list = new ArrayList();
		template = new SimpleFlatFileInputSource() {
			public void close() {
				list.add("close");
			}
		};
		template.destroy();
		assertEquals(1, list.size());
	}

	public void testInitializationWithNullResource() throws Exception {
		template = new SimpleFlatFileInputSource();
		try {
			template.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testOpenTwiceHasNoEffect() throws Exception {
		template.open();
		testRead();
	}

	public void testSetValidEncoding() throws Exception {
		template = new SimpleFlatFileInputSource();
		template.setEncoding("UTF-8");
		template.setResource(getInputResource(TEST_STRING));
		testRead();
	}

	public void testSetNullEncoding() throws Exception {
		template = new SimpleFlatFileInputSource();
		template.setEncoding(null);
		template.setResource(getInputResource(TEST_STRING));
		try {
			template.open();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testSetInvalidEncoding() throws Exception {
		template = new SimpleFlatFileInputSource();
		template.setEncoding("foo");
		template.setResource(getInputResource(TEST_STRING));
		try {
			template.open();
			fail("Expected BatchEnvironmentException");
		}
		catch (BatchEnvironmentException e) {
			// expected
			assertEquals("foo", e.getCause().getMessage());
		}
	}

	public void testEncoding() throws Exception {
		template.setEncoding("UTF-8");
		testRead();
	}

	public void testRecordSeparator() throws Exception {
		template.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		testRead();
	}

	public void testInvalidFile() throws IOException {
		DefaultFlatFileInputSource ffit = new DefaultFlatFileInputSource();

		FileSystemResource resource = new FileSystemResource("FooDummy.txt");
		assertTrue(!resource.exists());
		ffit.setResource(resource);

		try {
			ffit.open();
			fail("File is not existing but exception was not thrown.");
		}
		catch (BatchEnvironmentException e) {
			assertEquals("FooDummy", e.getCause().getMessage().substring(0,8));
		}

	}

}
