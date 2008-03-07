/**
 * 
 */
package org.springframework.batch.core;

import java.util.Date;
import java.util.Iterator;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class JobParametersBuilderTests extends TestCase {

	JobParametersBuilder parametersBuilder = new JobParametersBuilder();
	
	Date date = new Date(System.currentTimeMillis());
	
	public void testToJobRuntimeParamters(){	
		parametersBuilder.addDate("SCHEDULE_DATE", date);
		parametersBuilder.addLong("LONG", new Long(1));
		parametersBuilder.addString("STRING", "string value");
		JobParameters parameters = parametersBuilder.toJobParameters();
		assertEquals(date, parameters.getDate("SCHEDULE_DATE"));
		assertEquals(new Long(1), parameters.getLong("LONG"));
		assertEquals("string value", parameters.getString("STRING"));
	}

	public void testOrderedTypes(){	
		parametersBuilder.addDate("SCHEDULE_DATE", date);
		parametersBuilder.addLong("LONG", new Long(1));
		parametersBuilder.addString("STRING", "string value");
		Iterator parameters = parametersBuilder.toJobParameters().getParameters().keySet().iterator();
		assertEquals("STRING", parameters.next());
		assertEquals("LONG", parameters.next());
		assertEquals("SCHEDULE_DATE", parameters.next());
	}

	public void testOrderedStrings(){	
		parametersBuilder.addString("foo", "value foo");
		parametersBuilder.addString("bar", "value bar");
		parametersBuilder.addString("spam", "value spam");
		Iterator parameters = parametersBuilder.toJobParameters().getParameters().keySet().iterator();
		assertEquals("foo", parameters.next());
		assertEquals("bar", parameters.next());
		assertEquals("spam", parameters.next());
	}
}
