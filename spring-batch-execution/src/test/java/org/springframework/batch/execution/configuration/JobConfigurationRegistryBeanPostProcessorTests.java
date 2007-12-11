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
package org.springframework.batch.execution.configuration;

import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.DuplicateJobConfigurationException;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class JobConfigurationRegistryBeanPostProcessorTests extends TestCase {

	private JobConfigurationRegistryBeanPostProcessor processor = new JobConfigurationRegistryBeanPostProcessor();

	public void testInitialization() throws Exception {
		try {
			processor.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().indexOf("JobConfigurationRegistry") >= 0);
		}
	}

	public void testBeforeInitialization() throws Exception {
		// should be a no-op
		assertEquals("foo", processor.postProcessAfterInitialization("foo", "bar"));
	}

	public void testAfterInitializationWithWrongType() throws Exception {
		// should be a no-op
		assertEquals("foo", processor.postProcessAfterInitialization("foo", "bar"));
	}

	public void testAfterInitializationWithCorrectType() throws Exception {
		MapJobConfigurationRegistry registry = new MapJobConfigurationRegistry();
		processor.setJobConfigurationRegistry(registry);
		JobConfiguration configuration = new JobConfiguration();
		configuration.setBeanName("foo");
		assertEquals(configuration, processor.postProcessAfterInitialization(configuration, "bar"));
		assertEquals(configuration, registry.getJobConfiguration("foo"));
	}

	public void testAfterInitializationWithDuplicate() throws Exception {
		MapJobConfigurationRegistry registry = new MapJobConfigurationRegistry();
		processor.setJobConfigurationRegistry(registry);
		JobConfiguration configuration = new JobConfiguration();
		configuration.setBeanName("foo");
		processor.postProcessAfterInitialization(configuration, "bar");
		try {
			processor.postProcessAfterInitialization(configuration, "spam");
			fail("Expected FatalBeanException");
		} catch (FatalBeanException e) {
			// Expected
			assertTrue(e.getCause() instanceof DuplicateJobConfigurationException);
		}
	}

	public void testUnregisterOnDestroy() throws Exception {
		MapJobConfigurationRegistry registry = new MapJobConfigurationRegistry();
		processor.setJobConfigurationRegistry(registry);
		JobConfiguration configuration = new JobConfiguration();
		configuration.setBeanName("foo");
		assertEquals(configuration, processor.postProcessAfterInitialization(configuration, "bar"));
		processor.destroy();
		try {
			assertEquals(null, registry.getJobConfiguration("foo"));
			fail("Expected NoSuchJobConfigurationException");
		} catch (NoSuchJobConfigurationException e) {
			// expected
		}
	}
	
	public void testExecutionWithApplicationContext() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-context.xml", getClass());
		MapJobConfigurationRegistry registry = (MapJobConfigurationRegistry) context.getBean("registry");
		Collection configurations = registry.getJobConfigurations();
		assertEquals(6, configurations.size());
		assertNotNull(registry.getJobConfiguration("test-job"));
		assertEquals(context.getBean("test-job-with-name"), registry.getJobConfiguration("foo"));
		assertEquals(context.getBean("test-job-with-bean-name"), registry.getJobConfiguration("bar"));
		assertEquals(context.getBean("test-job-with-parent-and-name"), registry.getJobConfiguration("spam"));
		assertEquals(context.getBean("test-job-with-parent-and-bean-name"), registry.getJobConfiguration("bucket"));
	}
}
