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

public class JobRepositoryBeanDefinitionParserTests extends TestCase {

	private static final String PACKAGE = "org/springframework/batch/execution/configuration/";

	public void testJobRepoOk() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoOk.xml");
	}

	public void testJobRepoMissingDataSource() {
		try {
			new ClassPathXmlApplicationContext(PACKAGE + "JobRepoMissingDataSource.xml");
			fail("Expected BeanDefinitionParsingException");
		} catch (BeanDefinitionParsingException e) {
		}
	}

	public void testJobRepoDb2() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoDb2.xml");
	}

	public void testJobRepoDerby() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoDerby.xml");
	}

	public void testJobRepoHsql() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoHsql.xml");
	}

	public void testJobRepoMySql() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoMySql.xml");
	}

	public void testJobRepoOracle() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoOracle.xml");
	}

	public void testJobRepoPostgres() {
		new ClassPathXmlApplicationContext(PACKAGE + "JobRepoPostgres.xml");
	}
}
