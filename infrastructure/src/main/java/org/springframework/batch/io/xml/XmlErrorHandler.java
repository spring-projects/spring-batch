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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX error handler implementation. This implementation only logs warnings and
 * errors and re-throws exceptions.
 * 
 * @author peter.zozom
 */
public class XmlErrorHandler extends DefaultHandler {
	private static final Log log = LogFactory.getLog(XmlErrorHandler.class);

	/**
	 * @param e parsing exception
	 * @throws SAXException re-thrown exception
	 * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
	 */
	public void warning(SAXParseException e) throws SAXException {
		log.debug("Warning: \n" + printInfo(e));
		throw new SAXException(e);
	}

	/**
	 * @param e parsing exception
	 * @throws SAXException re-thrown exception
	 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException e) throws SAXException {
		log.debug("Warning: \n" + printInfo(e));
		throw new SAXException(e);
	}

	/**
	 * @param e parsing exception
	 * @throws SAXException re-thrown exception
	 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	public void fatalError(SAXParseException e) throws SAXException {
		log.debug("Warning: \n" + printInfo(e));
		throw new SAXException(e);
	}

	private String printInfo(SAXParseException e) {
		StringBuffer sb = new StringBuffer();
		sb.append("   Public ID: \n");
		sb.append(e.getPublicId());
		sb.append("   System ID: \n");
		sb.append(e.getSystemId());
		sb.append("   Line number: \n");
		sb.append(e.getLineNumber());
		sb.append("   Column number: \n");
		sb.append(e.getColumnNumber());
		sb.append("   Message: \n");
		sb.append(e.getMessage());

		return sb.toString();
	}
}
