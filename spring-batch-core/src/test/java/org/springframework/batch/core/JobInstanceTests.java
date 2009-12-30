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
package org.springframework.batch.core;

import junit.framework.TestCase;

import org.springframework.batch.support.SerializationUtils;

/**
 * @author dsyer
 * 
 */
public class JobInstanceTests extends TestCase {

	private JobInstance instance = new JobInstance(new Long(11), new JobParameters(), "job");

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobInstance#getJobName()}.
	 */
	public void testGetName() {
		instance = new JobInstance(new Long(1), new JobParameters(), "foo");
		assertEquals("foo", instance.getJobName());
	}

	public void testGetJob() {
		assertEquals("job", instance.getJobName());
	}

	public void testCreateWithNulls() {
		try {
			new JobInstance(null, null, null);
			fail("job instance can't exist without job specified");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		instance = new JobInstance(null, null, "testJob");
		assertEquals("testJob", instance.getJobName());
		assertEquals(0, instance.getJobParameters().getParameters().size());
	}

	public void testSerialization() {
		instance = new JobInstance(new Long(1), new JobParametersBuilder().addDouble("doubleKey", Double.valueOf(5.1))
				.toJobParameters(), "jobName");
		
		byte[] serialized = SerializationUtils.serialize(instance);
		
		assertEquals(instance, SerializationUtils.deserialize(serialized));

	}
}
