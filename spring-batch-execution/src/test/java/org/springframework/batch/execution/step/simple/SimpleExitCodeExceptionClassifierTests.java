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
package org.springframework.batch.execution.step.simple;


import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.repeat.ExitStatus;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class SimpleExitCodeExceptionClassifierTests extends TestCase {

	NullPointerException exception;
	
	SimpleExitCodeExceptionClassifier classifier = new SimpleExitCodeExceptionClassifier();
	
	protected void setUp() throws Exception {		
		super.setUp();
		
		exception = new NullPointerException();
	}
	
	public void testClassifyForExitCode() {
		
		ExitStatus exitStatus = classifier.classifyForExitCode(exception);
		assertEquals(exitStatus.getExitCode(), "FATAL_EXCEPTION");
		assertEquals(exitStatus.getExitDescription(), "java.lang.NullPointerException");
	}

	public void testClassify() {
		ExitStatus exitStatus = (ExitStatus)classifier.classify(exception);
		assertEquals(exitStatus.getExitCode(), "FATAL_EXCEPTION");
		assertEquals(exitStatus.getExitDescription(), "java.lang.NullPointerException");
	}

	public void testGetDefault() {
		ExitStatus exitStatus = (ExitStatus)classifier.getDefault();
		assertEquals(exitStatus.getExitCode(), "FATAL_EXCEPTION");
		assertEquals(exitStatus.getExitDescription(), "");
	}

	/*
	 * Attempting to classify a null throwable should lead to a blank description, not a 
	 * null pointer exception.
	 */
	public void testClassifyNullThrowable(){
		ExitStatus exitStatus = (ExitStatus)classifier.classify(null);
		assertEquals(exitStatus.getExitCode(), "FATAL_EXCEPTION");
		assertEquals(exitStatus.getExitDescription(), "");
	}
	
	public void testClassifyInterruptedException(){
		ExitStatus exitStatus = (ExitStatus)classifier.classifyForExitCode(new StepInterruptedException(""));
		assertEquals(exitStatus.getExitCode(), ExitCodeExceptionClassifier.STEP_INTERRUPTED);
		assertEquals(exitStatus.getExitDescription(), 
				StepInterruptedException.class.getName());
	}
}
