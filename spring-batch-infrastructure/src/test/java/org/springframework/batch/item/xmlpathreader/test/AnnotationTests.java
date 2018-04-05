/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.StaxXmlPathReader;
import org.springframework.batch.item.xmlpathreader.test.entities.TAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TObject;
import org.springframework.batch.item.xmlpathreader.test.entities.TStaticAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TWrongAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TWrongFunctionNameAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TWrongFunctionParameterCountAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TWrongStaticFunctionParameterCountAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TWrongStaticFunctionReturnWrongTypeAnnotated;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class AnnotationTests {
	private static final Logger log = LoggerFactory.getLogger(AnnotationTests.class);

	private static final String EXCEPTION_NOT_EXPECTED = "this exception is a error";

	private static final String EXCEPTION_EXPECTED = "Exception expected";

	/**
	 * {@link TObject} has no annotation
	 */
	@Test
	public void wrongClassWithoutAnnotation() {
		try {
			new StaxXmlPathReader(TObject.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * {@link TWrongAnnotated} has addName annotated
	 */
	@Test
	public void wrongAnnotatedMethodIsNotSetter() {
		try {
			new StaxXmlPathReader(TWrongAnnotated.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * {@link TWrongFunctionNameAnnotated} has the getter annotated
	 * 
	 */
	@Test
	public void wrongAnnotatedMethodWithoutArgumentsAndWrongFunctionName() {
		try {
			new StaxXmlPathReader(TWrongFunctionNameAnnotated.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * {@link TWrongFunctionParameterCountAnnotated} has a method with two parameters annotated
	 */
	@Test
	public void wrongFunctionParameterCountName() {
		try {
			new StaxXmlPathReader(TWrongFunctionParameterCountAnnotated.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * {@link TWrongStaticFunctionParameterCountAnnotated} has a method with two parameters annotated
	 */

	@Test
	public void wrongStaticFunctionParameterCount() {
		try {
			new StaxXmlPathReader(TWrongStaticFunctionParameterCountAnnotated.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * {@link TWrongStaticFunctionReturnWrongTypeAnnotated} has a static method that return Double annotated, but it
	 * should be TWrongStaticFunctionReturnWrongTypeAnnotated
	 */
	@Test
	public void wrongStaticFunctionReturnType() {
		try {
			new StaxXmlPathReader(TWrongStaticFunctionReturnWrongTypeAnnotated.class);
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.info(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * Correct annotated class is really ok
	 */
	@Test
	public void readAnnotatedClass() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(TAnnotated.class);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Correct annotated class with a static method is really ok
	 */
	@Test
	public void readStaticAnnotatedClass() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(TStaticAnnotated.class);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

}
