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

import org.springframework.batch.io.exception.FlatFileParsingException;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.io.file.support.separator.RecordSeparatorPolicy;
import org.springframework.batch.io.file.support.transform.AbstractLineTokenizer;
import org.springframework.batch.io.file.support.transform.DelimitedLineTokenizer;
import org.springframework.batch.io.file.support.transform.LineTokenizer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.item.provider.AbstractItemReader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * This class represents a basic input source, that reads data from the file and
 * returns it as structured tuples in the form of{@link FieldSet} instances.
 * The location of the file is defined by the resource property. To separate the
 * structure of the file, {@link LineTokenizer} is used to parse data obtained
 * from the file. <br/>
 * 
 * A {@link SimpleFlatFileItemReader} is not thread safe because it maintains
 * state in the form of a {@link ResourceLineReader}. Be careful to configure a
 * {@link SimpleFlatFileItemReader} using an appropriate factory or scope so
 * that it is not shared between threads.<br/>
 * 
 * @see FieldSetItemReader
 * 
 * @author Dave Syer
 */
public class SimpleFlatFileItemReader extends AbstractItemReader implements ItemReader,
		InitializingBean, DisposableBean {

	// default encoding for input files - set to ISO-8859-1
	public static final String DEFAULT_CHARSET = "ISO-8859-1";

	private Resource resource;

	/**
	 * Encapsulates the state of the input source. If it is null then we are
	 * uninitialized.
	 */
	private ResourceLineReader reader;

	private RecordSeparatorPolicy recordSeparatorPolicy;

	private String[] comments;

	private LineTokenizer tokenizer = new DelimitedLineTokenizer();

	private FieldSetMapper fieldSetMapper;

	private String encoding = DEFAULT_CHARSET;

	private boolean firstLineIsHeader = false;

	private int linesToSkip = 0;

	/**
	 * Setter for resource property. The location of an input stream that can be
	 * read.
	 * 
	 * @param resource
	 * @throws IOException
	 */
	public void setResource(Resource resource) throws IOException {
		this.resource = resource;
	}

	/**
	 * Public setter for the recordSeparatorPolicy. Used to determine where the
	 * line endings are and do things like continue over a line ending if inside
	 * a quoted string.
	 * 
	 * @param recordSeparatorPolicy
	 *            the recordSeparatorPolicy to set
	 */
	public void setRecordSeparatorPolicy(
			RecordSeparatorPolicy recordSeparatorPolicy) {
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	/**
	 * Setter for comment prefixes. Can be used to ignore header lines as well
	 * by using e.g. the first couple of column names as a prefix.
	 * 
	 * @param comments
	 *            an array of comment line prefixes.
	 */
	public void setComments(String[] comments) {
		this.comments = new String[comments.length];
		System.arraycopy(comments, 0, this.comments, 0, comments.length);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.state(resource.exists(), "Resource must exist: [" + resource
				+ "]");
		Assert.notNull(fieldSetMapper, "FieldSetMapper must not be null.");
	}

	/**
	 * Initialize the reader if necessary.
	 */
	public void open() {
		if (reader == null) {
			reader = new ResourceLineReader(resource, encoding);
			if (recordSeparatorPolicy != null) {
				reader.setRecordSeparatorPolicy(recordSeparatorPolicy);
			}
			if (comments != null) {
				reader.setComments(comments);
			}
			reader.open();
		}
		
		for (int i = 0; i < linesToSkip; i++) {
			readLine();
		}

		if (firstLineIsHeader) {
			// skip the header
			String firstLine = readLine();
			// set names in tokenizer if they haven't been set already
			if (tokenizer instanceof AbstractLineTokenizer
					&& !((AbstractLineTokenizer) tokenizer).hasNames()) {
				String[] names = tokenizer.tokenize(firstLine).getValues();
				((AbstractLineTokenizer) tokenizer).setNames(names);
			}
		}
	}

	/**
	 * Close and null out the reader.
	 * 
	 * @see ResourceLifecycle
	 */
	public void close() {
		try {
			if (reader != null) {
				reader.close();
			}
		} finally {
			reader = null;
		}
	}

	/**
	 * Calls close to ensure that bean factories can close and always release
	 * resources.
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	// Reads first valid line.
	protected String readLine() {
		return (String) getReader().read();
	}

	/**
	 * A wrapper for {@link #readFieldSet()} to make this into a real
	 * {@link ItemReader}.
	 * 
	 * @see org.springframework.batch.io.ItemReader#read()
	 */
	public Object read() {
		String line = readLine();

		if (line != null) {
			try {
				FieldSet tokenizedLine = tokenizer.tokenize(line);
				return fieldSetMapper.mapLine(tokenizedLine);
			} catch (RuntimeException ex) {
				// add current line count to message and re-throw
				throw new FlatFileParsingException("Parsing error", ex, line,
						getReader().getCurrentLineCount());
			}
		}
		return null;
	}

	/**
	 * Setter for the encoding for this input source. Default value is
	 * {@value #DEFAULT_CHARSET}.
	 * 
	 * @param encoding
	 *            a properties object which possibly contains the encoding for
	 *            this input file;
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Sets descriptor for this input template.
	 */
	public void setTokenizer(LineTokenizer lineTokenizer) {
		this.tokenizer = lineTokenizer;
	}

	/**
	 * Set the FieldSetMapper to be used for each line.
	 * 
	 * @param fieldSetMapper
	 */
	public void setFieldSetMapper(FieldSetMapper fieldSetMapper) {
		this.fieldSetMapper = fieldSetMapper;
	}

	// Returns object representing state of the input template.
	protected ResourceLineReader getReader() {
		if (reader == null) {
			open();
			// reader is now not null, or else an exception is thrown
		}
		return reader;
	}

	/**
	 * Indicates whether first line is a header. If the tokenizer is an
	 * {@link AbstractLineTokenizer} and the column names haven't been set
	 * already then the header will be used to setup column names. Default is
	 * <code>false</code>.
	 */
	public void setFirstLineIsHeader(boolean firstLineIsHeader) {
		this.firstLineIsHeader = firstLineIsHeader;
	}

	/**
	 * Public setter for the number of lines to skip at the start of a file. Can
	 * be used if the file contains a header without useful (column name)
	 * information, and without a comment delimiter at the beginning of the
	 * lines.
	 * 
	 * @param linesToSkip
	 *            the number of lines to skip
	 */
	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
	}

}
