package org.springframework.batch.execution.repository.dao;

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;

public class EntityNameInterceptorTests extends TestCase {

	private EntityNameInterceptor interceptor = new EntityNameInterceptor();
	
	public void testGetEntityNameForScheduledJobIdentifier() {
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertEquals("ScheduledJobInstance", interceptor.getEntityName(job));
	}

	public void testGetEntityNameForDefaultJobIdentifier() {
		JobInstance job = new JobInstance(new DefaultJobIdentifier("foo"));
		assertEquals("DefaultJobInstance", interceptor.getEntityName(job));
	}

	public void testGetEntityNameForSimpleJobIdentifier() {
		JobInstance job = new JobInstance(new SimpleJobIdentifier("foo"));
		assertEquals("SimpleJobInstance", interceptor.getEntityName(job));
	}

	public void testSetIdentifierTypesWithString() {
		interceptor.setIdentifierTypes(Collections.singletonMap(ScheduledJobIdentifier.class.getName(), "foo"));
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertEquals("foo", interceptor.getEntityName(job));
	}

	public void testSetIdentifierTypesWithClass() {
		interceptor.setIdentifierTypes(Collections.singletonMap(ScheduledJobIdentifier.class, "foo"));
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertEquals("foo", interceptor.getEntityName(job));
	}

	public void testSetIdentifierTypesWithInvalidClassName() {
		try {
			interceptor.setIdentifierTypes(Collections.singletonMap("FooBarNotAClass", "foo"));
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
