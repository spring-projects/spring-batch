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

public class BatchNamespaceHandlerJobRepositoryTests extends TestCase {

	private static final String PACKAGE = "org/springframework/batch/execution/configuration/";

	public void testRepositoryOk() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryOk.xml");
	}

	public void testRepositoryMissingDataSource() {
		try {
			new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryMissingDataSource.xml");
			fail("Expected BeanDefinitionParsingException");
		} catch (BeanDefinitionParsingException e) {	}
	}
	
	public void testRepositoryDb2() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryDb2.xml");
	}
	
	public void testRepositoryDerby() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryDerby.xml");
	}
	
	public void testRepositoryHsql() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryHsql.xml");
	}
	
	public void testRepositoryMySql() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryMySql.xml");
	}
	
	public void testRepositoryOracle() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryOracle.xml");
	}
	
	public void testRepositoryPostgres() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerJobRepositoryPostgres.xml");
	}
}
