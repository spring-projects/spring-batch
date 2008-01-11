/**
 * 
 */
package org.springframework.batch.core.domain;

import java.util.Date;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class JobInstancePropertiesBuilderTests extends TestCase {

	JobInstancePropertiesBuilder parametersBuilder;
	
	Date date = new Date(System.currentTimeMillis());
	
	protected void setUp() throws Exception {
		super.setUp();
		
		parametersBuilder = new JobInstancePropertiesBuilder();
		parametersBuilder.addDate("SCHEDULE_DATE", date);
		parametersBuilder.addLong("LONG", new Long(1));
		parametersBuilder.addString("STRING", "string value");
	}
	
	public void testToJobRuntimeParamters(){
		
		JobInstanceProperties parameters = parametersBuilder.toJobParameters();
		assertEquals(parameters.getDate("SCHEDULE_DATE"), date);
		assertEquals(parameters.getLong("LONG"), new Long(1));
		assertEquals(parameters.getString("STRING"), "string value");
	}
}
