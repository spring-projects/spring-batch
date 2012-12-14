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
		assertTrue("identifying", jobParameter.isIdentifying());
	}

	@Test
	public void testNonIdentifyingStringParameter(){
		jobParameter = new JobParameter("test", false);
		assertEquals("test", jobParameter.getValue());
		assertEquals("identifying", false, jobParameter.isIdentifying());
	}

	@Test
	public void testNullStringParameter(){
		jobParameter = new JobParameter((String)null);
		assertEquals(null, jobParameter.getValue());
	}

	@Test
	public void testLongParameter(){
		jobParameter = new JobParameter(1L);
		assertEquals(1L, jobParameter.getValue());
		assertTrue("identifying", jobParameter.isIdentifying());
	}

	@Test
	public void testDoubleParameter(){
		jobParameter = new JobParameter(1.1);
		assertEquals(1.1, jobParameter.getValue());
		assertTrue("identifying", jobParameter.isIdentifying());
	}

	@Test
	public void testDateParameter(){
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch);
		assertEquals(new Date(0L), jobParameter.getValue());
		assertTrue("identifying", jobParameter.isIdentifying());
	}

	@Test
	public void testNullDateParameter(){
		jobParameter = new JobParameter((Date)null);
		assertEquals(null, jobParameter.getValue());
	}

	@Test
	public void testDateParameterToString(){
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch);
		assertEquals("0", jobParameter.toString());
	}

	@Test
	public void testNonIdentifyingDateParameterToString(){
		Date epoch = new Date(0L);
		jobParameter = new JobParameter(epoch, false);
		assertEquals("(-)0", jobParameter.toString());
	}

	@Test
	public void testEquals(){
		jobParameter = new JobParameter("test");
		JobParameter testParameter = new JobParameter("test");
		assertTrue(jobParameter.equals(testParameter));
	}

	@Test
	public void testEqualsWithNonIdentifying(){
		jobParameter = new JobParameter("test", false);
		JobParameter testParameter = new JobParameter("test", false);
		assertEquals(jobParameter, testParameter);
	}

	@Test
	public void testEqualsWithDifferentIdentifyingFlag(){
		jobParameter = new JobParameter("test");  	// identifying
		JobParameter testParameter
				= new JobParameter("test", false);	// non-identifying

		assertFalse(jobParameter.equals(testParameter));
	}

	@Test
	public void testHashcode(){
		jobParameter = new JobParameter("test");
		JobParameter testParameter = new JobParameter("test");
		assertEquals(testParameter.hashCode(), jobParameter.hashCode());
	}

	@Test
	public void testEqualsWithNull(){
		jobParameter = new JobParameter((String)null);
		JobParameter testParameter = new JobParameter((String)null);
		assertTrue(jobParameter.equals(testParameter));
	}

	@Test
	public void testEqualsWithNullAndDifferentType(){
		jobParameter = new JobParameter((String)null);
		JobParameter testParameter = new JobParameter((Date)null);
		assertFalse(jobParameter.equals(testParameter));
	}

	@Test
	public void testHashcodeWithNull(){
		jobParameter = new JobParameter((String)null);
		JobParameter testParameter = new JobParameter((String)null);
		assertEquals(testParameter.hashCode(), jobParameter.hashCode());
	}


	@Test
	public void testHashcodeWithNotIdentifyIsDifferentFromParamHashCode(){
		jobParameter = new JobParameter("test");
		JobParameter testParameter = new JobParameter("test", false);
		assertFalse("non-identifying hashcode should be different",
					testParameter.hashCode() == jobParameter.hashCode());
	}
}