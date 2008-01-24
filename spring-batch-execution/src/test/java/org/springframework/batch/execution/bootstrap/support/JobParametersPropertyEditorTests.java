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

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobParameters;

/**
 * @author Dave Syer
 * 
 */
public class JobParametersPropertyEditorTests extends TestCase {

	private JobParametersPropertyEditor editor = new JobParametersPropertyEditor();

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.JobParametersPropertyEditor#setAsText(java.lang.String)}.
	 */
	public void testSetAsTextString() {
		editor.setAsText("foo=bar");
		JobParameters identifier = (JobParameters) editor.getValue();
		assertEquals("bar", identifier.getString("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.JobParametersPropertyEditor#getAsText()}.
	 */
	public void testGetAsText() {
		editor.setAsText("foo=bar,spam=bucket");
		assertEquals("foo=bar,spam=bucket", editor.getAsText());
	}

}
