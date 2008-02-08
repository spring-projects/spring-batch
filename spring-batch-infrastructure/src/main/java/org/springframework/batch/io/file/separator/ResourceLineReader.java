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

package org.springframework.batch.io.file.separator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.stream.ItemStreamAdapter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * An input source that reads lines one by one from a resource. <br/>
 * 
 * A line can consist of multiple lines in the input resource, according to the
 * {@link RecordSeparatorPolicy} in force. By default a line is either
 * terminated by a newline (as per {@link BufferedReader#readLine()}), or can
 * be continued onto the next line if a field surrounded by quotes (\") contains
 * a newline.<br/>
 * 
 * Comment lines can be indicated using a line prefix (or collection of
 * prefixes) and they will be ignored. The default is "#", so lines starting
 * with a pound sign will be ignored.<br/>
 * 
 * All the public methods that interact with the underlying resource (open,
 * close, read etc.) are synchronized on this.<br/>
 * 
 * Package private because this is not intended to be a public API - used
 * internally by the flat file input sources. That makes abuses of the fact that
 * it is stateful easier to control.<br/>
 * 
 * @author Dave Syer
 * @author Rob Harrop
 */
public class ResourceLineReader extends ItemStreamAdapter implements LineReader, ItemReader {

	private static final Collection DEFAULT_COMMENTS = Collections.singleton("#");

	private static final String DEFAULT_ENCODING = "ISO-8859-1";

	private static final int READ_AHEAD_LIMIT = 100000;

	private final Resource resource;

	private final String encoding;

	private Collection comments = DEFAULT_COMMENTS;

	// Encapsulates the state of the input source.
	private State state = null;

	private RecordSeparatorPolicy recordSeparatorPolicy = new DefaultRecordSeparatorPolicy();

	public ResourceLineReader(Resource resource) throws IOException {
		this(resource, DEFAULT_ENCODING);
	}

	public ResourceLineReader(Resource resource, String encoding) {
		Assert.notNull(resource, "'resource' cannot be null.");
		Assert.notNull(encoding, "'encoding' cannot be null.");
		this.resource = resource;
		this.encoding = encoding;
	}

	/**
	 * Setter for the {@link RecordSeparatorPolicy}. Default value is a
	 * {@link DefaultRecordSeparatorPolicy}. Ideally should not be changed once
	 * a reader is in use, but it would not be fatal if it was.
	 * 
	 * @param recordSeparatorPolicy the new {@link RecordSeparatorPolicy}
	 */
	public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
		/*
		 * The rest of the code accesses the policy in synchronized blocks,
		 * copying the reference before using it. So in principle it can be
		 * changed in flight - the results might not be what the user expected!
		 */
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	/**
	 * Setter for comment prefixes. Can be used to ignore header lines as well
	 * by using e.g. the first couple of column names as a prefix.
	 * 
	 * @param comments an array of comment line prefixes.
	 */
	public void setComments(String[] comments) {
		this.comments = new HashSet(Arrays.asList(comments));
	}

	/**
	 * Read the next line from the input resource, ignoring comments, and
	 * according to the {@link RecordSeparatorPolicy}.
	 * 
	 * @return a String.
	 * 
	 * @throws BatchEnvironmentException if there is an IOException while
	 * accessing the input resource.
	 * 
	 * @see org.springframework.batch.item.reader.support.ItemReader#read()
	 */
	public synchronized Object read() {
		// Make a copy of the recordSeparatorPolicy reference, in case it is
		// changed during a read operation (unlikely, but you never know)...
		RecordSeparatorPolicy recordSeparatorPolicy = this.recordSeparatorPolicy;
		String line = readLine();
		String record = line;
		if (line != null) {
			while (line != null && !recordSeparatorPolicy.isEndOfRecord(record)) {
				record = recordSeparatorPolicy.preProcess(record) + (line = readLine());
			}
		}
		return recordSeparatorPolicy.postProcess(record);
	}

	/**
	 * @return the next non-comment line
	 */
	private String readLine() {
		return getState().readLine();
	}

	/**
	 * @return
	 */
	private State getState() {
		if (state == null) {
			open();
		}
		return state;
	}

	/**
	 * A no-op because the oobject is initialized with all it needs to open in
	 * the constructor.
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public synchronized void open() {
		state = new State();
		state.open();
	}

	/**
	 * Close the reader associated with this input source.
	 * 
	 * @see org.springframework.batch.io.ItemReader#close()
	 * @throws BatchEnvironmentException if there is an {@link IOException}
	 * during the close operation.
	 */
	public synchronized void close() {
		if (state == null) {
			return;
		}
		try {
			state.close();
		}
		finally {
			state = null;
		}
	}

	/**
	 * Getter for current line count (not the current number of lines returned).
	 * 
	 * @return the current line count.
	 */
	public int getPosition() {
		return getState().getCurrentLineCount();
	}

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemStream#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		return true;
	}

	/**
	 * Mark the state for return later with reset. Uses the read-ahead limit
	 * from an underlying {@link BufferedReader}, which means that there is a
	 * limit to how much data can be recovered if the mark needs to be reset.
	 * 
	 * @see #reset()
	 * 
	 * @throws MarkFailedException if the mark could not be set.
	 */
	public synchronized void mark() throws MarkFailedException {
		getState().mark();
	}

	/**
	 * Reset the reader to the last mark.
	 * 
	 * @see #mark()
	 * 
	 * @throws ResetFailedException if the reset is unsuccessful, e.g. if
	 * the read-ahead limit was breached.
	 */
	public synchronized void reset() throws ResetFailedException {
		getState().reset();
	}

	private boolean isComment(String line) {
		for (Iterator iter = comments.iterator(); iter.hasNext();) {
			String prefix = (String) iter.next();
			if (line.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private class State {
		private BufferedReader reader;

		private int currentLineCount = 0;

		private int markedLineCount = -1;

		public String readLine() {
			String line = null;

			try {
				line = this.reader.readLine();
				if (line == null) {
					return null;
				}
				currentLineCount++;
				while (isComment(line)) {
					line = reader.readLine();
					if (line == null) {
						return null;
					}
					currentLineCount++;
				}
			}
			catch (IOException e) {
				throw new BatchEnvironmentException("Unable to read from resource '" + resource + "' at line "
						+ currentLineCount, e);
			}
			return line;
		}

		/**
		 * 
		 */
		public void open() {
			try {
				reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), encoding));
				mark();
			}
			catch (IOException e) {
				throw new BatchEnvironmentException("Could not open resource", e);
			}
		}

		/**
		 * Close the reader and reset the counters.
		 */
		public void close() {

			if (reader == null) {
				return;
			}
			try {
				reader.close();
			}
			catch (IOException e) {
				throw new BatchEnvironmentException("Could not close reader", e);
			}
			finally {
				currentLineCount = 0;
				markedLineCount = -1;
			}

		}

		/**
		 * @return the current line count
		 */
		public int getCurrentLineCount() {
			return currentLineCount;
		}

		/**
		 * Mark the underlying reader and set the line counters.
		 */
		public void mark() throws MarkFailedException {
			try {
				reader.mark(READ_AHEAD_LIMIT);
				markedLineCount = currentLineCount;
			}
			catch (IOException e) {
				throw new MarkFailedException("Could not mark reader", e);
			}
		}

		/**
		 * Reset the reader and line counters to the last marked position if
		 * possible.
		 */
		public void reset() throws ResetFailedException {

			if (markedLineCount < 0) {
				return;
			}
			try {
				this.reader.reset();
				currentLineCount = markedLineCount;
			}
			catch (IOException e) {
				throw new ResetFailedException("Could not reset reader", e);
			}

		}

	}

}
