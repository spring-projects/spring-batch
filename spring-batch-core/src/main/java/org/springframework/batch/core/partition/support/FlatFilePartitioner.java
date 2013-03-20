/*
 * Copyright 2006-2013 the original author or authors.
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

package org.springframework.batch.core.partition.support;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Creates a set of partitions for a flat text file.
 * <p/>
 * Assumes that each record is stored on one and only one line.
 * Reads the file's byte stream detecting line ends and creates partitions
 * splitted at the new line border. Populates the {@link ExecutionContext} with
 * the byte offset for each partition thread and number of items/lines to be read from that position.
 * <p/>
 * Can be used to read the file concurrently. Each partition thread should use the byte offset specified by the
 * <tt>startAt</tt> 
 * offset to set cursor at the starting position and a number of items (lines) to read as defined 
 * by the <tt>itemsCount</tt> property.
 *
 * @author Sergey Shcherbakov
 * @author Stephane Nicoll
 */
public class FlatFilePartitioner implements Partitioner {

    /**
     * The {@link ExecutionContext} key name for the number of bytes the partition should skip on startup.
     */
    public static final String DEFAULT_START_AT_KEY = "startAt";

    /**
     * The {@link ExecutionContext} key name for number of items/lines to read in the partition.
     */
    public static final String DEFAULT_ITEMS_COUNT_KEY = "itemsCount";

    /**
     * The {@link ExecutionContext} key name for the file resource which has been used for partitioning.
     */
	public static final String DEFAULT_RESOURCE_KEY = "resource";

    /**
     * The common partition prefix name to use.
     */
    public static final String PARTITION_PREFIX = "partition-";

    private final Logger logger = LoggerFactory.getLogger(FlatFilePartitioner.class);

    private Resource resource;

    private String startAtKeyName = DEFAULT_START_AT_KEY;
    private String itemsCountKeyName = DEFAULT_ITEMS_COUNT_KEY;
    private String resourceKeyName = DEFAULT_RESOURCE_KEY;
    
	/**
	 * The name of the key for the byte offset in each {@link ExecutionContext}.
	 * Defaults to "startAt".
	 * @param keyName the value of the key
	 */
	public void setStartAtKeyName(String keyName) {
		this.startAtKeyName = keyName;
	}

	/**
	 * The name of the key for the byte offset in each {@link ExecutionContext}.
	 * Defaults to "itemsCount".
	 * @param keyName the value of the key
	 */
	public void setItemsCountKeyName(String keyName) {
		this.itemsCountKeyName = keyName;
	}

	/**
	 * The name of the key for the file name in each {@link ExecutionContext}.
	 * Defaults to "resource".
	 * @param keyName the value of the key
	 */
	public void setResourceKeyName(String keyName) {
		this.resourceKeyName = keyName;
	}

    /**
     * Creates a set of {@link ExecutionContext} according to the provided
     * <tt>gridSize</tt> if there are enough elements.
     * <p/>
     * First computes the total number of items to process for the resource
     * and then split equality these in each partition. The returned context
     * hold the {@link #DEFAULT_START_AT_KEY} and {@link #DEFAULT_ITEMS_COUNT_KEY} properties
     * defining the number of elements to skip and the number of elements to
     * read respectively.
     *
     * @param gridSize the requested size of the grid
     * @return the execution contexts
     * @see #countItems(org.springframework.core.io.Resource)
     */
    public Map<String, ExecutionContext> partition(int gridSize) {
		Assert.isTrue(gridSize > 0, "Grid size must be greater than 0");

        checkResource(this.resource);
        if (logger.isDebugEnabled()) {
            logger.debug("Splitting [" + resource.getDescription() + "]");
        }
        try {
	        final Map<String, ExecutionContext> result = new LinkedHashMap<String, ExecutionContext>();
	        
	        final long sizeInBytes = resource.contentLength();
	        if (sizeInBytes == 0) {
	            logger.info("Empty input file [" + resource.getDescription() + "] no partition will be created.");
	            return result;
	        }

	        PartitionBorderCursor partitionCursor = new PartitionBorderCursor(gridSize, sizeInBytes); 
		        
	        // Check the case that the set is to small for the number of request partition(s)
	        if (partitionCursor.getBytesPerPartition() == 0) {
	        	long lines = countItems(resource);
	            logger.info("Not enough data (" + lines + ") for the requested gridSize [" + gridSize + "]");
	            partitionCursor.createPartition( 0, lines, result );
	            return result;
	        }

	        if (logger.isDebugEnabled()) {
	            logger.debug("Has to split [" + sizeInBytes + "] byte(s) in [" + gridSize + "] " +
	                    "grid(s) (" + partitionCursor.getBytesPerPartition() + " each)");
	        }

            final int BUFFER_SIZE = 4096;
            final InputStream in = resource.getInputStream();
        	try {
	            final InputStream is = new BufferedInputStream(in);
				byte[] c = new byte[BUFFER_SIZE];
				ByteStreamCursor byteCursor = new ByteStreamCursor(); 
	            int readChars;
	            while ((readChars = is.read(c)) != -1) {
	                for (int i = 0; i < readChars; ++i) {
	                	
	                	if( byteCursor.lastSeenCharIsNewline( c[i] ) ) {
		                	if( byteCursor.getCurrentByteInd() > partitionCursor.getPartitionBorder() ) {
		                		
		                		partitionCursor.createPartition( byteCursor.getStartAt(), 
		    	            			byteCursor.getLineCount(), result );
		    	            	
		    	            	byteCursor.startNewPartition();
		                	}
	                    }
	                }
	            }
	            if ( byteCursor.lastLineUnterminated() ) {
	            	byteCursor.startNewLine();
	            }
	            if( byteCursor.outstandingData() ) {
	            	partitionCursor.createPartition( byteCursor.getStartAt(), 
	            			byteCursor.getLineCount(), result );
	            }
		        return result;
        	}
        	finally {
                in.close();
        	}
        }
        catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception while partitioning ["
                    + resource.getDescription() + "]", e);
        }
    }
    
    /**
     * This is a helper class to simplify the byte stream iterating code.
     * Tracks current location in the byte stream, number of lines counted from the
     * last partition start and from the input stream beginning.
     * Increments indexes on a new character read. 
     * Detects the new line character and updates counters.
     */
    private static class ByteStreamCursor {
        private long totalLineCount = 0;
        private long lineCount = 0;
        private byte lastSeenChar = 0;
        private long currentByteInd = 0L;
        private long startAt = 0;
        
		public boolean lastSeenCharIsNewline(byte lastSeenChar) {
			this.lastSeenChar = lastSeenChar;
			this.currentByteInd++;
            // New line is \n on Unix and \r\n on Windows                
            if (lastSeenChar == '\n') {
            	startNewLine();
                return true;
            }
            return false;
		}
		
		public void startNewLine() {
            lineCount++;
            totalLineCount++;
		}

		public void startNewPartition() {
            startAt = currentByteInd;
            lineCount = 0;
		}

		public long getLineCount() {
			return lineCount;
		}

		public long getStartAt() {
			return startAt;
		}

		public long getCurrentByteInd() {
			return currentByteInd;
		}
		
		public boolean lastLineUnterminated() {
			return (totalLineCount > 0 && lastSeenChar != '\n') || 						// <-- last line is not empty but is not terminated by '\n'
	            (totalLineCount == 0 && lastSeenChar != '\n' && currentByteInd > 0);	// <-- the first line is the last line and it's not terminated by '\n'
		}
		
		public boolean outstandingData() {
			return currentByteInd > 0 && startAt != currentByteInd;
		}
    }

    /**
     * This is a helper class to simplify the byte stream iterating code.
     * Tracks the location of approximate byte offsets that split the input file into
     * approximately (+/-1) equal byte partitions.
     * When the main iteration passes this border the next partition will be created as soon
     * as the next new line character or end of stream is detected.
     */
    private class PartitionBorderCursor {
    	private int gridSize;
        private final long bytesPerPartition;
        private final long bytesRemainder;
        private long remainderCounter;
        private long partitionBorder;
        private int partitionIndex;

    	PartitionBorderCursor(int gridSize, long sizeInBytes) {
    		this.gridSize = gridSize;
            this.bytesPerPartition = sizeInBytes / gridSize;
            this.bytesRemainder = sizeInBytes % gridSize;
            this.remainderCounter = this.bytesRemainder;
            this.partitionBorder = 0;
            this.partitionIndex = 0;
			toNextPartitionBorder();
    	}

		public long getBytesPerPartition() {
			return bytesPerPartition;
		}
		
		public long getPartitionBorder() {
			return this.partitionBorder;
		}
		
		private void toNextPartitionBorder() {
			this.partitionBorder += bytesPerPartition + (remainderCounter-- > 0 ? 1 : 0);
		}
		
		public void createPartition(long startAt, long lineCount, 
				final Map<String, ExecutionContext> result) {

			final String partitionName = getPartitionName(gridSize, partitionIndex++);
			result.put(partitionName, createExecutionContext(partitionName, startAt, lineCount));
			toNextPartitionBorder();
		}
		
		private String getPartitionName(int gridSize, int partitionIndex) {
			final String partitionNumberFormat = "%0" + String.valueOf(gridSize).length() + "d";
			return PARTITION_PREFIX + String.format(partitionNumberFormat, partitionIndex);
		}
    }
    
    /**
     * Creates a standard {@link ExecutionContext} with the specified parameters.
     * @param partitionName the name of the partition
     * @param startAt the number of bytes for a partition thread to skip before starting reading
     * @param itemsCount the number of items to read
     * @return the execution context (output)
     */
    protected ExecutionContext createExecutionContext(String partitionName, long startAt, long itemsCount) {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong(startAtKeyName, startAt);
        executionContext.putLong(itemsCountKeyName, itemsCount);
		try {
			executionContext.putString(resourceKeyName, "file:" + resource.getFile().getPath());
		} catch (IOException e) {
			throw new IllegalArgumentException("File could not be located for: "+resource, e);
		}
		if (logger.isDebugEnabled()) {
            logger.debug("Added partition [" + partitionName + "] with [" + executionContext + "]");
        }
        return executionContext;
    }

    /**
     * Returns the number of elements in the specified {@link Resource}.
     *
     * @param resource the resource
     * @return the number of items contained in the resource
     */
    protected long countItems(Resource resource) {
        try {
            final InputStream in = resource.getInputStream();
            try {
                return countLines(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception while counting items for ["
                    + resource.getDescription() + "]", e);
        }
    }

    /**
     * Returns the number of lines found in the specified stream.
     * <p/>
     * The caller is responsible to close the stream.
     *
     * Up to 5 times faster than using BufferedReader and up to 2 times faster 
     * than LineNumberReader.
     * 
     * @param in the input stream to use
     * @return the number of lines found in the stream
     * @throws IOException if an error occurred
     */    
    public static long countLines(InputStream in) throws IOException {
        final InputStream is = new BufferedInputStream(in);
        byte[] c = new byte[4096];
        long count = 0;
        int readChars;
        byte lastChar = 0;
        boolean contentExists = false;
        while ((readChars = is.read(c)) != -1) {
            for (int i = 0; i < readChars; ++i) {
            	contentExists = true;
            	lastChar = c[i];
                // We're dealing with the char here, it's \n on Unix and \r\n on Windows                
                if (c[i] == '\n')
                    ++count;
            }
        }
        // Last line
        if ( (count > 0 && lastChar != '\n') || 						// <-- last line is not empty but is not terminated by '\n'
        	(count == 0 && lastChar != '\n' && contentExists) ) {		// <-- the first line is the last line and it's not terminated by '\n'
            count++;
        }
        return count;
    }

    /**
     * Checks whether the specified {@link Resource} is valid.
     *
     * @param resource the resource to check
     * @throws IllegalStateException if the resource is invalid
     */
    protected void checkResource(Resource resource) {
    	Assert.notNull(resource, "Resource is not set");
        if (!resource.exists()) {
            throw new IllegalStateException("Input resource must exist: " + resource);
        }
        if (!resource.isReadable()) {
            throw new IllegalStateException("Input resource must be readable: " + resource);
        }
    }

    /**
     * Sets the input {@link Resource} to use.
     *
     * @param resource the resource to partition
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
