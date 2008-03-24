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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.UnsupportedCharsetException;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetCreator;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This class is an output target that writes data to a file or stream. The writer also provides restart, statistics and
 * transaction features by implementing corresponding interfaces where possible (with a file). The location of the file
 * is defined by a {@link Resource} and must represent a writable file.<br/>
 * 
 * Uses buffered writer to improve performance.<br/>
 * 
 * Use {@link #write(String)} method to output a line to an item writer.
 * 
 * <p>
 * This class will be updated in the future to use a buffering approach to handling transactions, rather than outputting
 * directly to the file and truncating on rollback
 * </p>
 * 
 * @author Waseem Malik
 * @author Tomas Slanina
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class FlatFileItemWriter extends ExecutionContextUserSupport implements ItemWriter, ItemStream, InitializingBean {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final String WRITTEN_STATISTICS_NAME = "written";

	private static final String RESTART_COUNT_STATISTICS_NAME = "restart.count";

	private static final String RESTART_DATA_NAME = "current.count";

	private Resource resource;

	private OutputState state = null;

	private LineAggregator lineAggregator = new DelimitedLineAggregator();

	private FieldSetCreator fieldSetCreator;
	
	private boolean saveState = false;

	public FlatFileItemWriter() {
		setName(ClassUtils.getShortName(FlatFileItemWriter.class));
	}

	/**
	 * Assert that mandatory properties (resource) are set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource, "The resource must be set");
		Assert.notNull(fieldSetCreator, "A FieldSetUnmapper must be provided.");
		File file = resource.getFile();
		Assert.state(!file.exists() || file.canWrite(), "Resource is not writable: [" + resource + "]");
	}

	/**
	 * Public setter for the {@link LineAggregator}. This will be used to translate a {@link FieldSet} into a line for
	 * output.
	 * 
	 * @param lineAggregator the {@link LineAggregator} to set
	 */
	public void setLineAggregator(LineAggregator lineAggregator) {
		this.lineAggregator = lineAggregator;
	}

	/**
	 * Public setter for the {@link FieldSetCreator}. This will be used to transform the item into a {@link FieldSet}
	 * before it is aggregated by the {@link LineAggregator}.
	 * 
	 * @param fieldSetCreator the {@link FieldSetCreator} to set
	 */
	public void setFieldSetCreator(FieldSetCreator fieldSetCreator) {
		this.fieldSetCreator = fieldSetCreator;
	}

	/**
	 * Setter for resource. Represents a file that can be written.
	 * 
	 * @param resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Writes out a string followed by a "new line", where the format of the new line separator is determined by the
	 * underlying operating system. If the input is not a String and a converter is available the converter will be
	 * applied and then this method recursively called with the result. If the input is an array or collection each
	 * value will be written to a separate line (recursively calling this method for each value). If no converter is
	 * supplied the input object's toString method will be used.<br/>
	 * 
	 * @param data Object (a String or Object that can be converted) to be written to output stream
	 * @throws Exception if the transformer or file output fail
	 */
	public void write(Object data) throws Exception {
		FieldSet fieldSet = fieldSetCreator.mapItem(data);
		getOutputState().write(lineAggregator.aggregate(fieldSet) + LINE_SEPARATOR);
	}

	/**
	 * @see ResourceLifecycle#close(ExecutionContext)
	 */
	public void close(ExecutionContext executionContext) {
		if (state != null) {
			getOutputState().close();
			state = null;
		}
	}

	/**
	 * Sets encoding for output template.
	 */
	public void setEncoding(String newEncoding) {
		getOutputState().setEncoding(newEncoding);
	}

	/**
	 * Sets buffer size for output template
	 */
	public void setBufferSize(int newSize) {
		getOutputState().setBufferSize(newSize);
	}

	/**
	 * @param shouldDeleteIfExists the shouldDeleteIfExists to set
	 */
	public void setShouldDeleteIfExists(boolean shouldDeleteIfExists) {
		getOutputState().setShouldDeleteIfExists(shouldDeleteIfExists);
	}

	/**
	 * Initialize the Output Template.
	 * 
	 * @see ResourceLifecycle#open()
	 */
	public void open(ExecutionContext executionContext) {
		OutputState outputState = getOutputState();
		if (executionContext.containsKey(getKey(RESTART_DATA_NAME))) {
			outputState.restoreFrom(executionContext);
		}
	}

	/**
	 * @see ItemStream#update(ExecutionContext)
	 */
	public void update(ExecutionContext executionContext) {
		if (state == null) {
			throw new ItemStreamException("ItemStream not open or already closed.");
		}
		

		Assert.notNull(executionContext, "ExecutionContext must not be null");
		
		if(saveState){
		
			try {
				executionContext.putLong(getKey(RESTART_DATA_NAME), state.position());
			} catch (IOException e) {
				throw new ItemStreamException("ItemStream does not return current position properly", e);
			}
	
			executionContext.putLong(getKey(WRITTEN_STATISTICS_NAME), state.linesWritten);
			executionContext.putLong(getKey(RESTART_COUNT_STATISTICS_NAME), state.restartCount);
		}
	}

	// Returns object representing state.
	private OutputState getOutputState() {
		if (state == null) {
			state = new OutputState();
		}
		return (OutputState) state;
	}

	/**
	 * Encapsulates the runtime state of the writer. All state changing operations on the writer go through this class.
	 */
	private class OutputState {
		// default encoding for writing to output files - set to UTF-8.
		private static final String DEFAULT_CHARSET = "UTF-8";

		private static final int DEFAULT_BUFFER_SIZE = 2048;

		// The bufferedWriter over the file channel that is actually written
		BufferedWriter outputBufferedWriter;

		FileChannel fileChannel;

		// this represents the charset encoding (if any is needed) for the
		// output file
		String encoding = DEFAULT_CHARSET;

		// Optional write buffer size
		int bufferSize = DEFAULT_BUFFER_SIZE;

		boolean restarted = false;

		boolean initialized = false;

		long lastMarkedByteOffsetPosition = 0;

		long linesWritten = 0;

		long restartCount = 0;

		boolean shouldDeleteIfExists = true;

		/**
		 * Return the byte offset position of the cursor in the output file as a long integer.
		 */
		public long position() throws IOException {
			long pos = 0;

			if (fileChannel == null) {
				return 0;
			}

			outputBufferedWriter.flush();
			pos = fileChannel.position();

			return pos;

		}

		/**
		 * @param properties
		 */
		public void restoreFrom(ExecutionContext executionContext) {
			lastMarkedByteOffsetPosition = executionContext.getLong(getKey(RESTART_DATA_NAME));
			restarted = true;
		}

		/**
		 * @param shouldDeleteIfExists2
		 */
		public void setShouldDeleteIfExists(boolean shouldDeleteIfExists) {
			this.shouldDeleteIfExists = shouldDeleteIfExists;
		}

		/**
		 * @param newSize
		 */
		public void setBufferSize(int newSize) {
			bufferSize = newSize;
		}

		/**
		 * @param newEncoding
		 */
		public void setEncoding(String newEncoding) {
			encoding = newEncoding;
		}

		/**
		 * Close the open resource and reset counters.
		 */
		public void close() {
			initialized = false;
			restarted = false;
			try {
				if (outputBufferedWriter == null) {
					return;
				}
				outputBufferedWriter.close();
				fileChannel.close();
			} catch (IOException ioe) {
				throw new ItemStreamException("Unable to close the the ItemWriter", ioe);
			}
		}

		/**
		 * @param data
		 * @param offset
		 * @param length
		 * @throws IOException
		 */
		public void write(String line) throws IOException {
			if (!initialized) {
				initializeBufferedWriter();
			}

			outputBufferedWriter.write(line);
			outputBufferedWriter.flush();
			linesWritten++;
		}

		/**
		 * Truncate the output at the last known good point.
		 * 
		 * @throws IOException
		 */
		public void truncate() throws IOException {
			fileChannel.truncate(lastMarkedByteOffsetPosition);
			fileChannel.position(lastMarkedByteOffsetPosition);
		}

		/**
		 * Mark the current position.
		 */
		public void mark() {
			try {
				lastMarkedByteOffsetPosition = this.position();
			} catch (IOException e) {
				throw new MarkFailedException("Unable to get position for mark", e);
			}
		}

		/**
		 * Creates the buffered writer for the output file channel based on configuration information.
		 */
		private void initializeBufferedWriter() {
			File file;

			try {
				file = resource.getFile();

				// If the output source was restarted, keep existing file.
				// If the output source was not restarted, check following:
				// - if the file should be deleted, delete it if it was exiting
				// and create blank file,
				// - if the file should not be deleted, if it already exists,
				// throw an exception,
				// - if the file was not existing, create new.
				if (!restarted) {
					if (file.exists()) {
						if (shouldDeleteIfExists) {
							file.delete();
						} else {
							throw new ItemStreamException("Resource already exists: " + resource);
						}
					}
					String parent = file.getParent();
					if (parent != null) {
						new File(parent).mkdirs();
					}
					file.createNewFile();
				}

			} catch (IOException ioe) {
				throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]",
				        ioe);
			}

			try {
				fileChannel = (new FileOutputStream(file.getAbsolutePath(), true)).getChannel();
			} catch (FileNotFoundException fnfe) {
				throw new ItemStreamException("Bad filename property parameter " + file, fnfe);
			}

			outputBufferedWriter = getBufferedWriter(fileChannel, encoding, bufferSize);

			// in case of restarting reset position to last committed point
			if (restarted) {
				this.reset();
			}

			initialized = true;
			linesWritten = 0;
		}

		/**
		 * Returns the buffered writer opened to the beginning of the file specified by the absolute path name contained
		 * in absoluteFileName.
		 */
		private BufferedWriter getBufferedWriter(FileChannel fileChannel, String encoding, int bufferSize) {
			try {

				BufferedWriter outputBufferedWriter = null;

				// If a buffer was requested, allocate.
				if (bufferSize > 0) {
					outputBufferedWriter = new BufferedWriter(Channels.newWriter(fileChannel, encoding), bufferSize);
				} else {
					outputBufferedWriter = new BufferedWriter(Channels.newWriter(fileChannel, encoding));
				}

				return outputBufferedWriter;
			} catch (UnsupportedCharsetException ucse) {
				throw new ItemStreamException("Bad encoding configuration for output file " + fileChannel, ucse);
			}
		}

		/**
		 * Resets the file writer's current position to the point stored in the last marked byte offset position
		 * variable. It first checks to make sure the current size of the file is not less than the byte position to be
		 * moved to (if it is, throws an environment exception), then it truncates the file to that reset position, and
		 * set the cursor to start writing at that point.
		 */
		public void reset() throws ResetFailedException {
			checkFileSize();
			try {
				getOutputState().truncate();
			} catch (IOException e) {
				throw new ResetFailedException("Unable to truncate file", e);
			}
		}

		/**
		 * Checks (on setState) to make sure that the current output file's size is not smaller than the last saved
		 * commit point. If it is, then the file has been damaged in some way and whole task must be started over again
		 * from the beginning.
		 */
		private void checkFileSize() {
			long size = -1;

			try {
				outputBufferedWriter.flush();
				size = fileChannel.size();
			} catch (IOException e) {
				throw new ResetFailedException("An Error occured while checking file size", e);
			}

			if (size < lastMarkedByteOffsetPosition) {
				throw new ResetFailedException("Current file size is smaller than size at last commit");
			}
		}

	}

	public void clear() throws ClearFailedException {
		try {
			getOutputState().reset();
		} catch (Exception e) {
			throw new ClearFailedException("Could not reset the state of the writer", e);
		}
	}

	public void flush() throws FlushFailedException {
		getOutputState().mark();
	}
	
	/**
	 * Set the boolean indicating whether or not state should be saved
	 * in the provided {@link ExecutionContext} during the {@link ItemStream}
	 * call to update.  Setting this to false means that it will always start
	 * at the beginning.
	 * 
	 * @param saveState
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

}
