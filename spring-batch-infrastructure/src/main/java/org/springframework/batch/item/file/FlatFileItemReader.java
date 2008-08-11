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

package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.AbstractLineTokenizer;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.support.AbstractBufferedItemReaderItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This class represents a {@link ItemReader}, that reads lines from text file,
 * tokenizes them to structured tuples ({@link FieldSet}s) instances and maps
 * the {@link FieldSet}s to domain objects. The location of the file is defined
 * by the resource property. To separate the structure of the file,
 * {@link LineTokenizer} is used to parse data obtained from the file. <br/>
 * 
 * A {@link FlatFileItemReader} is not thread safe because it maintains state in
 * instance variables. <br/>
 * 
 * <p>
 * This class supports restart, skipping invalid lines and storing statistics.
 * It can be configured to setup {@link FieldSet} column names from the file
 * header, skip given number of lines at the beginning of the file.
 * </p>
 * 
 * The implementation is *not* thread-safe.
 * 
 * @author Waseem Malik
 * @author Tomas Slanina
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class FlatFileItemReader<T> extends AbstractBufferedItemReaderItemStream<T> implements
		ResourceAwareItemReaderItemStream<T>, InitializingBean {

	private static Log log = LogFactory.getLog(FlatFileItemReader.class);

	// default encoding for input files
	public static final String DEFAULT_CHARSET = "ISO-8859-1";

	private String encoding = DEFAULT_CHARSET;

	private Resource resource;

	private RecordSeparatorPolicy recordSeparatorPolicy = new DefaultRecordSeparatorPolicy();

	private String[] comments = new String[] { "#" };

	private int linesToSkip = 0;

	private boolean firstLineIsHeader = false;

	private LineTokenizer tokenizer = new DelimitedLineTokenizer();

	private FieldSetMapper<? extends T> fieldSetMapper;

	private int lineCount = 0;

	private BufferedReader reader;

	public FlatFileItemReader() {
		setName(ClassUtils.getShortName(FlatFileItemReader.class));
	}

	/**
	 * Setter for resource property. The location of an input stream that can be
	 * read.
	 * 
	 * @param resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Public setter for the recordSeparatorPolicy. Used to determine where the
	 * line endings are and do things like continue over a line ending if inside
	 * a quoted string.
	 * 
	 * @param recordSeparatorPolicy the recordSeparatorPolicy to set
	 */
	public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	/**
	 * Setter for comment prefixes. Can be used to ignore header lines as well
	 * by using e.g. the first couple of column names as a prefix.
	 * 
	 * @param comments an array of comment line prefixes.
	 */
	public void setComments(String[] comments) {
		this.comments = new String[comments.length];
		System.arraycopy(comments, 0, this.comments, 0, comments.length);
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
	 * @param lineTokenizer tokenizes each line from file into {@link FieldSet}.
	 */
	public void setLineTokenizer(LineTokenizer lineTokenizer) {
		this.tokenizer = lineTokenizer;
	}

	/**
	 * Set the FieldSetMapper to be used for each line.
	 * 
	 * @param fieldSetMapper
	 */
	public void setFieldSetMapper(FieldSetMapper<? extends T> fieldSetMapper) {
		this.fieldSetMapper = fieldSetMapper;
	}

	/**
	 * Public setter for the number of lines to skip at the start of a file. Can
	 * be used if the file contains a header without useful (column name)
	 * information, and without a comment delimiter at the beginning of the
	 * lines.
	 * 
	 * @param linesToSkip the number of lines to skip
	 */
	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
	}

	/**
	 * Setter for the encoding for this input source. Default value is
	 * {@link #DEFAULT_CHARSET}.
	 * 
	 * @param encoding a properties object which possibly contains the encoding
	 * for this input file;
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(fieldSetMapper, "FieldSetMapper must not be null.");
	}

	/**
	 * Close the {@link BufferedReader} and reset internal state.
	 */
	protected void doClose() throws Exception {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		}
		catch (IOException e) {
			throw new ItemStreamException("Could not close reader", e);
		}
		finally {
			lineCount = 0;
		}
	}

	/**
	 * Initialize the {@link BufferedReader} and skip
	 * {@link #setLinesToSkip(int)} number of lines. Setup column names if
	 * {@link #setFirstLineIsHeader(boolean)} is <code>true</code>.
	 */
	protected void doOpen() throws Exception {
		Assert.notNull(resource, "Input resource must not be null");
		Assert.state(resource.exists(), "Resource must exist: [" + resource + "]");

		try {
			reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), encoding));
		}
		catch (IOException e) {
			throw new ItemStreamException("Could not open resource", e);
		}

		log.debug("Opening flat file for reading: " + resource);

		for (int i = 0; i < linesToSkip; i++) {
			readRecord();
		}

		if (firstLineIsHeader) {
			// skip the header
			String firstLine = readRecord();
			// set names in tokenizer if they haven't been set already
			if (tokenizer instanceof AbstractLineTokenizer && !((AbstractLineTokenizer) tokenizer).hasNames()) {
				String[] names = tokenizer.tokenize(firstLine).getValues();
				((AbstractLineTokenizer) tokenizer).setNames(names);
			}
		}

	}

	/**
	 * Reads a line from input, tokenizes is it using the
	 * {@link #setLineTokenizer(LineTokenizer)} and maps to domain object using
	 * {@link #setFieldSetMapper(FieldSetMapper)}.
	 */
	protected T doRead() throws Exception {
		String record = readRecord();

		if (record != null) {
			try {
				FieldSet tokenizedLine = tokenizer.tokenize(record);
				return fieldSetMapper.mapLine(tokenizedLine);
			}
			catch (RuntimeException ex) {
				// add current line count to message and re-throw
				throw new FlatFileParseException("Parsing error at line: " + lineCount + " in resource="
						+ resource.getDescription() + ", input=[" + record + "]", ex, record, lineCount);
			}
		}
		return null;
	}

	/**
	 * @return next line (skip comments).
	 */
	private String readLine() {

		if (reader == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}

		String line = null;

		try {
			line = this.reader.readLine();
			if (line == null) {
				return null;
			}
			lineCount++;
			while (isComment(line)) {
				line = reader.readLine();
				if (line == null) {
					return null;
				}
				lineCount++;
			}
		}
		catch (IOException e) {
			throw new UnexpectedInputException("Unable to read from resource '" + resource + "' at line " + lineCount,
					e);
		}
		return line;
	}

	/**
	 * @return string corresponding to logical record according to
	 * {@link #setRecordSeparatorPolicy(RecordSeparatorPolicy)} (might span
	 * multiple lines in file).
	 */
	private String readRecord() {
		String line = readLine();
		String record = line;
		if (line != null) {
			while (line != null && !recordSeparatorPolicy.isEndOfRecord(record)) {
				record = recordSeparatorPolicy.preProcess(record) + (line = readLine());
			}
		}
		return recordSeparatorPolicy.postProcess(record);
	}

	private boolean isComment(String line) {
		for (String prefix : comments) {
			if (line.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

}
