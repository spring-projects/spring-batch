package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.*;

import org.junit.Test;


public class JobParserJobFactoryBeanTests {
	
	private JobParserJobFactoryBean factory = new JobParserJobFactoryBean("jobFactory");
	
	@Test
	public void testSingleton() throws Exception {
		assertTrue("Expected singleton", factory.isSingleton());
	}

}
