package org.springframework.batch.execution.runtime;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobIdentifier;

public class ScheduledJobIdentifierFactoryTests extends TestCase {

	public void testGetJobIdentifier() {
		JobIdentifier jobIdentifier = new DefaultJobIdentifierFactory().getJobIdentifier("foo");
		assertEquals("foo", jobIdentifier.getName());
	}

}
