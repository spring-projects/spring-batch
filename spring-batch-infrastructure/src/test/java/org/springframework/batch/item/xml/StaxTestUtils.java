/*
 * Copyright 2015-2018 the original author or authors.
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

import java.lang.reflect.Method;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * Utility methods for StAX related tests.
 */
public final class StaxTestUtils {

	public static XMLEventWriter getXmlEventWriter(Result r) throws Exception {
	    Method m = r.getClass().getDeclaredMethod("getXMLEventWriter");
	    boolean accessible = m.isAccessible();
	    m.setAccessible(true);
	    Object result = m.invoke(r);
	    m.setAccessible(accessible);
	    return (XMLEventWriter) result;
	}

	public static XMLEventReader getXmlEventReader(Source s) throws Exception {
	    Method m = s.getClass().getDeclaredMethod("getXMLEventReader");
	    boolean accessible = m.isAccessible();
	    m.setAccessible(true);
	    Object result = m.invoke(s);
	    m.setAccessible(accessible);
	    return (XMLEventReader) result;
	}

}
