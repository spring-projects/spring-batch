package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * 
 * @author Dave Syer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AutomaticJobRegistrarContextTests {

	@Autowired
	private JobRegistry registry;
	
	@Test
	public void testLocateJob() throws Exception{
		
		Collection<String> names = registry.getJobNames();
		assertEquals(2, names.size());
		assertTrue(names.contains("test-job"));
		
		Job job = registry.getJob("test-job");
		assertEquals("test-job", job.getName());

	}
	
}
