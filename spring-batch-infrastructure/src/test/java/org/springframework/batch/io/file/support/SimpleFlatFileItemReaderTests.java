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
import org.springframework.batch.io.exception.FlatFileParsingException;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.io.file.support.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.io.file.support.transform.DelimitedLineTokenizer;
import org.springframework.batch.io.file.support.transform.LineTokenizer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link SimpleFlatFileItemReaderTests}
 *
 * @author Dave Syer
 *
 */
public class SimpleFlatFileItemReaderTests extends TestCase {

	// object under test
	private SimpleFlatFileItemReader inputSource = new SimpleFlatFileItemReader();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet tokenize(String line) {
			return new FieldSet(new String[] { line });
		}
	};

	private FieldSetMapper fieldSetMapper = new FieldSetMapper(){
		public Object mapLine(FieldSet fs) {
			return fs;
		}
	};

	/**
	 * Create inputFile, inject mock/stub dependencies for tested object,
	 * initialize the tested object
	 */
	protected void setUp() throws Exception {

		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setTokenizer(tokenizer);
		inputSource.setFieldSetMapper(fieldSetMapper);
		inputSource.afterPropertiesSet();

		inputSource.open();
	}

	/**
	 * Release resources.
	 */
	protected void tearDown() throws Exception {
		inputSource.close();
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testRead() throws IOException {
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testReadExhausted() throws IOException {
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
		assertEquals(null, inputSource.read());
	}

	/**
	 * Regular usage of <code>read</code> method
	 */
	public void testReadWithTokenizerError() throws IOException {
		inputSource.setTokenizer(new LineTokenizer() {
			public FieldSet tokenize(String line) {
				throw new RuntimeException("foo");
			}
		});
		try {
			inputSource.read();
			fail("Expected ParsingException");
		} catch (FlatFileParsingException e) {
			assertEquals(e.getInput(), TEST_STRING);
			assertEquals(e.getLineNumber(), 1);
		}
	}

	public void testReadWithMapperError() throws IOException {
		inputSource.setFieldSetMapper(new FieldSetMapper(){
			public Object mapLine(FieldSet fs) {
				throw new RuntimeException("foo");
			}
		});

		try {
			inputSource.read();
			fail("Expected ParsingException");
		} catch (FlatFileParsingException e) {
			assertEquals(e.getInput(), TEST_STRING);
			assertEquals(e.getLineNumber(), 1);
		}
	}

	public void testReadBeforeOpen() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setFieldSetMapper(fieldSetMapper);
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
	}

	public void testCloseBeforeOpen() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setFieldSetMapper(fieldSetMapper);
		inputSource.close();
		// The open still happens automatically on a read...
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
	}

	public void testCloseOnDestroy() throws Exception {
		final List list = new ArrayList();
		inputSource = new SimpleFlatFileItemReader() {
			public void close() {
				list.add("close");
			}
		};
		inputSource.destroy();
		assertEquals(1, list.size());
	}

	public void testInitializationWithNullResource() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		try {
			inputSource.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testOpenTwiceHasNoEffect() throws Exception {
		inputSource.open();
		testRead();
	}

	public void testSetValidEncoding() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setEncoding("UTF-8");
		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setFieldSetMapper(fieldSetMapper);
		testRead();
	}

	public void testSetNullEncoding() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setEncoding(null);
		inputSource.setResource(getInputResource(TEST_STRING));
		try {
			inputSource.open();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testSetInvalidEncoding() throws Exception {
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setEncoding("foo");
		inputSource.setResource(getInputResource(TEST_STRING));
		try {
			inputSource.open();
			fail("Expected BatchEnvironmentException");
		}
		catch (BatchEnvironmentException e) {
			// expected
			assertEquals("foo", e.getCause().getMessage());
		}
	}

	public void testEncoding() throws Exception {
		inputSource.setEncoding("UTF-8");
		testRead();
	}

	public void testRecordSeparator() throws Exception {
		inputSource.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		testRead();
	}

	public void testComments() throws Exception {
		inputSource.setResource(getInputResource("% Comment\n"+TEST_STRING));
		inputSource.setComments(new String[] {"%"});
		testRead();
	}

	public void testInvalidFile() throws IOException {
		DefaultFlatFileItemReader ffit = new DefaultFlatFileItemReader();

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

	/**
	 * Header line is skipped and used to setup fieldSet column names.
	 */
	public void testColumnNamesInHeader() throws Exception {
		final String INPUT = "name1|name2\nvalue1|value2\nvalue3|value4";
		
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setResource(getInputResource(INPUT));
		inputSource.setTokenizer(new DelimitedLineTokenizer('|'));
		inputSource.setFieldSetMapper(fieldSetMapper);
		inputSource.setFirstLineIsHeader(true);
		inputSource.afterPropertiesSet();
		inputSource.open();
		
		FieldSet fs = (FieldSet) inputSource.read();
		assertEquals("value1", fs.readString("name1"));
		assertEquals("value2", fs.readString("name2"));
		
		fs = (FieldSet) inputSource.read();
		assertEquals("value3", fs.readString("name1"));
		assertEquals("value4", fs.readString("name2"));
	}

	/**
	 * Header line is skipped and used to setup fieldSet column names.
	 */
	public void testLinesToSkip() throws Exception {
		final String INPUT = "foo bar spam\none two\nthree four";
		
		inputSource = new SimpleFlatFileItemReader();
		inputSource.setResource(getInputResource(INPUT));
		inputSource.setTokenizer(new DelimitedLineTokenizer(' '));
		inputSource.setFieldSetMapper(fieldSetMapper);
		inputSource.setLinesToSkip(1);
		inputSource.afterPropertiesSet();
		inputSource.open();
		
		FieldSet fs = (FieldSet) inputSource.read();
		assertEquals("one", fs.readString(0));
		assertEquals("two", fs.readString(1));
		
		fs = (FieldSet) inputSource.read();
		assertEquals("three", fs.readString(0));
		assertEquals("four", fs.readString(1));
	}
}
