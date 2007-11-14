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
package org.springframework.batch.execution.repository.dao;

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;

/**
 * @author Dave Syer
 *
 */
public class JobIdentiferEntityNameLocatorTests extends TestCase {

	private JobIdentifierEntityNameLocator interceptor = new JobIdentifierEntityNameLocator();
	
	public void testGetEntityNameForScheduledJobIdentifier() {
		assertEquals("ScheduledJobInstance", interceptor.locate(ScheduledJobIdentifier.class));
	}

	public void testGetEntityNameForDefaultJobIdentifier() {
		assertEquals("DefaultJobInstance", interceptor.locate(DefaultJobIdentifier.class));
	}

	public void testGetEntityNameForSimpleJobIdentifier() {
		assertEquals("SimpleJobInstance", interceptor.locate(SimpleJobIdentifier.class));
	}

	public void testSetIdentifierTypesWithString() {
		interceptor.setIdentifierTypes(Collections.singletonMap(ScheduledJobIdentifier.class.getName(), "foo"));
		assertEquals("foo", interceptor.locate(ScheduledJobIdentifier.class));
	}

	public void testSetIdentifierTypesWithClass() {
		interceptor.setIdentifierTypes(Collections.singletonMap(ScheduledJobIdentifier.class, "foo"));
		assertEquals("foo", interceptor.locate(ScheduledJobIdentifier.class));
	}

	public void testSetIdentifierTypesWithInvalidClassName() {
		try {
			interceptor.setIdentifierTypes(Collections.singletonMap("FooBarNotAClass", "foo"));
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
