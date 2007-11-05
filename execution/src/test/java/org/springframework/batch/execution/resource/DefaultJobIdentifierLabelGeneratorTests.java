package org.springframework.batch.execution.resource;

import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.resource.DefaultJobIdentifierLabelGenerator;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;

public class DefaultJobIdentifierLabelGeneratorTests extends TestCase {

	DefaultJobIdentifierLabelGenerator instance = new DefaultJobIdentifierLabelGenerator();
	
	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobIdentifierLabelGenerator#getLabel()}.
	 */
	public void testGetLabelFromNull() {
		assertEquals(null, instance.getLabel(null));
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobIdentifierLabelGenerator#getLabel()}.
	 */
	public void testGetLabel() {
		assertEquals("foo", instance.getLabel(new SimpleJobIdentifier("foo")));
	}
	
	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobIdentifierLabelGenerator#getLabel()}.
	 */
	public void testDefaultGetLabel() throws Exception {
		assertEquals("null--0-19700101", instance.getLabel(new ScheduledJobIdentifier(null)));
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobIdentifierLabelGenerator#getLabel()}.
	 */
	public void testGetLabelWithAllProperties() throws Exception {
		ScheduledJobIdentifier identifier = new ScheduledJobIdentifier(null);
		identifier.setName("foo");
		identifier.setJobStream("bar");
		identifier.setJobRun(11);
		identifier.setScheduleDate(new SimpleDateFormat("yyyyMMdd").parse("20070730"));
		assertEquals("foo-bar-11-20070730", instance.getLabel(identifier));
	}

}
