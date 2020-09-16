/*
 * Copyright 2006-2020 the original author or authors.
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

package org.springframework.batch.item.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;


/**
 * StAX utility methods.
 * <br>
 * This class is thread-safe.
 *
 * @author Josh Long
 * @author Mahmoud Ben Hassine
 * 
 * @deprecated in favor of {@link org.springframework.util.xml.StaxUtils}
 *
 */
@Deprecated
public abstract class StaxUtils {

	public static Source getSource(XMLEventReader r) throws XMLStreamException {
		return new StAXSource(r);
	}

	public static Result getResult(XMLEventWriter w) {
		return new StAXResult(w);
	}

	public static XMLInputFactory createXmlInputFactory() {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		return xmlInputFactory;
	}
}
