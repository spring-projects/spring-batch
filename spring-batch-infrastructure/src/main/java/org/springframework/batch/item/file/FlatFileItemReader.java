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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemReaderException;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.LineReader;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.ResourceLineReader;
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
 * the form of a {@link ResourceLineReader}. Be careful to configure a
 * {@link FlatFileItemReader} using an appropriate factory or scope so that it
 * is not shared between threads.<br/>
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
public class FlatFileItemReader extends AbstractBufferedItemReaderItemStream implements
		ResourceAwareItemReaderItemStream, InitializingBean {

	private static Log log = LogFactory.getLog(FlatFileItemReader.class);

	// default encoding for input files
	public static final String DEFAULT_CHARSET = "ISO-8859-1";

	private String encoding = DEFAULT_CHARSET;

	private Resource resource;

	private RecordSeparatorPolicy recordSeparatorPolicy;

	private String[] comments;

	private int linesToSkip = 0;

	private boolean firstLineIsHeader = false;

	private LineTokenizer tokenizer = new DelimitedLineTokenizer();

	private FieldSetMapper fieldSetMapper;

	/**
	 * Encapsulates the state of the input source. If it is null then we are
	 * uninitialized.
	 */
	private LineReader reader;

	public FlatFileItemReader() {
		setName(ClassUtils.getShortName(FlatFileItemReader.class));
	}

	/**
	 * @return next line to be tokenized and mapped.
	 */
	private String readLine() {
		try {
			return (String) getReader().read();
		}
		catch (ItemStreamException e) {
			throw e;
		}
		catch (ItemReaderException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalStateException();
		}
	}

	/**
	 * @return line reader used to read input file
	 */
	protected LineReader getReader() {
		if (reader == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
			// reader is now not null, or else an exception is thrown
		}
		return reader;
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
	public void setFieldSetMapper(FieldSetMapper fieldSetMapper) {
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
		Assert.notNull(resource, "Input resource must not be null");
		Assert.notNull(fieldSetMapper, "FieldSetMapper must not be null.");
	}

	protected void doClose() throws Exception {
		try {
			if (reader != null) {
				log.debug("Closing flat file for reading: " + resource);
				reader.close();
			}
		}
		finally {
			reader = null;
		}
	}

	protected void doOpen() throws Exception {
		Assert.state(resource.exists(), "Resource must exist: [" + resource + "]");

		log.debug("Opening flat file for reading: " + resource);

		if (this.reader == null) {
			ResourceLineReader reader = new ResourceLineReader(resource, encoding);
			if (recordSeparatorPolicy != null) {
				reader.setRecordSeparatorPolicy(recordSeparatorPolicy);
			}
			if (comments != null) {
				reader.setComments(comments);
			}
			reader.open();
			this.reader = reader;
		}

		for (int i = 0; i < linesToSkip; i++) {
			readLine();
		}

		if (firstLineIsHeader) {
			// skip the header
			String firstLine = readLine();
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
	 * 
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	protected Object doRead() throws Exception {
		String line = readLine();

		if (line != null) {
			int lineCount = getReader().getPosition();
			try {
				FieldSet tokenizedLine = tokenizer.tokenize(line);
				return fieldSetMapper.mapLine(tokenizedLine);
			}
			catch (RuntimeException ex) {
				// add current line count to message and re-throw
				throw new FlatFileParseException("Parsing error at line: " + lineCount + " in resource="
						+ resource.getDescription() + ", input=[" + line + "]", ex, line, lineCount);
			}
		}
		return null;
	}

}
