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
package org.springframework.batch.execution.launch;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.execution.launch.JobExecutionListener;
import org.springframework.batch.execution.launch.JobExecutionListenerSupport;

/**
 * @author Dave Syer
 * 
 */
public class JobExecutionListenerSupportTests extends TestCase {

	private List list = new ArrayList();

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.launch.JobExecutionListenerSupport#after(org.springframework.batch.core.domain.JobExecution)}.
	 */
	public void testAfter() {
		JobExecutionListener listener = new JobExecutionListenerSupport() {
			public void after(JobExecution execution) {
				super.after(execution);
				list.add("after");
			}
		};

		listener.after(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.launch.JobExecutionListenerSupport#before(org.springframework.batch.core.domain.JobExecution)}.
	 */
	public void testBefore() {
		JobExecutionListener listener = new JobExecutionListenerSupport() {
			public void before(JobExecution execution) {
				super.before(execution);
				list.add("after");
			}
		};

		listener.before(null);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.launch.JobExecutionListenerSupport#before(org.springframework.batch.core.domain.JobExecution)}.
	 */
	public void testStop() {
		JobExecutionListener listener = new JobExecutionListenerSupport() {
			public void onStop(JobExecution execution) {
				super.onStop(execution);
				list.add("stop");
			}
		};

		listener.onStop(null);
		assertEquals(1, list.size());
	}
}
