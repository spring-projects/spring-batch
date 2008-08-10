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
package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobFactory;

/**
 * @author Dave Syer
 * 
 */
public class JobFactoryRegistrationListenerTests {

	private JobFactoryRegistrationListener listener = new JobFactoryRegistrationListener();

	private MapJobRegistry registry = new MapJobRegistry();

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.configuration.support.JobFactoryRegistrationListener#bind(org.springframework.batch.core.configuration.JobFactory, java.util.Map)}.
	 * @throws Exception
	 */
	@Test
	public void testBind() throws Exception {
		listener.setJobRegistry(registry);
		listener.bind(new JobFactory() {
			public Job createJob() {
				return null;
			}

			public String getJobName() {
				return "foo";
			}
		}, null);
		assertEquals(1, registry.getJobNames().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.configuration.support.JobFactoryRegistrationListener#unbind(org.springframework.batch.core.configuration.JobFactory, java.util.Map)}.
	 * @throws Exception 
	 */
	@Test
	public void testUnbind() throws Exception {
		testBind();
		listener.unbind(new JobFactory() {
			public Job createJob() {
				return null;
			}

			public String getJobName() {
				return "foo";
			}
		}, null);
		assertEquals(0, registry.getJobNames().size());
	}

}
