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
package org.springframework.batch.core.configuration.support;

import junit.framework.TestCase;

import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ClassPathXmlApplicationContextJobFactoryTests extends TestCase {
	
	private ClassPathXmlApplicationContextJobFactory factory = new ClassPathXmlApplicationContextJobFactory("test-job", ClassUtils.addResourcePathToPackagePath(getClass(), "trivial-context.xml"), null);

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.ClassPathXmlApplicationContextJobFactory#createJob()}.
	 */
	public void testCreateJob() {
		assertNotNull(factory.createJob());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.ClassPathXmlApplicationContextJobFactory#getJobName()}.
	 */
	public void testGetJobName() {
		assertEquals("test-job", factory.getJobName());
	}

}
