/**
 * 
 */
package org.springframework.batch.core;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

/**
 * @author Lucas Ward
 *
 */
public class JobParameterTests {

	JobParameter jobParameter;
	
	@Test
	public void testStringParameter(){
		jobParameter = new JobParameter("test");
		assertEquals("test", jobParameter.getValue());
	}
	
	@Test
	public void testLongParameter(){
		jobParameter = new JobParameter(1L);
		assertEquals(1L, jobParameter.getValue());
	}
	
	@Test
	public void testDoubleParameter(){
		jobParameter = new JobParameter(1.1);
		assertEquals(1.1, jobParameter.getValue());
	}
	
	@Test
	public void testDateParameter(){
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch);
		assertEquals(new Date(0L), jobParameter.getValue());
	}
	
	@Test
	public void testDateParameterToString(){
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch);
		assertEquals("0", jobParameter.toString());
	}
	
	@Test
	public void testEquals(){
		jobParameter = new JobParameter("test");
		JobParameter testParameter = new JobParameter("test");
		assertTrue(jobParameter.equals(testParameter));
	}
	
}