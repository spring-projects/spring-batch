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

package org.springframework.batch.io.xml;

import java.io.IOException;

import org.springframework.batch.io.xml.xstream.XStreamFactory;

/**
 * The <code>ObjectOutput</code> interface provides methods for writing
 * objects to output stream and also provides methods for manipulating output
 * stream (such as flush, position, truncate, etc.).
 * @author peter.zozom
 * @see XStreamFactory.ObjectOutputWrapper
 */
public interface ObjectOutput {

	/**
	 * Enables output stream postprocess after writing to the stream has been
	 * restarted.
	 * @param data
	 */
	public void afterRestart(Object data);

	/**
	 * Write an object to the stream. The class that implements this interface
	 * defines how the object is written.
	 * 
	 * @param obj the object to be written
	 * @exception IOException Any of the usual Input/Output related exceptions.
	 */
	public void writeObject(Object obj) throws IOException;

	/**
	 * Closes the stream. This method must be called to release any resources
	 * associated with the stream.
	 */
	public void close();

	/**
	 * Flushes the stream. This will write any buffered output bytes.
	 */
	public void flush();

	/**
	 * Get actual position in the output stream.
	 * @return actual position in the output stream
	 */
	public long position();

	/**
	 * Set a new position in the output stream.
	 * @param newPosition the new position, a non-negative integer counting the
	 * number of bytes from the beginning of the file
	 */
	public void position(long newPosition);

	/**
	 * Truncates the otput file to the given size.
	 * @param size the new size, a non-negative byte count
	 */
	public void truncate(long size);

	/**
	 * Returns the current size of the output file
	 * @return The current size of the output file, measured in bytes
	 */
	public long size();

}
