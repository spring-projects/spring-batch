package org.springframework.batch.execution.runtime;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobIdentifier;

public class DefaultJobIdentifierFactoryTests extends TestCase {

	public void testGetJobIdentifier() {
		JobIdentifier jobIdentifier = new ScheduledJobIdentifierFactory().getJobIdentifier("foo");
		assertEquals("foo", jobIdentifier.getName());
	}

}
