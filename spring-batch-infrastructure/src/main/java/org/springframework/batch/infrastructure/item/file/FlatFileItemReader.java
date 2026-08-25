/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ReaderNotOpenException;
import org.springframework.batch.infrastructure.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.infrastructure.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Restartable {@link ItemReader} that reads lines from input
 * {@link #setResource(Resource)}. Line is defined by the
 * {@link #setRecordSeparatorPolicy(RecordSeparatorPolicy)} and mapped to item using
 * {@link #setLineMapper(LineMapper)}. If an exception is thrown during line mapping it is
 * rethrown as {@link FlatFileParseException} adding information about the problematic
 * line and its line number.
 *
 * <p>
 * This reader is <b>not</b> thread-safe.
 * </p>
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @author Jimmy Praet
 */
// FIXME the design of creating a flat file reader with an optional resource (to support
// the multi-resource case) is broken.
// FIXME The multi-resource reader should create the delegate with the current resource
public class FlatFileItemReader<T> extends AbstractItemCountingItemStreamItemReader<T>
		implements ResourceAwareItemReaderItemStream<T> {

	private static final Log logger = LogFactory.getLog(FlatFileItemReader.class);

	public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

	public static final String[] DEFAULT_COMMENT_PREFIXES = new String[] { "#" };

	/**
	 * Default upper bound on the number of physical lines that may be folded into a
	 * single logical record by the configured {@link RecordSeparatorPolicy}. Bounds
	 * worst-case work for the multi-line-record accumulator so that a malformed input
	 * (e.g. one unbalanced quote for {@code DefaultRecordSeparatorPolicy}) cannot pin a
	 * CPU core for an extended period.
	 *
	 * @since 6.0.5
	 */
	public static final int DEFAULT_MAX_LINES_PER_RECORD = 1_000;

	/**
	 * Default upper bound on the character length of a single accumulated logical record.
	 * Bounds heap allocation and concatenation cost for the multi-line-record
	 * accumulator. 1 MiB worth of characters.
	 *
	 * @since 6.0.5
	 */
	public static final int DEFAULT_MAX_BYTES_PER_RECORD = 1_048_576;

	private RecordSeparatorPolicy recordSeparatorPolicy = new SimpleRecordSeparatorPolicy();

	private @Nullable Resource resource;

	private @Nullable BufferedReader reader;

	private int lineCount = 0;

	protected String[] comments = DEFAULT_COMMENT_PREFIXES;

	private boolean noInput = false;

	private String encoding = DEFAULT_CHARSET;

	private LineMapper<T> lineMapper;

	private int linesToSkip = 0;

	private @Nullable LineCallbackHandler skippedLinesCallback;

	private boolean strict = true;

	private BufferedReaderFactory bufferedReaderFactory = new DefaultBufferedReaderFactory();

	private int maxLinesPerRecord = DEFAULT_MAX_LINES_PER_RECORD;

	private int maxBytesPerRecord = DEFAULT_MAX_BYTES_PER_RECORD;

	/**
	 * Create a new {@link FlatFileItemReader} with a {@link LineMapper}.
	 * @param lineMapper to use to map lines to items
	 * @since 6.0
	 */
	public FlatFileItemReader(LineMapper<T> lineMapper) {
		Assert.notNull(lineMapper, "A LineMapper is required");
		this.lineMapper = lineMapper;
	}

	/**
	 * Create a new {@link FlatFileItemReader} with a {@link Resource} and a
	 * {@link LineMapper}.
	 * @param resource the input resource
	 * @param lineMapper to use to map lines to items
	 * @since 6.0
	 */
	public FlatFileItemReader(Resource resource, LineMapper<T> lineMapper) {
		this(lineMapper);
		Assert.notNull(resource, "The resource must not be null");
		this.resource = resource;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(ExecutionContext)} if the input resource does not exist.
	 * @param strict <code>true</code> by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	/**
	 * @param skippedLinesCallback will be called for each one of the initial skipped
	 * lines before any items are read.
	 */
	public void setSkippedLinesCallback(LineCallbackHandler skippedLinesCallback) {
		this.skippedLinesCallback = skippedLinesCallback;
	}

	/**
	 * Public setter for the number of lines to skip at the start of a file. Can be used
	 * if the file contains a header without useful (column name) information, and without
	 * a comment delimiter at the beginning of the lines.
	 * @param linesToSkip the number of lines to skip
	 */
	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
	}

	/**
	 * Setter for line mapper. This property is required to be set.
	 * @param lineMapper maps line to item
	 */
	public void setLineMapper(LineMapper<T> lineMapper) {
		this.lineMapper = lineMapper;
	}

	/**
	 * Setter for the encoding for this input source. Default value is
	 * {@link #DEFAULT_CHARSET}.
	 * @param encoding a properties object which possibly contains the encoding for this
	 * input file;
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Factory for the {@link BufferedReader} that will be used to extract lines from the
	 * file. The default is fine for plain text files, but this is a useful strategy for
	 * binary files where the standard BufferedReader from java.io is limiting.
	 * @param bufferedReaderFactory the bufferedReaderFactory to set
	 */
	public void setBufferedReaderFactory(BufferedReaderFactory bufferedReaderFactory) {
		this.bufferedReaderFactory = bufferedReaderFactory;
	}

	/**
	 * Set the maximum number of physical lines that may be folded into a single logical
	 * record by the configured {@link RecordSeparatorPolicy}. Defaults to
	 * {@link #DEFAULT_MAX_LINES_PER_RECORD}. The accumulator throws a
	 * {@link FlatFileParseException} as soon as this limit is exceeded, which bounds the
	 * worst-case CPU and memory cost of a malformed input (e.g. an unbalanced quote or
	 * brace that would otherwise cause {@code DefaultRecordSeparatorPolicy} /
	 * {@code JsonRecordSeparatorPolicy} to fold the entire rest of the file into a single
	 * record). Set to {@link Integer#MAX_VALUE} to effectively disable the limit (not
	 * recommended on untrusted input).
	 * @param maxLinesPerRecord the maximum number of lines accepted per logical record.
	 * Must be positive.
	 * @since 6.0.5
	 */
	public void setMaxLinesPerRecord(int maxLinesPerRecord) {
		Assert.isTrue(maxLinesPerRecord > 0, "maxLinesPerRecord must be positive.");
		this.maxLinesPerRecord = maxLinesPerRecord;
	}

	/**
	 * Set the maximum character length of a single accumulated logical record. Defaults
	 * to {@link #DEFAULT_MAX_BYTES_PER_RECORD} (1&nbsp;MiB). The accumulator throws a
	 * {@link FlatFileParseException} as soon as a folded record would exceed this limit,
	 * which bounds heap allocation and concatenation cost. Set to
	 * {@link Integer#MAX_VALUE} to effectively disable the limit (not recommended on
	 * untrusted input).
	 * @param maxBytesPerRecord the maximum character length accepted per logical record.
	 * Must be positive.
	 * @since 6.0.5
	 */
	public void setMaxBytesPerRecord(int maxBytesPerRecord) {
		Assert.isTrue(maxBytesPerRecord > 0, "maxBytesPerRecord must be positive.");
		this.maxBytesPerRecord = maxBytesPerRecord;
	}

	/**
	 * Setter for comment prefixes. Can be used to ignore header lines as well by using
	 * e.g. the first couple of column names as a prefix. Defaults to
	 * {@link #DEFAULT_COMMENT_PREFIXES}.
	 * @param comments an array of comment line prefixes.
	 */
	public void setComments(String[] comments) {
		this.comments = new String[comments.length];
		System.arraycopy(comments, 0, this.comments, 0, comments.length);
	}

	/**
	 * Public setter for the input resource.
	 */
	@Override
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * Public setter for the recordSeparatorPolicy. Used to determine where the line
	 * endings are and do things like continue over a line ending if inside a quoted
	 * string. Defaults to {@link SimpleRecordSeparatorPolicy}.
	 * @param recordSeparatorPolicy the recordSeparatorPolicy to set
	 */
	public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	/**
	 * @return string corresponding to logical record according to
	 * {@link #setRecordSeparatorPolicy(RecordSeparatorPolicy)} (might span multiple lines
	 * in file).
	 */
	@Override
	protected @Nullable T doRead() throws Exception {
		Assert.notNull(resource, "Input resource must be set");

		if (noInput) {
			return null;
		}

		String line = readLine();

		if (line == null) {
			return null;
		}
		else {
			try {
				return lineMapper.mapLine(line, lineCount);
			}
			catch (Exception ex) {
				throw new FlatFileParseException("Parsing error at line: " + lineCount + " in resource=["
						+ resource.getDescription() + "], input=[" + line + "]", ex, line, lineCount);
			}
		}
	}

	/**
	 * @return next line (skip comments).getCurrentResource
	 */
	private @Nullable String readLine() {

		if (reader == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}

		String line = null;

		try {
			do {
				line = reader.readLine();
				if (line == null) {
					return null;
				}
				lineCount++;
			}
			while (isComment(line));

			line = applyRecordSeparatorPolicy(line);
		}
		catch (IOException e) {
			// Prevent IOException from recurring indefinitely
			// if client keeps catching and re-calling
			noInput = true;
			if (line == null) {
				throw new NonTransientFlatFileException("Unable to read from resource: [" + resource + "]", e);
			}
			else {
				throw new NonTransientFlatFileException("Unable to read from resource: [" + resource + "]", e, line,
						lineCount);
			}
		}
		return line;
	}

	protected boolean isComment(String line) {
		for (String prefix : comments) {
			if (line.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doClose() throws Exception {
		lineCount = 0;
		if (reader != null) {
			reader.close();
		}
	}

	@Override
	protected void doOpen() throws Exception {
		Assert.notNull(resource, "Input resource must be set");

		noInput = true;
		if (!resource.exists()) {
			if (strict) {
				throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode): " + resource);
			}
			logger.warn("Input resource does not exist " + resource.getDescription());
			return;
		}

		if (!resource.isReadable()) {
			if (strict) {
				throw new IllegalStateException(
						"Input resource must be readable (reader is in 'strict' mode): " + resource);
			}
			logger.warn("Input resource is not readable " + resource.getDescription());
			return;
		}

		reader = bufferedReaderFactory.create(resource, encoding);
		for (int i = 0; i < linesToSkip; i++) {
			String line = readLine();
			if (skippedLinesCallback != null && line != null) {
				skippedLinesCallback.handleLine(line);
			}
		}
		noInput = false;
	}

	@Override
	protected void jumpToItem(int itemIndex) throws Exception {
		for (int i = 0; i < itemIndex; i++) {
			readLine();
		}
	}

	private String applyRecordSeparatorPolicy(String line) throws IOException {
		if (reader == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}
		// Accumulate into a StringBuilder to avoid the per-iteration `record + line`
		// allocation. The policy callbacks still operate on a String snapshot
		// (their API contract), so we maintain a current snapshot alongside the
		// builder; the snapshot is refreshed only when a new line is folded in.
		StringBuilder record = new StringBuilder(line);
		String snapshot = line;
		int linesInRecord = 1;
		while (!recordSeparatorPolicy.isEndOfRecord(snapshot)) {
			line = this.reader.readLine();
			if (line == null) {
				if (StringUtils.hasText(snapshot)) {
					// A record was partially complete since it hasn't ended but
					// the line is null
					throw new FlatFileParseException("Unexpected end of file before record complete", snapshot,
							lineCount);
				}
				else {
					// Record has no text but it might still be post processed
					// to something (skipping preProcess since that was already
					// done)
					break;
				}
			}
			lineCount++;
			linesInRecord++;
			if (linesInRecord > maxLinesPerRecord) {
				throw new FlatFileParseException(
						"Record at line " + lineCount + " exceeded the configured limit of " + maxLinesPerRecord
								+ " lines per record. "
								+ "If your records legitimately span more lines, raise the limit via "
								+ "FlatFileItemReader#setMaxLinesPerRecord(int); otherwise check the input for an "
								+ "unterminated record separator (e.g. unbalanced quote or brace).",
						snapshot, lineCount);
			}
			String preProcessed = recordSeparatorPolicy.preProcess(snapshot);
			long projectedLength = (long) preProcessed.length() + line.length();
			if (projectedLength > maxBytesPerRecord) {
				throw new FlatFileParseException(
						"Record at line " + lineCount + " exceeded the configured limit of " + maxBytesPerRecord
								+ " bytes per record. "
								+ "If your records legitimately exceed this size, raise the limit via "
								+ "FlatFileItemReader#setMaxBytesPerRecord(int); otherwise check the input for an "
								+ "unterminated record separator (e.g. unbalanced quote or brace).",
						snapshot, lineCount);
			}
			record.setLength(0);
			record.append(preProcessed).append(line);
			snapshot = record.toString();
		}

		return recordSeparatorPolicy.postProcess(snapshot);

	}

}
