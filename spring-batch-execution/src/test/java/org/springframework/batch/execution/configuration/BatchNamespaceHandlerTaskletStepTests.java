/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import junit.framework.TestCase;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BatchNamespaceHandlerTaskletStepTests extends TestCase {

	private static final String PACKAGE = "org/springframework/batch/execution/configuration/";

	public void testNamespaceOk() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepOk.xml");
	}

	public void testNamespaceMissingTasklet() {
		try {
			new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepMissingTasklet.xml");
			fail("Expected BeanDefinitionParsingException");
		} catch (BeanDefinitionParsingException e) {

		}
	}

}
