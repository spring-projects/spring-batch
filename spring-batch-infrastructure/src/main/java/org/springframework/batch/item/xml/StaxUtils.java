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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This class provides a little bit of indirection to avoid ugly conditional object creation. It is unfortunately
 * a bit redundant assuming a Spring 3.0 environment, but is necessary to work with Spring WS 1.5.x.
 * <p/>
 * The returned object determines whether the environment has Spring OXM as included in the Spring 3.x series of relies
 * or whether it has Spring OXM from Spring WS 1.5x and factories a StaxSource instance appropriately.
 * <p/>
 * As the only class state maintained is to cache java reflection metadata, which is thread safe, this class is thread-safe.
 *
 * @author Josh Long
 *
 * @see org.springframework.xml.transform.StaxSource
 */
public abstract class StaxUtils {

	private static final Log logger = LogFactory.getLog(StaxUtils.class);

	private static ClassLoader defaultClassLoader = ClassUtils.getDefaultClassLoader();

	// regular object.
	private static String staxSourceClassNameOnSpringWs15 = "org.springframework.xml.transform.StaxSource";
	private static String staxResultClassNameOnSpringOxm15 = "org.springframework.xml.transform.StaxResult";

	// in Spring 3, StaxUtils is package private, so use static utility StaxUtils#createStaxSource / StaxUtils#createStaxResult
	private static String staxSourceClassNameOnSpringOxm30 = "org.springframework.util.xml.StaxUtils";

	private static boolean hasSpringWs15StaxSupport = ClassUtils.isPresent(staxSourceClassNameOnSpringWs15, defaultClassLoader);

	private static boolean hasSpring30StaxSupport = ClassUtils.isPresent(staxSourceClassNameOnSpringOxm30, defaultClassLoader);

	private static Method staxUtilsSourceMethodOnSpring30, staxUtilsResultMethodOnSpring30;

	@SuppressWarnings("rawtypes")
	private static Constructor staxSourceClassCtorOnSpringWs15, staxResultClassCtorOnSpringWs15;

	static {
		try {

			// cache the factory method / constructor so that we spend as little time in reflection as possible
			if (hasSpring30StaxSupport) {
				Class<?> clzz = ClassUtils.forName(staxSourceClassNameOnSpringOxm30, defaultClassLoader);

				// javax.xml.transform.Source
				staxUtilsSourceMethodOnSpring30 = ClassUtils.getStaticMethod(clzz, "createStaxSource", new Class[]{ XMLEventReader.class});

				// javax.xml.transform.Result
				staxUtilsResultMethodOnSpring30 = ClassUtils.getStaticMethod(clzz, "createStaxResult", new Class[]{XMLEventWriter.class});
			} else if (hasSpringWs15StaxSupport) {

				// javax.xml.transform.Source
				Class<?> staxSourceClassOnSpringWs15 = ClassUtils.forName(staxSourceClassNameOnSpringWs15, defaultClassLoader);
				staxSourceClassCtorOnSpringWs15 = staxSourceClassOnSpringWs15.getConstructor(XMLEventReader.class);

				// javax.xml.transform.Result
				Class<?> staxResultClassOnSpringWs15 = ClassUtils.forName(staxResultClassNameOnSpringOxm15, defaultClassLoader);
				staxResultClassCtorOnSpringWs15 = staxResultClassOnSpringWs15.getConstructor(XMLEventWriter.class);
			} else {

				logger.debug("'StaxSource' was not detected in Spring 3.0's OXM support or Spring WS 1.5's OXM support. " +
						"This is a problem if you intend to use the " +StaxEventItemWriter.class.getName() + " or " +
						StaxEventItemReader.class.getName()+". Please add the appropriate dependencies.");

			}
		} catch (Exception ex) {
			logger.error("Could not precache required class and method metadata in " + StaxUtils.class.getName());
		}
	}

	public static Source getSource(XMLEventReader r) throws Exception {
		if (hasSpring30StaxSupport) {
			// org.springframework.util.xml.StaxUtils.createStaxSource(r)
			Object result = staxUtilsSourceMethodOnSpring30.invoke(null,r);
			Assert.isInstanceOf(Source.class, result, "the result should be assignable to " + Source.class.getName());
			return (Source) result;
		} else if (hasSpringWs15StaxSupport) {
			Object result = staxSourceClassCtorOnSpringWs15.newInstance(r);
			Assert.isInstanceOf(Source.class, result, "the result should be assignable to " + Source.class.getName());
			return (Source) result;
		}
		// maybe you don't have either environment?
		return null;
	}

	public static Result getResult(XMLEventWriter w) throws Exception {
		if (hasSpring30StaxSupport) {
			Object result = staxUtilsResultMethodOnSpring30.invoke(null,w);
			Assert.isInstanceOf(Result.class, result, "the result should be assignable to " + Result.class.getName());
			return (Result) result;
		} else if (hasSpringWs15StaxSupport) {
			Object result = staxResultClassCtorOnSpringWs15.newInstance(w);
			Assert.isInstanceOf(Result.class, result, "the result should be assignable to " + Result.class.getName());
			return (Result) result;
		}
		// maybe you don't have either environment?
		return null;
	}

	public static XMLEventWriter getXmlEventWriter(Result r) throws Exception {
		Method m = ClassUtils.getMethodIfAvailable(r.getClass(), "getXMLEventWriter", new Class[]{});
		boolean accessible=m.isAccessible();
		m.setAccessible(true);
		Object result = m.invoke(r);
		m.setAccessible(accessible);
		return (XMLEventWriter) result;
	}

	public static XMLEventReader getXmlEventReader(Source s) throws Exception {
		Method m = ClassUtils.getMethodIfAvailable(s.getClass(), "getXMLEventReader", new Class[]{});
		boolean accessible=m.isAccessible();
		m.setAccessible(true);
		Object result = m.invoke(s);
		m.setAccessible(accessible);
		return (XMLEventReader) result;
	}
}
