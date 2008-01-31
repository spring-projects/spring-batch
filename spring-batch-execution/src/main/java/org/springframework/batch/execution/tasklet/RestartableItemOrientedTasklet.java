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

package org.springframework.batch.execution.tasklet;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.StreamException;

/**
 * An extension of {@link ItemOrientedTasklet} that delegates calls to
 * {@link ItemStream} to the reader and writer.
 * 
 * @see ItemReader
 * @see ItemWriter
 * @see ItemStream
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class RestartableItemOrientedTasklet extends ItemOrientedTasklet implements ItemStream {

	/**
	 * @see ItemStream#getStreamContext()
	 */
	public StreamContext getStreamContext() {
		throw new UnsupportedOperationException("This class is not used");
	}

	/**
	 * @see ItemStream#restoreFrom(StreamContext)
	 */
	public void restoreFrom(StreamContext data) {
		throw new UnsupportedOperationException("This class is not used");
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open() throws StreamException {
		throw new UnsupportedOperationException("Not implemented.");
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	public void close() throws StreamException {
		throw new UnsupportedOperationException("Not implemented.");		
	}
}
