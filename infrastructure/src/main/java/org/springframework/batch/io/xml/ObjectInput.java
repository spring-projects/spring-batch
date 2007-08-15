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
 * The <code>ObjectInput</code> interface provides method for reading objects
 * from input stream and also provides methods for manipulating input stream.
 * @author peter.zozom
 * @see XStreamFactory.ObjectInputWrapper
 */
public interface ObjectInput {

	/**
	 * Enables input stream postprocess after reading from the stream has been
	 * restarted.
	 * @param data
	 */
	public void afterRestart(Object data);

	/**
	 * Read and return an object. The class that implements this interface
	 * defines where the object is "read" from.
	 * 
	 * @return the object read from the stream
	 * @exception java.lang.ClassNotFoundException If the class of a serialized
	 * object cannot be found.
	 * @exception IOException If any of the usual Input/Output related
	 * exceptions occur.
	 */
	public Object readObject() throws ClassNotFoundException, IOException;

	/**
	 * Closes the input stream. Must be called to release any resources
	 * associated with the stream.
	 */
	public void close();

	/**
	 * Get actual position in the input stream.
	 * @return actual position in the input stream
	 */
	public long position();
}
