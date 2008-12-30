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
package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dave Syer
 *
 */
public class SystemPropertyInitializerTests {
	
	private static final String SIMPLE_NAME = SystemPropertyInitializerTests.class.getSimpleName();
	private SystemPropertyInitializer initializer = new SystemPropertyInitializer();
	
	@Before
	@After
	public void initializeProperty() {
		System.clearProperty(SystemPropertyInitializer.ENVIRONMENT);
		System.clearProperty(SIMPLE_NAME);
	}

	@Test
	public void testSetKeyName() throws Exception {
		initializer.setKeyName(SIMPLE_NAME);
		System.setProperty(SIMPLE_NAME, "foo");
		initializer.afterPropertiesSet();
		assertEquals("foo", System.getProperty(SIMPLE_NAME));
	}

	@Test
	public void testSetDefaultValue() throws Exception {
		initializer.setDefaultValue("foo");
		initializer.afterPropertiesSet();
		assertEquals("foo", System.getProperty(SystemPropertyInitializer.ENVIRONMENT));
	}

	@Test(expected=IllegalStateException.class)
	public void testNoDefaultValue() throws Exception {
		initializer.afterPropertiesSet();
	}

}
