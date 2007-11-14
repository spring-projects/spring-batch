package org.springframework.batch.execution.repository.dao;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;

public class EntityNameInterceptorTests extends TestCase {

	private EntityNameInterceptor interceptor = new EntityNameInterceptor();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		interceptor.setEntityNameLocator(new EntityNameLocator() {
			public String locate(Class clz) {
				return "foo";
			}
		});
	}
	
	public void testGetEntityName() {
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertEquals("foo", interceptor.getEntityName(job));
	}

}
