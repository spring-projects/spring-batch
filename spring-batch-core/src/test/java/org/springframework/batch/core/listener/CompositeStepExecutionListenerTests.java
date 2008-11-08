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
package org.springframework.batch.core.listener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * @author Dave Syer
 * 
 */
public class CompositeStepExecutionListenerTests extends TestCase {

	private CompositeStepExecutionListener listener = new CompositeStepExecutionListener();

	private List<String> list = new ArrayList<String>();

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.CompositeStepExecutionListener#setListeners(org.springframework.batch.core.StepExecutionListener[])}
	 * .
	 */
	public void testSetListeners() {
		listener.setListeners(new StepExecutionListener[] { new StepExecutionListenerSupport() {
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("fail");
				return ExitStatus.FAILED;
			}
		}, new StepExecutionListenerSupport() {
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("continue");
				return ExitStatus.EXECUTING;
			}
		} });
		assertEquals(ExitStatus.FAILED, listener.afterStep(null));
		assertEquals(2, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.CompositeStepExecutionListener#register(org.springframework.batch.core.StepExecutionListener)}
	 * .
	 */
	public void testSetListener() {
		listener.register(new StepExecutionListenerSupport() {
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("fail");
				return ExitStatus.FAILED;
			}
		});
		assertEquals(ExitStatus.FAILED, listener.afterStep(null));
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.CompositeStepExecutionListener#beforeStep(StepExecution)}
	 * .
	 */
	public void testOpen() {
		listener.register(new StepExecutionListenerSupport() {
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}
		});
		listener.beforeStep(new StepExecution("foo", null));
		assertEquals(1, list.size());
	}

}
