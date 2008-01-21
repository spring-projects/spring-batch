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
package org.springframework.batch.execution.bootstrap.support;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifierFactory;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class JobIdentifierPropertyEditorTests extends TestCase {

	private JobIdentifierPropertyEditor editor = new JobIdentifierPropertyEditor();
	
	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.support.JobIdentifierPropertyEditor#setJobIdentifierFactory(org.springframework.batch.core.runtime.JobIdentifierFactory)}.
	 */
	public void testSetJobIdentifierFactory() {
		editor.setJobIdentifierFactory(new SimpleJobIdentifierFactory() {
			public JobIdentifier getJobIdentifier(String name) {
				return super.getJobIdentifier("test:"+name);
			}
		});
		editor.setAsText("foo");
		JobIdentifier identifier = (JobIdentifier) editor.getValue();
		assertEquals("test:foo", identifier.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.support.JobIdentifierPropertyEditor#setAsText(java.lang.String)}.
	 */
	public void testSetAsTextString() {
		editor.setAsText("foo");
		JobIdentifier identifier = (JobIdentifier) editor.getValue();
		assertEquals("foo", identifier.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.support.JobIdentifierPropertyEditor#getAsText()}.
	 */
	public void testGetAsText() {
		editor.setAsText("foo");
		assertEquals("foo", editor.getAsText());
	}

}
