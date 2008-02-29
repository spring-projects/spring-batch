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
package org.springframework.batch.execution.listener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobListener;
import org.springframework.batch.core.listener.JobListenerSupport;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.batch.execution.listener.CompositeJobListener;

/**
 * @author Dave Syer
 * 
 */
public class CompositeJobListenerTests extends TestCase {

	private CompositeJobListener listener = new CompositeJobListener();

	private List list = new ArrayList();

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.listener.CompositeJobListener#setListeners(org.springframework.batch.core.domain.JobListener[])}.
	 */
	public void testSetListeners() {
		listener.setListeners(new JobListener[] { new JobListenerSupport() {
			public void afterJob() {
				list.add("fail");
			}
		}, new JobListenerSupport() {
			public void afterJob() {
				list.add("continue");
			}
		} });
		listener.afterJob();
		assertEquals(2, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.listener.CompositeJobListener#setListener(org.springframework.batch.core.domain.JobListener)}.
	 */
	public void testSetListener() {
		listener.register(new JobListenerSupport() {
			public void afterJob() {
				list.add("fail");
			}
		});
		listener.afterJob();
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.listener.CompositeJobListener#beforeJob(JobExecution)}.
	 */
	public void testOpen() {
		listener.register(new JobListenerSupport() {
			public void beforeJob(JobExecution stepExecution) {
				list.add("foo");
			}
		});
		listener.beforeJob(new JobExecution(new JobInstance(new Long(11L), null, new JobSupport())));
		assertEquals(1, list.size());
	}

}
