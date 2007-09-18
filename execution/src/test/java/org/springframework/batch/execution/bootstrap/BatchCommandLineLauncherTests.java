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
package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class BatchCommandLineLauncherTests extends TestCase {

	BatchCommandLineLauncher commandLine = new BatchCommandLineLauncher();
	
	int count = 0;
	
	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testMainWithDefaultArguments() throws Exception {
	//	BatchCommandLineLauncher.main(new String[0]);
		// TODO: find a way to assert something.  No error actually
		// means the test was successful...
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testMainWithParentContext() throws Exception {
		// Try an XML file name for the parent context with no suffix
		//BatchCommandLineLauncher.main(new String[]{"job-configuration"});
		//assertEquals(commandLine.start("job-configuration.xml", null), 0);
	}
	
	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testMainWithParentContextAndValidJobId() throws Exception {
		// Try a job id as the second argument 
		//BatchCommandLineLauncher.main(new String[]{"job-configuration", "test-job"});
		//assertEquals(commandLine.start("job-configuration.xml", "test-job"), 0);
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testMainWithParentContextAndInvalidJobId() throws Exception {
		// Try a job id as the second argument test-job
		//BatchCommandLineLauncher.main(new String[]{"job-configuration", "foo-bar-spam"});
		//assertEquals(commandLine.start("job-configuration", "foo-bar-spam"), 2);
	}
}
