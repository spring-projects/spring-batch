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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class JobRegistryBeanPostProcessorTests {

	private JobRegistryBeanPostProcessor processor = new JobRegistryBeanPostProcessor();

	@Test
	public void testInitializationFails() throws Exception {
		try {
			processor.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains("JobRegistry"));
		}
	}

	@Test
	public void testBeforeInitialization() throws Exception {
		// should be a no-op
		assertEquals("foo", processor.postProcessBeforeInitialization("foo", "bar"));
	}

	@Test
	public void testAfterInitializationWithWrongType() throws Exception {
		// should be a no-op
		assertEquals("foo", processor.postProcessAfterInitialization("foo", "bar"));
	}

	@Test
	public void testAfterInitializationWithCorrectType() throws Exception {
		MapJobRegistry registry = new MapJobRegistry();
		processor.setJobRegistry(registry);
		JobSupport job = new JobSupport();
		job.setBeanName("foo");
		assertNotNull(processor.postProcessAfterInitialization(job, "bar"));
		assertEquals("[foo]", registry.getJobNames().toString());
	}

	@Test
	public void testAfterInitializationWithGroupName() throws Exception {
		MapJobRegistry registry = new MapJobRegistry();
		processor.setJobRegistry(registry);
		processor.setGroupName("jobs");
		JobSupport job = new JobSupport();
		job.setBeanName("foo");
		assertNotNull(processor.postProcessAfterInitialization(job, "bar"));
		assertEquals("[jobs.foo]", registry.getJobNames().toString());
	}

	@Test
	public void testAfterInitializationWithDuplicate() throws Exception {
		MapJobRegistry registry = new MapJobRegistry();
		processor.setJobRegistry(registry);
		JobSupport job = new JobSupport();
		job.setBeanName("foo");
		processor.postProcessAfterInitialization(job, "bar");
		try {
			processor.postProcessAfterInitialization(job, "spam");
			fail("Expected FatalBeanException");
		}
		catch (FatalBeanException e) {
			// Expected
			assertTrue(e.getCause() instanceof DuplicateJobException);
		}
	}

	@Test
	public void testUnregisterOnDestroy() throws Exception {
		MapJobRegistry registry = new MapJobRegistry();
		processor.setJobRegistry(registry);
		JobSupport job = new JobSupport();
		job.setBeanName("foo");
		assertNotNull(processor.postProcessAfterInitialization(job, "bar"));
		processor.destroy();
		assertEquals("[]", registry.getJobNames().toString());
	}

	@Test
	public void testExecutionWithApplicationContext() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-context.xml", getClass());
		MapJobRegistry registry = (MapJobRegistry) context.getBean("registry");
		Collection<String> configurations = registry.getJobNames();
		// System.err.println(configurations);
		String[] names = context.getBeanNamesForType(JobSupport.class);
		int count = names.length;
		// Each concrete bean of type JobConfiguration is registered...
		assertEquals(count, configurations.size());
		// N.B. there is a failure / wonky mode where a parent bean is given an
		// explicit name or beanName (using property setter): in this case then
		// child beans will have the same name and will be re-registered (and
		// override, if the registry supports that).
		assertNotNull(registry.getJob("test-job"));
		assertEquals(context.getBean("test-job-with-name"), registry.getJob("foo"));
		assertEquals(context.getBean("test-job-with-bean-name"), registry.getJob("bar"));
		assertEquals(context.getBean("test-job-with-parent-and-name"), registry.getJob("spam"));
		assertEquals(context.getBean("test-job-with-parent-and-bean-name"), registry.getJob("bucket"));
		assertEquals(context.getBean("test-job-with-concrete-parent"), registry.getJob("maps"));
		assertEquals(context.getBean("test-job-with-concrete-parent-and-name"), registry.getJob("oof"));
		assertEquals(context.getBean("test-job-with-concrete-parent-and-bean-name"), registry.getJob("rab"));
	}

}
