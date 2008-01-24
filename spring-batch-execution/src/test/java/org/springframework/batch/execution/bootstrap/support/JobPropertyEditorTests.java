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

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobLocator;
import org.springframework.batch.core.domain.NoSuchJobException;

/**
 * @author Dave Syer
 * 
 */
public class JobPropertyEditorTests extends TestCase {

	private JobPropertyEditor editor = new JobPropertyEditor();
	private Job job = new Job();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		editor.setJobLocator(new JobLocator() {
			public Job getJob(String name) throws NoSuchJobException {
				job.setName(name);
				return job;
			}
		});
	}
	
	public void testMandatoryProperties() throws Exception {
		editor = new JobPropertyEditor();
		try {
			editor.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.JobParametersPropertyEditor#setAsText(java.lang.String)}.
	 */
	public void testSetAsTextString() {
		editor.setAsText("foo");
		Job job = (Job) editor.getValue();
		assertEquals(job, job);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.JobParametersPropertyEditor#getAsText()}.
	 */
	public void testGetAsText() {
		editor.setAsText("foo");
		assertEquals("foo", editor.getAsText());
	}

}
