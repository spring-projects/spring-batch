package org.springframework.batch.execution.resource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;

public class DefaultJobInstanceLabelGeneratorTests extends TestCase {

	/**
	 * 
	 */
	private static final Date DATE = new Date(0);

	DefaultJobInstanceLabelGenerator instance = new DefaultJobInstanceLabelGenerator();
	
	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	
	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobInstanceLabelGenerator#getLabel()}.
	 */
	public void testGetLabelFromNull() {
		assertEquals(null, instance.getLabel(null));
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobInstanceLabelGenerator#getLabel()}.
	 */
	public void testGetLabel() {
		assertEquals("foo", instance.getLabel(new JobInstance(null, new JobParameters(), new JobSupport("foo"))));
	}
	
	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobInstanceLabelGenerator#getLabel()}.
	 */
	public void testDefaultGetLabel() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addDate("schedule.date", DATE).addString("key", "bar").toJobParameters();
		JobInstance job = new JobInstance(null, jobParameters, new JobSupport(null));
		assertEquals("null-bar-"+dateFormat.format(DATE), instance.getLabel(job));
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.resource.DefaultJobInstanceLabelGenerator#getLabel()}.
	 */
	public void testGetLabelWithAllProperties() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addDate("schedule.date", DATE).addString("key", "bar").toJobParameters();
		JobInstance job = new JobInstance(null, jobParameters, new JobSupport("foo"));
		assertEquals("foo-bar-"+dateFormat.format(DATE), instance.getLabel(job));
	}

}
