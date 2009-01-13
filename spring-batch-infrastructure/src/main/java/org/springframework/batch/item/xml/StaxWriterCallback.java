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

package org.springframework.batch.item.xml;

import java.io.IOException;

import javax.xml.stream.XMLEventWriter;

/**
 * Callback interface for writing to an XML file - useful e.g. for handling headers
 * and footers.
 * 
 * @author Robert Kasanicky
 */
public interface StaxWriterCallback {

	/**
	 * Write contents using the supplied {@link XMLEventWriter}. It is not
	 * required to flush the writer inside this method.
	 */
	void write(XMLEventWriter writer) throws IOException;
}
