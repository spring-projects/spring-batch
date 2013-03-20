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

package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A multi-threaded aware {@link FlatFileItemReader} implementation
 * that starts at the offset specified in bytes relative to the file begin
 * and reads maxItemCount items/lines from the file.
 * The offset and number of items to read is passed vie ExecutionContext
 * parameters set by the {@link FlatFilePartitioner}
 * <p/>
 * Reads all the file by default.
 * 
 * The usage is similar to the {@link FlatFileItemReader}. The <tt>reource</tt>, <tt>maxItemCount</tt>, <tt>startAt</tt>
 * properties can be set from the <tt>stepExecutionContext</tt> populated by the {@link FlatFilePartitioner}:
 * <p/>
 * <pre>
 * {@code <bean id="partitioner" class="org.springframework.batch.core.partition.support.FlatFilePartitioner" scope="step"
 *          p:resource="#{jobParameters['input.file']}" /&gt;
 *          
 *  &lt;bean id="myReader" class="org.springframework.batch.item.file.MultiThreadedFlatFileItemReader" scope="step"&gt;
 *  	...
 *      &lt;property name="resource" value="#{stepExecutionContext['resource']}"/&gt;
 *      &lt;property name="maxItemCount" value="#{stepExecutionContext['itemsCount']}" /&gt;
 *      &lt;property name="startAt" value="#{stepExecutionContext['startAt']}"/&gt;
 *  &lt;/bean&gt;
 * </pre>
 * @author Sergey Shcherbakov
 */
public class MultiThreadedFlatFileItemReader<T> extends FlatFileItemReader<T> {

    /**
     * The number of bytes the partition should skip on startup.
     */
    public static final String START_AT_KEY = "start.at";

    private long startAt = 0;

    public MultiThreadedFlatFileItemReader() {
        setName(ClassUtils.getShortName(MultiThreadedFlatFileItemReader.class));
    }

    /**
     * Sets the byte offset within the file at which this instance should start reading. Set
     * to 0 by default so that this instance starts at the first item.
     *
     * @param startAt the byte offset at which this instance should
     * start reading
     */
    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }
    
    @Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
    	if (isSaveState()) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
    		if (executionContext.containsKey(getExecutionContextKey(START_AT_KEY))) {
    			startAt = executionContext.getLong(getExecutionContextKey(START_AT_KEY));
    		}
    	}
    	// replace the DefaultBufferedReaderFactory with an implementation that seeks to the start before reading
        setBufferedReaderFactory(new SkippingBufferedReaderFactory(this.startAt));
    	super.open(executionContext);
	}

    @Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (isSaveState()) {
			executionContext.putLong(getExecutionContextKey(START_AT_KEY), startAt);
		}
	}

    public static class SkippingBufferedReaderFactory implements BufferedReaderFactory {
    	
    	private long skipBytes;

    	public SkippingBufferedReaderFactory() {
    		this(0L);
    	}
    	
    	public SkippingBufferedReaderFactory(long skipBytes) {
    		this.skipBytes = skipBytes;
    	}
    	
    	/* (non-Javadoc)
    	 * @see org.springframework.batch.item.file.BufferedReaderFactory#create(org.springframework.core.io.Resource, java.lang.String)
    	 */
    	public BufferedReader create(Resource resource, String encoding) throws UnsupportedEncodingException, IOException {
    		InputStream is = resource.getInputStream();
    		is.skip(this.skipBytes);
    		return new BufferedReader(new InputStreamReader(is, encoding));
    	}
    	
    	public void setSkipBytes(long skipBytes) {
    		this.skipBytes = skipBytes;
    	}

    }

}
