package org.springframework.batch.core.runtime;

import org.springframework.batch.core.domain.JobIdentifier;

import junit.framework.TestCase;

public class SimpleJobIdentifierFactoryTests extends TestCase {

	public void testGetJobIdentifier() {
		JobIdentifier jobIdentifier = new SimpleJobIdentifierFactory().getJobIdentifier("foo");
		assertEquals("foo", jobIdentifier.getName());
	}

}
